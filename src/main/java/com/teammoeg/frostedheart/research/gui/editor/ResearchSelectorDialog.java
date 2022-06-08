package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.gui.RTextField;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechScrollBar;

import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class ResearchSelectorDialog extends EditDialog {
	public static final Editor<Research> EDITOR=(p,l,v,c)->{
		new ResearchSelectorDialog(p,l,v,c).open();
	};
	String lbl;
	Research val;
	Consumer<Research> cb;
	
    public ResearchSelectorDialog(Widget panel, String lbl, Research val, Consumer<Research> cb) {
		super(panel);
		this.lbl = lbl;
		this.val = val;
		this.cb = cb;
		setSize(220,300);
		
	}
	public PanelScrollBar scroll;
    public SelectionList rl;
    public TextBox searchBox;
    public class SelectionList extends Panel{
		public SelectionList(ResearchSelectorDialog panel) {
			super(panel);
			this.setWidth(200);
		}

		@Override
		public void addWidgets() {
	        int offset = 0;
	        String stext=searchBox.getText();
	        for (Research r:FHResearch.getAllResearch()) {
	        	if(stext.isEmpty()||r.getId().contains(stext)||r.getName().getString().contains(stext)) {
		            ResearchButton button = new ResearchButton(this, r);
		            add(button);
		            button.setPos(4,offset);
		            offset += 18;
	        	}
	        }
	        this.setHeight(offset+1);
            scroll.setMaxValue(offset+1);
		}

		@Override
		public void alignWidgets() {
		}
    	
    }
    public class ResearchButton extends Button {
        Research research;
        SelectionList listPanel;
        RTextField tf;
        public ResearchButton(SelectionList panel, Research research) {
            super(panel, research.getName(), research.getIcon());
            this.research = research;
            this.listPanel =  panel;
            setSize(200,18);
            tf=new RTextField(panel).setMaxLine(1).setMaxWidth(180).setText(research.getName());
            tf.setWidth(180);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
        	cb.accept(research);
        	close();
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            //GuiHelper.setupDrawing();
			this.drawIcon(matrixStack, theme, x+1, y + 1,16,16);
			tf.draw(matrixStack, theme, x+18, y+6,180,tf.height);
			if(val==this.research)
				TechIcons.SELECTED.draw(matrixStack,x-4, y+7,4,4);
			TechIcons.HLINE.draw(matrixStack,x, y+17, 99, 1);
        }
    }

    @Override
    public void addWidgets() {
    	
    	rl=new SelectionList(this);
    	searchBox = new TextBox(this) {
			@Override
			public void onTextChanged() {
				rl.refreshWidgets();
			}
		};
		searchBox.ghostText = "Search research...";
		searchBox.setFocused(true);
		rl.setPos(0,20);
    	scroll=new TechScrollBar(this,rl);
    	add(rl);
    	add(scroll);
    	add(searchBox);
    	searchBox.setPosAndSize(0, 0, width,20);
    	scroll.setPos(200,20);
    	scroll.setSize(8,height);

    }

    @Override
    public void alignWidgets() {

    }
	@Override
	public void onClose() {
	}
	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.draw(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack,lbl,x, y-10);
	}
}
