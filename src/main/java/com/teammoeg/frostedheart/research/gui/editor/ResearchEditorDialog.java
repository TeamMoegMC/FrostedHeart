package com.teammoeg.frostedheart.research.gui.editor;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategory;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHItemIcon;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.item.ItemStack;

public class ResearchEditorDialog extends EditDialog {
	Research r;
	LabeledTextBox id,name;
	LabeledSelection<ResearchCategory> cat;
	public ResearchEditorDialog(Widget panel,Research r,ResearchCategory def) {
		super(panel);
		
		setSize(400,800);
		if(r==null) {
			r=new Research();
			r.setCategory(def==null?ResearchCategory.RESCUE:def);
		}
		this.r=r;
		id=new LabeledTextBoxAndBtn(this,"id",r.getId(),"Random",t->t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
		
		cat=new LabeledSelection<ResearchCategory>(this,"category",r.getCategory(),ResearchCategory.values(),ResearchCategory::name);
		name=new LabeledTextBox(this,"name",r.name);
	}

	
	@Override
	public void onClose() {
		if(r.getRId()==0) {//creating new research
			if(!id.getText().isEmpty()) {
				r.setId(id.getText());
				r.name=name.getText();
				r.setCategory(cat.getSelection());
				FHResearch.register(r);
				r.doIndex();
			}
		}else {//modify old research
			r.setNewId(id.getText());
			r.name=name.getText();
			r.setCategory(cat.getSelection());
			FHResearch.register(r);
		}
		EditUtils.saveResearch(r);
	}

	@Override
	public void addWidgets() {
		
		add(id);
		add(new SimpleTextButton(this,GuiUtils.str("Reset id"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				id.setText(r.getId());
			}
			
		});
		add(name);
		add(new OpenEditorButton<>(this,"Set Icon",IconEditor.EDITOR,r.icon,r.icon,s->r.icon=s));
		add(cat);
		
		add(new OpenEditorButton<>(this,"Edit Description",EditListDialog.STRING_LIST,r.desc,s->r.desc=new ArrayList<>(s)));
		add(new OpenEditorButton<>(this,"Edit Parents",EditListDialog.RESEARCH_LIST,r.getParents(),s->{
			r.setParents(s.stream().map(Research::getSupplier).collect(Collectors.toList()));
		}));
		add(new OpenEditorButton<>(this,"Edit Children",EditListDialog.RESEARCH_LIST,r.getChildren(),s->{
			r.getChildren().forEach(e->e.removeParent(r));
			s.forEach(e->{e.addParent(r.getSupplier());e.doIndex();});
			
		}));
		
	}

	@Override
	public void alignWidgets() {
		int offset=5;
		for(Widget w:this.widgets) {
			w.setPos(5, offset);
			offset+=w.height+1;
		}
		setHeight(offset+5);
	}


	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}


	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.draw(matrixStack, theme, x, y, w, h);
		Research r=FHResearch.researches.getByName(id.getText());
		if(r!=null&&r!=this.r)
			theme.drawString(matrixStack,"ID Existed!", x+id.width+10, y+7,Color4I.RED,0);
	}

}
