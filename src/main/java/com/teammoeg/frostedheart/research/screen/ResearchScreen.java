package com.teammoeg.frostedheart.research.screen;

import static com.teammoeg.frostedheart.research.screen.ResearchCategoryPanel.CAT_PANEL_HEIGHT;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.*;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.entity.player.PlayerEntity;

public class ResearchScreen extends BaseScreen {

    public static final int PADDING = 2;
    private PlayerEntity player;
    public ResearchCategoryPanel researchCategoryPanel;
    public ResearchListPanel researchListPanel;
    public ResearchHierarchyPanel researchHierarchyPanel;
    public ResearchProgressPanel progressPanel;
    public ResearchCategory selectedCategory;
    public ResearchLevel researchLevel;
    public Research selectedResearch;
    public Research inProgressResearch;

    public ResearchScreen(PlayerEntity player, ResearchLevel level, Research progress) {
        this.player = player;
        this.researchLevel = level;
        researchCategoryPanel = new ResearchCategoryPanel(this);
        researchListPanel = new ResearchListPanel(this);
        researchHierarchyPanel = new ResearchHierarchyPanel(this);
        progressPanel = new ResearchProgressPanel(this);
        selectCategory(ResearchCategories.HEATING);
        inProgressResearch = progress; // nullable
        selectedResearch = FHResearch.researches.getByName("generator_t1");
    }

    @Override
    public void addWidgets() {
        add(researchCategoryPanel);
        add(researchListPanel);
        add(researchHierarchyPanel);
        add(progressPanel);
    }

    @Override
    public boolean onInit() {
    	setFullscreen();
    	researchCategoryPanel.setPosAndSize(PADDING,PADDING, this.width-PADDING*2, CAT_PANEL_HEIGHT);
    	researchListPanel.setPosAndSize(PADDING,PADDING + CAT_PANEL_HEIGHT + PADDING + IN_PROGRESS_HEIGHT + PADDING, RESEARCH_LIST_WIDTH, height - (PADDING*5 + CAT_PANEL_HEIGHT + IN_PROGRESS_HEIGHT));
    	researchHierarchyPanel.setPosAndSize(PADDING + RESEARCH_LIST_WIDTH + PADDING,PADDING + CAT_PANEL_HEIGHT + PADDING, width - (PADDING*5 + RESEARCH_LIST_WIDTH), height - (PADDING*4 + CAT_PANEL_HEIGHT));
        progressPanel.setPosAndSize(PADDING,PADDING + CAT_PANEL_HEIGHT + PADDING, RESEARCH_LIST_WIDTH, 80);
        return true;
    }

    public void selectCategory(@Nullable ResearchCategory category) {
        if (selectedCategory != category) {
            selectedCategory = category;
            researchCategoryPanel.refreshWidgets();
        }
    }

    public void selectResearch(@Nullable Research research) {
        if (selectedResearch != research) {
            selectedResearch = research;
            researchCategoryPanel.refreshWidgets();
        }
    }

    public Research getInProgressResearch() {
        return inProgressResearch;
    }

    public void setInProgressResearch(Research research) {
        inProgressResearch = research;
    }

    public static final int IN_PROGRESS_HEIGHT = 80;
    public static final int RESEARCH_LIST_WIDTH = 210;

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(matrixStack, theme, x, y, w, h);
//        theme.drawContainerSlot(matrixStack, x + 200, y + 16, 16, 16);
//        theme.drawButton(matrixStack, x + 200, y + 64, 16, 16, WidgetType.MOUSE_OVER);
//        theme.drawButton(matrixStack, x + 232, y + 64, 16, 16, WidgetType.DISABLED);
//        theme.drawButton(matrixStack, x + 264, y + 64, 16, 16, WidgetType.NORMAL);
//        theme.drawPanelBackground(matrixStack, x + 200, y + 100, 100, 50);
//        theme.drawHorizontalTab(matrixStack, x+ 200, y + 200, 50, 25, false);
//        theme.drawHorizontalTab(matrixStack, x+ 200, y + 230, 50, 25, true);
//        // in progress research panel
//        theme.drawPanelBackground(matrixStack, x + PADDING,y + PADDING + CAT_PANEL_HEIGHT + PADDING, IN_PROGRESS_WIDTH, IN_PROGRESS_HEIGHT);
//        // research list of current category panel
//        theme.drawPanelBackground(matrixStack, x + PADDING,y + PADDING + CAT_PANEL_HEIGHT + PADDING + IN_PROGRESS_HEIGHT + PADDING, RESEARCH_LIST_WIDTH, h - (PADDING + CAT_PANEL_HEIGHT + PADDING + PADDING + PADDING));
//        // research hierarchy of selected research
//        theme.drawPanelBackground(matrixStack, x + PADDING + IN_PROGRESS_WIDTH + PADDING,y + PADDING + CAT_PANEL_HEIGHT + PADDING, w - (PADDING + IN_PROGRESS_WIDTH + PADDING + PADDING), h - (PADDING + CAT_PANEL_HEIGHT + PADDING + PADDING));
    }
}
