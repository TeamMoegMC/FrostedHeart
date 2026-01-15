/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedresearch.gui.tech;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.ClientResearchData;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.TechScrollBar;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;

import net.minecraft.client.gui.GuiGraphics;

public abstract class ResearchPanel extends UILayer {

    public static final int PADDING = 2;
    public static final int IN_PROGRESS_HEIGHT = 80;
    public static final int RESEARCH_LIST_WIDTH = 210;
    public ResearchCategoryPanel researchCategoryPanel;
    public ResearchListPanel researchListPanel;
    public ResearchHierarchyPanel researchHierarchyPanel;
    public ResearchProgressPanel progressPanel;
    public ResearchCategory selectedCategory;
    public Research selectedResearch;
    public ResearchDetailPanel detailframe;

    public UIElement modalPanel = null;

    public TechScrollBar hierarchyBar;


    boolean enabled;

    public ResearchPanel(UIElement p) {
        super(p);
        researchCategoryPanel = new ResearchCategoryPanel(this);
        researchListPanel = new ResearchListPanel(this);
        researchHierarchyPanel = new ResearchHierarchyPanel(this);
        progressPanel = new ResearchProgressPanel(this) {
            @Override
            public boolean onMousePressed(MouseButton arg0) {
                if (super.onMousePressed(arg0))
                    return true;
                if (isMouseOver()) {
                    Research inprog = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
                    if (inprog != null) {
                        selectResearch(inprog);
                        return true;
                    }
                }
                return false;
            }
        };
        hierarchyBar = new TechScrollBar(this, false, researchHierarchyPanel);
        detailframe = new ResearchDetailPanel(this);
        selectedCategory=ResearchCategory.RESCUE;
        selectedResearch=null;
    }


    @Override
    public void getTooltip(TooltipBuilder list) {
        list.translateZ(350);
        super.getTooltip(list);
    }
    boolean isFirstPassed=true;
    @Override
    public void addUIElements() {
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
        if(isFirstPassed) {
	        Research cr = null;
	        if (ClientResearchData.last != null) {
	            cr = FHResearch.researches.get(ClientResearchData.last);
	        }
	        if(cr==null)
	            cr = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
	        if(cr==null)
	        	cr= FHResearch.getFirstResearchInCategory(selectedCategory);
	        selectedCategory = cr == null ? ResearchCategory.RESCUE : cr.getCategory();
	        selectedResearch = cr;
	        isFirstPassed=false;
        }
        researchCategoryPanel.setPosAndSize(165, 0, 190, 21);
        researchListPanel.setPosAndSize(12, 74, 114, 118);
        researchHierarchyPanel.setPosAndSize(160, 23, 210, 160);
        progressPanel.setPosAndSize(14, 19, 111, 51);
        detailframe.setPosAndSize((width - 302) / 2, (height - 170) / 2, 302, 170);
        hierarchyBar.setPosAndSize(170, 175, 190, 8);
        add(researchCategoryPanel);
        add(researchListPanel);
        add(researchHierarchyPanel);
        add(progressPanel);
        add(detailframe);
        add(hierarchyBar);
    }

    @Override
    public void alignWidgets() {
    }

    public boolean canEnable(UIElement p) {
        return modalPanel == null || modalPanel == p;
    }

    public void closeModal(UIElement p) {
        if (p == modalPanel)
            modalPanel = null;
    }

    @Override
    public void render(GuiGraphics arg0, int arg2, int arg3, int arg4, int arg5) {
        if (enabled)
            super.render(arg0, arg2, arg3, arg4, arg5);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        TechIcons.Background.draw(matrixStack, x, y, w, h);
    }


    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean onKeyPressed(int keyCode,int scanCode,int modifier) {
        if (CInputHelper.isEsc(keyCode)) {
            if (modalPanel != null) {
                detailframe.close();
                return true;
            }
            this.onDisabled();
            //this.closeGui(true);
            return true;
        }
        return super.onKeyPressed(keyCode,scanCode,modifier);
    }

    public abstract void onDisabled();

    public void selectCategory(ResearchCategory category) {
        if (selectedCategory != category) {
            selectedCategory = category;
            /*if (FHResearch.getFirstResearchInCategory(category) != null)
				selectResearch(FHResearch.getFirstResearchInCategory(category));*/
            this.refresh();
        }
    }

    public void selectResearch(Research research) {
        if (selectedResearch != research) {
            ClientResearchData.last = research.getId();
            selectedResearch = research;
            if (selectedResearch != null)
                selectCategory(selectedResearch.getCategory());
            researchHierarchyPanel.refresh();
        } else if (FHResearch.isEditor() || (research.isUnlocked() && !research.isHidden())) {
            detailframe.open(research);
        }
    }

    public void setModal(UIElement p) {
        modalPanel = p;
    }


}
