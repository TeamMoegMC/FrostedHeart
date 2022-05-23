package com.teammoeg.frostedheart.research.gui;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;
import com.teammoeg.frostedheart.research.ResearchLevel;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
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
    public Panel modalPanel=null;
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
    	int sw=387;
    	int sh=203;
    	this.setSize(sw,sh);
        selectCategory(ResearchCategories.RESCUE);
        selectedResearch = FHResearch.researches.getByName("generator_t1");
    	researchCategoryPanel.setPosAndSize(165,0,190,21);
    	researchListPanel.setPosAndSize(16,74,110,118);
    	researchHierarchyPanel.setPosAndSize(160,23,210,160);
        progressPanel.setPosAndSize(14,19,111,51);
        detailframe.setPosAndSize((width-300)/2,(height-150)/2,300,150);
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
        }else {
        	detailframe.open(research);
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
        DrawDeskIcons.Background.draw(matrixStack, x, y, w, h);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        list.zOffset = 950;
        list.zOffsetItemTooltip = 500;
        super.addMouseOverText(list);
    }

	@Override
	public void onBack() {
		if(detailframe.research !=null) {
            detailframe.close();
        }
		else super.onBack();
	}
	public void setModal(Panel p) {
		modalPanel=p;
	}
	public void closeModal(Panel p) {
		if(p==modalPanel)
		modalPanel=null;
	}
	public boolean canEnable(Panel p) {
		return modalPanel==null||modalPanel==p;
	}

	@Override
	public void drawWidget(MatrixStack arg0, Theme arg1, Widget arg2, int arg3, int arg4, int arg5, int arg6,
			int arg7) {
		GuiHelper.setupDrawing();
		super.drawWidget(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
	}
}
