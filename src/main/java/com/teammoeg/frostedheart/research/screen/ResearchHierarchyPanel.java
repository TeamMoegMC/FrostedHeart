package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.Research;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

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

        for (Research parent : researchScreen.selectedResearch.getParents()) {
            // todo: draw a small button
            // todo: draw a connection line
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
            theme.drawString(matrixStack, researchScreen.selectedResearch.getName(), x, y + 32);
        }
    }
}
