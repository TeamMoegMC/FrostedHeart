package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class ResearchListPanel extends Panel {

    public static final int RESEARCH_WIDTH = 200, RESEARCH_HEIGHT = 18;
    public static final int RES_ICON_WIDTH = 16, RES_ICON_HEIGHT = 16;
    public static final int RES_PANEL_WIDTH = 80;

    public ResearchScreen researchScreen;
    public PanelScrollBar scroll;
    public ResearchList rl;
    public ResearchListPanel(ResearchScreen panel) {
        super(panel);
        researchScreen = panel;
    }
    public static class ResearchList extends Panel{
    	public ResearchScreen researchScreen;
		public ResearchList(ResearchListPanel panel) {
			super(panel);
			researchScreen=panel.researchScreen;
			this.setWidth(RESEARCH_WIDTH);
		}

		@Override
		public void addWidgets() {
	        int offset = 0;

	        for (Research r:FHResearch.getResearchesForRender(this.researchScreen.selectedCategory, true)) {
	            ResearchButton button = new ResearchButton(this, r);
	            add(button);
	            button.setPos(0,offset);
	            offset += (RESEARCH_HEIGHT);
	        }
	        this.setHeight(offset);
		}

		@Override
		public void alignWidgets() {
		}
    	
    }
    public static class ResearchButton extends Button {

        Research research;
        ResearchList listPanel;

        public ResearchButton(ResearchList panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.listPanel =  panel;
            setSize(RESEARCH_WIDTH, RESEARCH_HEIGHT);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            listPanel.researchScreen.selectResearch(research);
        }

//        @Override
//        public void addMouseOverText(TooltipList list) {
//            list.add(research.getDesc());
//        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.drawBackground(matrixStack, theme, x, y, w, h);
            //theme.drawHorizontalTab(matrixStack, x, y, w, h, categoryPanel.researchScreen.selectedCategory == category);
			
			this.drawIcon(matrixStack, theme, x + 2, y + 2, RES_ICON_WIDTH, RES_ICON_HEIGHT);
//            theme.drawHorizontalTab(matrixStack, x, y, w, h, listPanel.researchScreen.selectedResearch == research);
//            this.drawIcon(matrixStack, theme, x + 2, y + 2, RES_ICON_WIDTH, RES_ICON_HEIGHT);
			if(research.isCompleted()) {
				 theme.drawString(matrixStack,research.getName(), x + RES_ICON_WIDTH + 4, y + RES_ICON_HEIGHT /2 - 4,Color4I.GREEN,0);
			}else if(!research.isUnlocked()) {
				theme.drawString(matrixStack,research.getName(), x + RES_ICON_WIDTH + 4, y + RES_ICON_HEIGHT /2 - 4,Color4I.RED,0);
			}else
            theme.drawString(matrixStack, research.getName(), x + RES_ICON_WIDTH + 4, y + RES_ICON_HEIGHT /2 - 4);
        }
    }

    @Override
    public void addWidgets() {
    	rl=new ResearchList(this);
    	scroll=new PanelScrollBar(this,rl);
    	add(rl);
    	add(scroll);
    	scroll.setX(RESEARCH_WIDTH);
    	scroll.setSize(10,height);
    }

    @Override
    public void alignWidgets() {

    }

    @Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawPanelBackground(matrixStack, x, y, w, h);
	}

	@Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
    }
}

