package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class ResearchHierarchyPanel extends Panel {
    public ResearchScreen researchScreen;

    public ResearchHierarchyPanel(Panel panel) {
        super(panel);
        researchScreen = (ResearchScreen) panel.getGui();
    }

    @Override
    public void addWidgets() {

    }

    @Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawPanelBackground(matrixStack, x, y, w, h);
	}

	@Override
    public void alignWidgets() {

    }
}
