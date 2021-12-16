package com.teammoeg.frostedheart.research.screen;

import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class ResearchHierarchyPanel extends Panel {
    public static class ResearchHierarchyLine extends ThickLine {
    	Research r;
		public ResearchHierarchyLine(Panel p,Research r) {
			super(p);
		}
		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			if(r.isCompleted())
				color=Color4I.GREEN;
			else
				color=Color4I.RED;
			super.draw(matrixStack, theme, x, y, w, h);
		}
		

	}

	public ResearchScreen researchScreen;

    public ResearchHierarchyPanel(ResearchScreen panel) {
        super(panel);
        researchScreen = panel;
    }

    @Override
    public void addWidgets() {
        ResearchDetailButton button = new ResearchDetailButton(this, researchScreen.selectedResearch);
        add(button);
        button.setPos((width - 64) / 2, (height - 48) / 2);
        int jointx=button.posX+button.width/2;
        int upjointy=button.posY;
        int downjointy=upjointy+button.height;
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
            l.setPosAndSize(jointx,upjointy,parentButton.posX+parentButton.width/2-jointx,parentButton.posY+parentButton.height-upjointy);
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
	            l.setPosAndSize(jointx,downjointy,childButton.posX+childButton.width/2-jointx,childButton.posY-downjointy);
	            
	            k++;
	        }
        }
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
        // title
        theme.drawString(matrixStack, GuiUtils.translateGui("research_hierarchy"), x + 10, y + 10);
        // horizontal line
        //GuiHelper.drawRectWithShade(matrixStack, x + 10, y + (w - 64) / 2 - 5, w - 20, 2, Color4I.BLACK, 128);

    }

    public static class ResearchDetailButton extends Button {

        ResearchScreen researchScreen;
        Research research;

        public ResearchDetailButton(ResearchHierarchyPanel panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.researchScreen = panel.researchScreen;
            setSize(64, 48);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            // todo: open research detail gui
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getDesc());
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            super.drawBackground(matrixStack, theme, x, y, w, h);
            this.drawIcon(matrixStack, theme, x + 16, y, 32, 32);
            theme.drawString(matrixStack, research.getName(), x + (w - theme.getStringWidth(research.getName())) / 2, y + 32);

        }
    }

    public static class ResearchSimpleButton extends Button {

        ResearchScreen researchScreen;
        Research research;

        public ResearchSimpleButton(ResearchHierarchyPanel panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.researchScreen = panel.researchScreen;
            setSize(32, 24);
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
            super.drawBackground(matrixStack, theme, x, y, w, h);
            this.drawIcon(matrixStack, theme, x + (w - 16) / 2, y, 16, 16);
        }
    }
}
