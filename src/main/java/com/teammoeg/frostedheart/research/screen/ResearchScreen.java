package com.teammoeg.frostedheart.research.screen;

import static com.teammoeg.frostedheart.research.screen.ResearchCategoryPanel.CAT_PANEL_HEIGHT;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;
import com.teammoeg.frostedheart.research.ResearchLevel;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import net.minecraft.entity.player.PlayerEntity;

public class ResearchScreen extends BaseScreen {

    public static final int PADDING = 2;
    PlayerEntity player;
    public ResearchCategoryPanel researchCategoryPanel;
    public ResearchListPanel researchListPanel;
    public ResearchHierarchyPanel researchHierarchyPanel;
    public ResearchProgressPanel progressPanel;
    public ResearchCategory selectedCategory;
    public ResearchLevel researchLevel;
    public Research selectedResearch;
    public Research inProgressResearch;
    public ResearchDetailPanel detailframe;
    public ResearchScreen(PlayerEntity player, ResearchLevel level, Research progress) {
        this.player = player;
        this.researchLevel = level;
        researchCategoryPanel = new ResearchCategoryPanel(this);
        researchListPanel = new ResearchListPanel(this);
        researchHierarchyPanel = new ResearchHierarchyPanel(this);
        progressPanel = new ResearchProgressPanel(this);
        inProgressResearch = progress; // nullable
        detailframe=new ResearchDetailPanel(this);
    }

    @Override
    public void addWidgets() {
        add(researchCategoryPanel);
        add(researchListPanel);
        add(researchHierarchyPanel);
        add(progressPanel);
        add(detailframe);
    }

    @Override
    public boolean onInit() {
    	setFullscreen();
        selectCategory(ResearchCategories.RESCUE);
        selectedResearch = FHResearch.researches.getByName("generator_t1");
    	researchCategoryPanel.setPosAndSize(PADDING,PADDING, this.width-PADDING*2, CAT_PANEL_HEIGHT);
    	researchListPanel.setPosAndSize(PADDING,PADDING + CAT_PANEL_HEIGHT + PADDING + IN_PROGRESS_HEIGHT + PADDING, RESEARCH_LIST_WIDTH, height - (PADDING*5 + CAT_PANEL_HEIGHT + IN_PROGRESS_HEIGHT));
    	researchHierarchyPanel.setPosAndSize(PADDING + RESEARCH_LIST_WIDTH + PADDING,PADDING + CAT_PANEL_HEIGHT + PADDING, width - (PADDING*5 + RESEARCH_LIST_WIDTH), height - (PADDING*4 + CAT_PANEL_HEIGHT));
        progressPanel.setPosAndSize(PADDING,PADDING + CAT_PANEL_HEIGHT + PADDING, RESEARCH_LIST_WIDTH, 80);
        detailframe.setPosAndSize((super.width-200)/2,(super.height-100)/2,200,100);
        return true;
    }

    public void selectCategory(@Nullable ResearchCategory category) {
        if (selectedCategory != category) {
            selectedCategory = category;
            this.refreshWidgets();
        }
    }

    public void selectResearch(@Nullable Research research) {
        if (selectedResearch != research) {
            selectedResearch = research;
            this.refreshWidgets();
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
    }

	/*@Override
	public void closeGui() {
		if(detailframe.r!=null)
			detailframe.r=null;
		else
			super.closeGui();
	}
*/
	@Override
	public void onBack() {
		if(detailframe.r!=null)
			detailframe.r=null;
		else super.onBack();
	}


	/*@Override
	public boolean onClosedByKey(Key key) {
		if(detailframe.r!=null) {
			detailframe.r=null;
			return false;
		}
		return super.onClosedByKey(key);
	}*/
}
