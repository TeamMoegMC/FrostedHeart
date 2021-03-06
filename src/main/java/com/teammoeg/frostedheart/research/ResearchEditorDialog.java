package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.clues.ClueEditor;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.EffectEditor;
import com.teammoeg.frostedheart.research.gui.FHIcons.IconEditor;
import com.teammoeg.frostedheart.research.gui.editor.BaseEditDialog;
import com.teammoeg.frostedheart.research.gui.editor.EditListDialog;
import com.teammoeg.frostedheart.research.gui.editor.EditUtils;
import com.teammoeg.frostedheart.research.gui.editor.Editor;
import com.teammoeg.frostedheart.research.gui.editor.IngredientEditor;
import com.teammoeg.frostedheart.research.gui.editor.LabeledSelection;
import com.teammoeg.frostedheart.research.gui.editor.LabeledTextBox;
import com.teammoeg.frostedheart.research.gui.editor.LabeledTextBoxAndBtn;
import com.teammoeg.frostedheart.research.gui.editor.NumberBox;
import com.teammoeg.frostedheart.research.gui.editor.OpenEditorButton;
import com.teammoeg.frostedheart.research.gui.editor.SelectDialog;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class ResearchEditorDialog extends BaseEditDialog {
	Research r;
	LabeledTextBox id,name;
	LabeledSelection<ResearchCategory> cat;
	NumberBox pts;
	LabeledSelection<Boolean> hide;
	LabeledSelection<Boolean> alt;
	boolean removed;
	public static final Editor<Collection<Research>> RESEARCH_LIST=(p,l,v,c)->{
		new EditListDialog<>(p,l,v,null,SelectDialog.EDITOR_RESEARCH,e->e.getName().getString(),Research::getIcon,c).open();
	};
	public ResearchEditorDialog(Widget panel,Research r,ResearchCategory def) {
		super(panel);
		if(r==null) {
			r=new Research();
			r.setCategory(def==null?ResearchCategory.RESCUE:def);
		}
		this.r=r;
		id=new LabeledTextBoxAndBtn(this,"id",r.getId(),"Random",t->t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
		
		cat=new LabeledSelection<ResearchCategory>(this,"category",r.getCategory(),ResearchCategory.values(),ResearchCategory::name);
		name=new LabeledTextBox(this,"name",r.name);
		pts=new NumberBox(this,"points",r.points);
		hide=LabeledSelection.createBool(this,"Hide effects before complete",r.hideEffects);
		alt=LabeledSelection.createBool(this,"Show alt description before complete",r.showfdesc);
	}

	
	@Override
	public void onClose() {
		if(removed) {
			if(r.getRId()!=0)
				r.delete();
			
		}else {
			r.name=name.getText();
			r.setCategory(cat.getSelection());
			r.points=pts.getNum();
			r.hideEffects=hide.getSelection();
			r.showfdesc=alt.getSelection();
			if(r.getRId()==0) {//creating new research
				if(!id.getText().isEmpty()) {
					r.setId(id.getText());
					FHResearch.register(r);
					r.doIndex();
				}
			}else {//modify old research
				r.setNewId(id.getText());
			}
			EditUtils.saveResearch(r);
		}
	}

	@Override
	public void addWidgets() {
		add(EditUtils.getTitle(this,"Edit/New Research"));
		add(id);
		add(new SimpleTextButton(this,GuiUtils.str("Reset id"),Icon.EMPTY) {
			@Override
			public void onClicked(MouseButton arg0) {
				id.setText(r.getId());
			}
		});
		add(name);
		add(pts);
		add(new OpenEditorButton<>(this,"Set Icon",IconEditor.EDITOR,r.icon,r.icon,s->r.icon=s));
		add(cat);
		
		add(new OpenEditorButton<>(this,"Edit Description",EditListDialog.STRING_LIST,r.desc,s->r.desc=new ArrayList<>(s)));
		add(new OpenEditorButton<>(this,"Edit Alternative Description",EditListDialog.STRING_LIST,r.fdesc,s->r.fdesc=new ArrayList<>(s)));
		add(new OpenEditorButton<>(this,"Edit Parents",ResearchEditorDialog.RESEARCH_LIST,r.getParents(),s->{
			r.setParents(s.stream().map(Research::getSupplier).collect(Collectors.toList()));
		}));
		add(new OpenEditorButton<>(this,"Edit Children",ResearchEditorDialog.RESEARCH_LIST,r.getChildren(),s->{
			r.getChildren().forEach(e->e.removeParent(r));
			s.forEach(e->{e.addParent(r.getSupplier());e.doIndex();});
			
		}));
		add(new OpenEditorButton<>(this,"Edit Ingredients",IngredientEditor.LIST_EDITOR,r.getRequiredItems(),s->{
			r.requiredItems=new ArrayList<>(s);
		}));
		add(new OpenEditorButton<>(this,"Edit Effects",EffectEditor.EFFECT_LIST,r.getEffects(),s->{
			r.getEffects().forEach(Effect::deleteSelf);
			r.getEffects().clear();
			r.getEffects().addAll(s);
			r.doIndex();
		}));
		add(new OpenEditorButton<>(this,"Edit Clues",ClueEditor.EDITOR_LIST,r.getClues(),s->{
			r.getClues().forEach(Clue::deleteSelf);
			r.getClues().clear();
			r.getClues().addAll(s);
			r.doIndex();
		}));
		add(new SimpleTextButton(this,GuiUtils.str("Remove"),Icon.EMPTY){

			@Override
			public void onClicked(MouseButton arg0) {
				removed=true;
				close();
			}
			
		});
		add(hide);
		add(alt);
		
	}


	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.draw(matrixStack, theme, x, y, w, h);
		Research r=FHResearch.researches.getByName(id.getText());
		if(r!=null&&r!=this.r)
			theme.drawString(matrixStack,"ID Existed!", x+id.width+10, y+27,Color4I.RED,0);
	}

}
