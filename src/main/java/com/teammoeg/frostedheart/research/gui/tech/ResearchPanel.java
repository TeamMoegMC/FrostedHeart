/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ClientResearchData;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechScrollBar;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.research.research.ResearchCategory;

import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.ScrollBar.Plane;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public abstract class ResearchPanel extends Panel {

    public static final int PADDING = 2;
    public ResearchCategoryPanel researchCategoryPanel;
    public ResearchListPanel researchListPanel;
    public ResearchHierarchyPanel researchHierarchyPanel;
    public ResearchProgressPanel progressPanel;
    public ResearchCategory selectedCategory;
    public Research selectedResearch;
    public ResearchDetailPanel detailframe;
    public Panel modalPanel = null;
    public TechScrollBar hierarchyBar;

    public ResearchPanel(Panel p) {
        super(p);
        researchCategoryPanel = new ResearchCategoryPanel(this);
        researchListPanel = new ResearchListPanel(this);
        researchHierarchyPanel = new ResearchHierarchyPanel(this);
        progressPanel = new ResearchProgressPanel(this) {
            @Override
            public boolean mousePressed(MouseButton arg0) {
                if (super.mousePressed(arg0))
                    return true;
                if (isMouseOver()) {
                    Research inprog = ClientResearchDataAPI.getData().getCurrentResearch().orElse(null);
                    if (inprog != null) {
                        selectResearch(inprog);
                        return true;
                    }
                }
                return false;
            }
        };
        hierarchyBar = new TechScrollBar(this, Plane.HORIZONTAL, researchHierarchyPanel);
        detailframe = new ResearchDetailPanel(this);
        //TODO default select on progress research
        Research cr = null;
        if (ClientResearchData.last != null && ClientResearchData.last.getRId() > 0)
            cr = ClientResearchData.last;
        else
            cr = ClientResearchDataAPI.getData().getCurrentResearch().orElse(null);
        selectedCategory = cr == null ? ResearchCategory.RESCUE : cr.getCategory();
        selectedResearch = cr == null ? FHResearch.getFirstResearchInCategory(selectedCategory) : cr;
    }

    @Override
    public void addWidgets() {
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);

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


    public void selectCategory(ResearchCategory category) {
        if (selectedCategory != category) {
            selectedCategory = category;
            /*if (FHResearch.getFirstResearchInCategory(category) != null)
				selectResearch(FHResearch.getFirstResearchInCategory(category));*/
            this.refreshWidgets();
        }
    }

    public void selectResearch(Research research) {
        if (selectedResearch != research) {
            ClientResearchData.last = research;
            selectedResearch = research;
            if (selectedResearch != null)
                selectCategory(selectedResearch.getCategory());
            researchHierarchyPanel.refreshWidgets();
        } else if (FHResearch.isEditor() || (research.isUnlocked() && !research.isHidden())) {
            detailframe.open(research);
        }
    }


    public static final int IN_PROGRESS_HEIGHT = 80;
    public static final int RESEARCH_LIST_WIDTH = 210;

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        TechIcons.Background.draw(matrixStack, x, y, w, h);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        list.zOffset = 950;
        list.zOffsetItemTooltip = 500;
        super.addMouseOverText(list);
    }

    public void setModal(Panel p) {
        modalPanel = p;
    }

    public void closeModal(Panel p) {
        if (p == modalPanel)
            modalPanel = null;
    }

    public boolean canEnable(Panel p) {
        return modalPanel == null || modalPanel == p;
    }

    @Override
    public void drawWidget(MatrixStack arg0, Theme arg1, Widget arg2, int arg3, int arg4, int arg5, int arg6,
                           int arg7) {
        GuiHelper.setupDrawing();
        super.drawWidget(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }


    @Override
    public boolean keyPressed(Key key) {
        if (key.esc()) {
            if (modalPanel != null) {
                detailframe.close();
                return true;
            }
            this.onDisabled();
            //this.closeGui(true);
            return true;
        }
        return super.keyPressed(key);
    }

    public abstract void onDisabled();

    @Override
    public void alignWidgets() {
    }

    boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void draw(MatrixStack arg0, Theme arg1, int arg2, int arg3, int arg4, int arg5) {
        if (enabled)
            super.draw(arg0, arg1, arg2, arg3, arg4, arg5);
    }


}
