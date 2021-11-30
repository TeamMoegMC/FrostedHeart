package com.teammoeg.frostedheart.research.screen;

import dev.ftb.mods.ftblibrary.ui.Panel;

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
    public void alignWidgets() {

    }
}
