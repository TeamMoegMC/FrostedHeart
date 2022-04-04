package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.item.ItemStack;

public class ResearchProgressPanel extends Panel {
    public ResearchScreen researchScreen;

    public ResearchProgressPanel(ResearchScreen panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        researchScreen = panel;
    }

    @Override
    public void addWidgets() {

    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        // title
        theme.drawString(matrixStack, GuiUtils.translateGui("research_progress"), x + 10, y + 10);
        // progress bar
        // TODO: this cause crash when root clue is added
        // float progress = researchScreen.getInProgressResearch().getProgressFraction();
        // float reqTime = researchScreen.getInProgressResearch().getCurrentPoints();
        // float finTIme = researchScreen.getInProgressResearch().getRequiredPoints();
        // GuiHelper.drawHollowRect(matrixStack, x + 10, y + 20, w - 20, 5, Color4I.BLACK, false);
        // GuiHelper.drawRectWithShade(matrixStack, x + 10, y + 20, (int) ( (w - 20) * progress), 5, Color4I.WHITE, 128);
        // theme.drawString(matrixStack, GuiUtils.translateGui("research_time_left", (int) ( (reqTime - finTIme) / 20 / 60) ), x + 10, y + 30);

        // research icon
        GuiHelper.drawItem(matrixStack,researchScreen.getInProgressResearch().getIcon(), x + 10, y + 40, 2, 2, false, null);
        theme.drawString(matrixStack, researchScreen.getInProgressResearch().getName(), x + 50, y + 45);
        // theme.drawString(matrixStack, researchScreen.getInProgressResearch().getDesc(), x + 50, y + 55, Color4I.GRAY, 0);

    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        theme.drawPanelBackground(matrixStack, x, y, w, h);
    }
}
