package com.teammoeg.frostedheart.research.screen;

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

        int k = 0;
        for (Research parent : researchScreen.selectedResearch.getParents()) {
            if (k > 4) break;
            ResearchSimpleButton parentButton = new ResearchSimpleButton(this, parent);
            add(parentButton);
            parentButton.setPos((width - 34 * researchScreen.selectedResearch.getParents().size()) / 2 + k * 34, (height / 2 - 24) / 2);
            k++;
        }

        k = 0;
        for (Research child : researchScreen.selectedResearch.getChildren()) {
            if (k > 4) break;
            ResearchSimpleButton childButton = new ResearchSimpleButton(this, child);
            add(childButton);
            childButton.setPos((width - 34 * researchScreen.selectedResearch.getChildren().size()) / 2 + k * 34, (height / 2 - 24) / 2 + height / 2);
            k++;
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
        FHGuiHelper.drawLine(matrixStack,0xFF000000, (width - 64) / 2, (height - 48) / 2,0,0);
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
            GuiHelper.drawRectWithShade(matrixStack, x + 31, y - 5, 2, 5, Color4I.BLACK, 128);

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
            if (research.isUnlocked()) {
                researchScreen.selectResearch(research);
            }
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
            GuiHelper.drawRectWithShade(matrixStack, x + (w - 16) / 2 + 16/2 - 1, y + 16 + 5, 2, 5, Color4I.BLACK, 128);
        }
    }
}
