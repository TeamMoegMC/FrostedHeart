package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class ResearchListPanel extends Panel {
    public static final int RESEARCH_WIDTH = 80, RESEARCH_HEIGHT = 18;
    public static final int RES_ICON_WIDTH = 16, RES_ICON_HEIGHT = 16;
    public static final int RES_PANEL_WIDTH = 80;

    public ResearchScreen researchScreen;

    public ResearchListPanel(Panel panel) {
        super(panel);
        researchScreen = (ResearchScreen) panel.getGui();
    }

    public static class ResearchButton extends Button {

        Research research;
        ResearchListPanel listPanel;

        public ResearchButton(Panel panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.listPanel = (ResearchListPanel) panel;
            setSize(RESEARCH_WIDTH, RESEARCH_HEIGHT);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            listPanel.researchScreen.selectResearch(research);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getDesc());
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            super.draw(matrixStack, theme, x, y, w, h);
//            theme.drawHorizontalTab(matrixStack, x, y, w, h, listPanel.researchScreen.selectedResearch == research);
            this.drawIcon(matrixStack, theme, x + 2, y + 2, RES_ICON_WIDTH, RES_ICON_HEIGHT);
            theme.drawString(matrixStack, research.getName(), x + RES_ICON_WIDTH + 4, y + RES_ICON_HEIGHT /2 - 4);
        }
    }

    @Override
    public void addWidgets() {
        int offset = 0;

        for (int k = 0; k < FHResearch.researches.getSize(); k++) {
            Research research = FHResearch.researches.getById(k);
            if (research.getCategory() == researchScreen.selectedCategory) {
                if (research.isUnlocked()) {
                    // first show in progress researches
                    if (research.getData().isInProgress()) {
                        ResearchButton button = new ResearchButton(this, research);
                        button.setPos(getX(), getY() + k * (RESEARCH_WIDTH + 4));
                        add(button);
                    }
                }
            }
            offset = k;
        }

        for (int k = 0; k < FHResearch.researches.getSize(); k++) {
            Research research = FHResearch.researches.getById(k);
            if (research.getCategory() == researchScreen.selectedCategory) {
                if (research.isUnlocked()) {
                    // then show unlocked but not in progress researches
                    if (!research.getData().isInProgress()) {
                        ResearchButton button = new ResearchButton(this, research);
                        button.setPos(getX(), getY() + (offset + k) * (RESEARCH_WIDTH + 4));
                        add(button);
                    }
                }
            }
            offset += k;
        }

        for (int k = 0; k < FHResearch.researches.getSize(); k++) {
            Research research = FHResearch.researches.getById(k);
            if (research.getCategory() == researchScreen.selectedCategory) {
                // show the rest researches
                if (!research.isUnlocked()) {
                    ResearchButton button = new ResearchButton(this, research);
                    button.setPos(getX(), getY() + (offset + k) * (RESEARCH_WIDTH + 4));
                    add(button);
                }
            }
        }
    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        for (int k = 0; k < widgets.size(); k++) {
            widgets.get(k).draw(matrixStack, theme, x, y+ k * (RESEARCH_WIDTH + 4), w, h);
        }
    }
}

