package com.teammoeg.frostedheart.research.screen;

import static com.teammoeg.frostedheart.research.screen.ResearchCategoryPanel.CAT_PANEL_HEIGHT;
import static com.teammoeg.frostedheart.research.screen.ResearchCategoryPanel.CAT_PANEL_WIDTH;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;
import com.teammoeg.frostedheart.research.ResearchLevel;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.entity.player.PlayerEntity;

public class ResearchScreen extends BaseScreen {

    public static final int PADDING = 10;
    private PlayerEntity player;
    public ResearchCategoryPanel researchCategoryPanel;
    public ResearchListPanel researchListPanel;
    public ResearchHierarchyPanel researchHierarchyPanel;
    public ResearchCategory selectedCategory;
    public ResearchLevel researchLevel;
    public Research selectedResearch;

    public ResearchScreen(PlayerEntity player, ResearchLevel level) {
        this.player = player;
        this.researchLevel = level;
        researchCategoryPanel = new ResearchCategoryPanel(this);
        researchListPanel = new ResearchListPanel(this);
        researchHierarchyPanel = new ResearchHierarchyPanel(this);
        selectCategory(ResearchCategories.HEATING);
    }

    @Override
    public void addWidgets() {
        researchCategoryPanel.setPosAndSize(PADDING, PADDING, CAT_PANEL_WIDTH, CAT_PANEL_HEIGHT);
        add(researchCategoryPanel);
        researchListPanel.setPosAndSize(getX() + PADDING,getY() + PADDING + CAT_PANEL_HEIGHT + PADDING + IN_PROGRESS_HEIGHT + PADDING, RESEARCH_LIST_WIDTH, height - (PADDING + CAT_PANEL_HEIGHT + PADDING + PADDING + PADDING));
        add(researchListPanel);
        researchHierarchyPanel.setPosAndSize(getX() + PADDING + IN_PROGRESS_WIDTH + PADDING,getY() + PADDING + CAT_PANEL_HEIGHT + PADDING, width - (PADDING + IN_PROGRESS_WIDTH + PADDING + PADDING), height - (PADDING + CAT_PANEL_HEIGHT + PADDING + PADDING));
        add(researchHierarchyPanel);
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
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

    public static final int IN_PROGRESS_WIDTH = 80, IN_PROGRESS_HEIGHT = 80;
    public static final int RESEARCH_LIST_WIDTH = 80;

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
