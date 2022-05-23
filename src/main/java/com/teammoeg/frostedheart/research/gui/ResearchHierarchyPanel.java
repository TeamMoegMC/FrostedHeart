package com.teammoeg.frostedheart.research.gui;

import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.TextFormatting;

public class ResearchHierarchyPanel extends Panel {
    public static class ResearchHierarchyLine extends ThickLine {
    	Research r;
		public ResearchHierarchyLine(Panel p,Research r) {
			super(p);
			this.r=r;
		}
		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			if(r.isCompleted())
				color=Color4I.rgb(0x474139);
			else
				color=Color4I.rgb(0xADA691);
			super.draw(matrixStack, theme, x, y, w, h);
		}
		

	}

	public ResearchScreen researchScreen;

    public ResearchHierarchyPanel(ResearchScreen panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        researchScreen = panel;
    }

    @Override
    public void addWidgets() {
        ResearchDetailButton button = new ResearchDetailButton(this, researchScreen.selectedResearch);
        add(button);
        button.setPos((width - 64) / 2, (height - 48) / 2);
        int jointx=button.posX+button.width/2;
        int upjointy=button.posY+2;
        int downjointy=upjointy+button.height-4;
        int k = 0;
        Set<Research> parents=researchScreen.selectedResearch.getParents();
        int psize=parents.size()>4?5:parents.size();
        for (Research parent : parents) {
            if (k > 4) break;
            ResearchSimpleButton parentButton = new ResearchSimpleButton(this, parent);
            add(parentButton);
            parentButton.setPos((width - 34 * psize) / 2 + k * 34, (height / 2 - 24) / 2);
            ThickLine l=new ResearchHierarchyLine(this,parent);
            add(l);
            l.setPosAndSize(jointx,upjointy,parentButton.posX+parentButton.width/2-jointx,parentButton.posY+parentButton.height-upjointy-4);
            k++;
        }

        k = 0;
        if(researchScreen.selectedResearch.isUnlocked()) {
	        Set<Research> children=researchScreen.selectedResearch.getChildren();
	        int csize=children.size()>4?5:children.size();
	        for (Research child : children) {
	            if (k > 4) break;
	            ResearchSimpleButton childButton = new ResearchSimpleButton(this, child);
	            add(childButton);
	            childButton.setPos((width - 34 * csize) / 2 + k * 34, (height / 2 - 24) / 2 + height / 2);
	            ThickLine l=new ResearchHierarchyLine(this,researchScreen.selectedResearch);
	            add(l);
	            l.setPosAndSize(jointx,downjointy,childButton.posX+childButton.width/2-jointx,childButton.posY-downjointy+6);
	            
	            k++;
	        }
        }
    }
    
	@Override
    public void alignWidgets() {

    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        //theme.drawPanelBackground(matrixStack, x, y, w, h);
    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        theme.drawString(matrixStack, GuiUtils.translateGui("research_hierarchy"), x + 3, y + 3,Color4I.rgb(0x474139),0);
        DrawDeskIcons.HLINE_L.draw(matrixStack, x+1, y+13,80,3);
    }

    public static class ResearchDetailButton extends Button {

        ResearchScreen researchScreen;
        Research research;

        public ResearchDetailButton(ResearchHierarchyPanel panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.researchScreen = panel.researchScreen;
            setSize(36, 36);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
        	this.researchScreen.detailframe.open(research);
        	this.researchScreen.refreshWidgets();
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getName().mergeStyle(TextFormatting.BOLD));
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            //this.drawBackground(matrixStack, theme, x, y, w, h);
        	DrawDeskIcons.LSLOT.draw(matrixStack, x, y, w, h);
            this.drawIcon(matrixStack, theme, x + 2, y+2, 32, 32);
        }
    }

    public static class ResearchSimpleButton extends Button {

        ResearchScreen researchScreen;
        Research research;

        public ResearchSimpleButton(ResearchHierarchyPanel panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.researchScreen = panel.researchScreen;
            setSize(24, 24);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
                researchScreen.selectResearch(research);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getName().mergeStyle(TextFormatting.BOLD));
            if (!research.isUnlocked()) {
                list.add(GuiUtils.translateTooltip("research_is_locked").mergeStyle(TextFormatting.RED));
                for (Research parent : research.getParents()) {
                    if (!parent.isCompleted()) {
                        list.add(parent.getName().mergeStyle(TextFormatting.GRAY));
                    }
                }
            }
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        	GuiHelper.setupDrawing();
        	DrawDeskIcons.SLOT.draw(matrixStack, x, y, w, h);
        	if(research.isUnlocked())
        		this.drawIcon(matrixStack, theme, x + 4, y+4, 16, 16);
        	else
        		DrawDeskIcons.Question.draw(matrixStack, x+4, y+4,16,16);
        }
    }
	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}
}
