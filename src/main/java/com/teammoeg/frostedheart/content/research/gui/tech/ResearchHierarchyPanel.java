/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui.tech;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.content.research.gui.TechTextButton;
import com.teammoeg.frostedheart.content.research.gui.ThickLine;
import com.teammoeg.frostedheart.content.research.gui.editor.EditUtils;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.ResearchEditorDialog;
import com.teammoeg.frostedheart.util.TranslateUtils;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;

public class ResearchHierarchyPanel extends Panel {
    public static class MoreResearchHierarchyLine extends ThickLine {
        List<Research> r;

        public MoreResearchHierarchyLine(List<Research> r) {
            this.r = r;
        }

        public boolean doShow() {
            return r.stream().allMatch(Research::isCompleted);
        }

        @Override
        public void draw(GuiGraphics matrixStack, int x, int y) {
            if (doShow())
                color = TechIcons.text;
            else
                color = Color4I.rgb(0xADA691);
            super.draw(matrixStack, x, y);
        }
    }

    public static class ResearchCombinatorLine extends ResearchHierarchyLine {

        public ResearchCombinatorLine(Research r) {
            super(r);
        }

        public boolean doShow() {
            return r.isUnlocked();
        }

    }

    public static class ResearchDetailButton extends Button {

        ResearchPanel researchScreen;
        Research research;

        public ResearchDetailButton(ResearchHierarchyPanel panel, Research research) {
            super(panel, research.getName(), research.getIcon());
            this.research = research;
            this.researchScreen = panel.researchPanel;
            setSize(36, 36);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getName().withStyle(ChatFormatting.BOLD));
            if (!research.isUnlocked()) {
                list.add(TranslateUtils.translateTooltip("research_is_locked").withStyle(ChatFormatting.RED));
                for (Research parent : research.getParents()) {
                    if (!parent.isCompleted()) {
                        list.add(parent.getName().withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }

        @Override
        public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
            // this.drawBackground(matrixStack, theme, x, y, w, h);
            GuiHelper.setupDrawing();
            TechIcons.LSLOT.draw(matrixStack, x, y, w, h);
            if (FHResearch.editor || research.isShowable()) {
                this.drawIcon(matrixStack, theme, x + 2, y + 2, 32, 32);
                if (research.isCompleted()) {
                    matrixStack.pushPose();
                    matrixStack.translate(0, 0, 300);
                    GuiHelper.setupDrawing();
                    TechIcons.FIN.draw(matrixStack, x + 2, y + 2, 32, 32);
                    matrixStack.popPose();
                }
            } else
                TechIcons.Question.draw(matrixStack, x + 2, y + 2, 32, 32);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            if ((research.isShowable() && !research.isHidden()) || FHResearch.isEditor())
                this.researchScreen.detailframe.open(research);
            // this.researchScreen.refreshWidgets();
        }
    }

    public static class ResearchHierarchyLine extends ThickLine {
        Research r;

        public ResearchHierarchyLine(Research r) {
            this.r = r;
        }

        public boolean doShow() {
            return r.isCompleted();
        }

        @Override
        public void draw(GuiGraphics matrixStack, int x, int y) {
            if (doShow())
                color = TechIcons.text;
            else
                color = Color4I.rgb(0xADA691);
            super.draw(matrixStack, x, y);
        }
    }

    public static class ResearchSimpleButton extends Button {

        ResearchPanel researchScreen;
        Research research;
        Research parent;

        public ResearchSimpleButton(ResearchHierarchyPanel panel, Research research) {
            super(panel, research.getName(), research.getIcon());
            this.research = research;
            this.researchScreen = panel.researchPanel;
            setSize(24, 24);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getName().withStyle(ChatFormatting.BOLD));
            if ((parent == null && !research.isUnlocked()) || (parent != null && !parent.isUnlocked())) {
                list.add(TranslateUtils.translateTooltip("research_is_locked").withStyle(ChatFormatting.RED));
                for (Research parent : research.getParents()) {
                    if (!parent.isCompleted()) {
                        list.add(parent.getName().withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }

        @Override
        public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            TechIcons.SLOT.draw(matrixStack, x, y, w, h);
            if (FHResearch.editor || research.isShowable()) {
                this.drawIcon(matrixStack, theme, x + 4, y + 4, 16, 16);
                if (research.isCompleted()) {
                    matrixStack.pushPose();
                    matrixStack.translate(0, 0, 300);
                    GuiHelper.setupDrawing();
                    TechIcons.FIN.draw(matrixStack, x + 4, y + 4, 16, 16);
                    matrixStack.popPose();
                }
            } else
                TechIcons.Question.draw(matrixStack, x + 4, y + 4, 16, 16);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            researchScreen.selectResearch(research);
        }

        public void setChildren(Research p) {
            parent = p;
        }
    }

    private static int[] ButtonPos = new int[]{76, 44, 108, 12, 140};
    public ResearchPanel researchPanel;

    List<ThickLine> lines = new ArrayList<>();

    public ResearchHierarchyPanel(ResearchPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        researchPanel = panel;
    }

    @Override
    public void addWidgets() {
        if (FHResearch.editor) {
            int offset = 5;
            if (researchPanel.selectedResearch != null) {
                Button par = new TechTextButton(this, TranslateUtils.str("parents"), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {
                        // TODO Add parent
                        Research r = researchPanel.selectedResearch;
                        ResearchEditorDialog.RESEARCH_LIST.open(this, "Edit parents", r.getParents(), s -> {
                            r.setParents(s.stream().map(Research::getSupplier).collect(Collectors.toList()));
                            FHResearch.reindex();
                            EditUtils.saveResearch(r);
                        });
                    }
                };
                par.setPos(offset, 130);
                add(par);
                offset += par.width + 3;
                Button chd = new TechTextButton(this, TranslateUtils.str("children"), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {
                        // TODO Add children
                        Research r = researchPanel.selectedResearch;
                        ResearchEditorDialog.RESEARCH_LIST.open(this, "Edit children", r.getChildren(), s -> {
                            r.getChildren().forEach(e -> {
                                e.removeParent(r);
                                EditUtils.saveResearch(e);
                            });
                            s.forEach(e -> e.addParent(r.getSupplier()));
                            FHResearch.reindex();
                            EditUtils.saveResearch(r);
                        });
                    }
                };
                chd.setPos(offset, 130);
                add(chd);
                offset += chd.width + 3;
            }
            {
                Button create = new TechTextButton(this, TranslateUtils.str("new"), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {
                        // TODO Add research
                        new ResearchEditorDialog(this, null, researchPanel.selectedCategory).open();
                    }
                };
                create.setPos(offset, 130);
                add(create);
                offset += create.width + 3;
            }
            if (researchPanel.selectedResearch != null) {
                Button create = new TechTextButton(this, TranslateUtils.str("edit"), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {
                        EditUtils.editResearch(this, researchPanel.selectedResearch);
                    }
                };
                create.setPos(offset, 130);
                add(create);
                offset += create.width + 3;
                Button rem = new TechTextButton(this, TranslateUtils.str("delete"), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton mouseButton) {
                        researchPanel.selectedResearch.delete();
                        researchPanel.refreshWidgets();
                    }
                };
                rem.setPos(offset, 130);
                add(rem);
                offset += rem.width + 3;
            }
        }
        if (researchPanel.selectedResearch == null)
            return;
        ResearchDetailButton button = new ResearchDetailButton(this, researchPanel.selectedResearch);
        add(button);
        button.setPos(70, 48);
        int k = 0;
        int trmost = 0;
        boolean haveHScroll = false;
        Set<Research> parents = researchPanel.selectedResearch.getParents();
        for (Research parent : parents) {
            int x;
            if (k >= 4) {
                x = ButtonPos[4] + (k - 4) * 32;
            } else
                x = ButtonPos[k];
            ResearchSimpleButton parentButton = new ResearchSimpleButton(this, parent);
            add(parentButton);
            parentButton.setPos(x, 16);
            ThickLine l = new ResearchHierarchyLine(parent);
            lines.add(l);

            l.setPosAndDelta(x + 12, 30, 0, 12);
            k++;
        }
        if (k > 6)
            haveHScroll = true;
        if (k > 1) {
            int lmost = 0;
            int rmost = 0;

            ThickLine lu = new ResearchCombinatorLine(researchPanel.selectedResearch);
            lines.add(lu);
            if (k > 4) {
                rmost = ButtonPos[4] + (k - 5) * 32 + 12;
            } else if (k >= 3)
                rmost = ButtonPos[2] + 12;
            else
                rmost = ButtonPos[0] + 12;
            if (k >= 4)
                lmost = ButtonPos[3] + 12;
            else
                lmost = ButtonPos[1] + 12;
            trmost = rmost;
            lu.setPoints(lmost, 42, rmost, 42);

        }
        if (k > 0) {
            ThickLine lux = new ResearchCombinatorLine(researchPanel.selectedResearch);
            lines.add(lux);
            lux.setPosAndDelta(ButtonPos[0] + 12, 42, 0, 24);
        }
        k = 0;

        // if (FHResearch.editor || researchPanel.selectedResearch.isUnlocked()) {
        boolean crunlocked = researchPanel.selectedResearch.isUnlocked();
        Set<Research> children = researchPanel.selectedResearch.getChildren();
        for (Research child : children) {
            if (!crunlocked && !child.isShowable())
                continue;
            int x;
            if (k >= 4) {
                x = ButtonPos[4] + (k - 4) * 32;
            } else
                x = ButtonPos[k];
            ResearchSimpleButton childButton = new ResearchSimpleButton(this, child);
            childButton.setChildren(researchPanel.selectedResearch);
            add(childButton);
            childButton.setPos(x, 92);
            ThickLine l = new ResearchHierarchyLine(researchPanel.selectedResearch);
            lines.add(l);
            l.setPosAndDelta(x + 12, 90, 0, 16);
            k++;
        }
        if (k > 6)
            haveHScroll = true;
        if (k > 1) {
            int lmost = 0;
            int rmost = 0;
            ThickLine lu = new ResearchHierarchyLine(researchPanel.selectedResearch);
            lines.add(lu);
            if (k > 4) {
                rmost = ButtonPos[4] + (k - 5) * 32 + 12;
            } else if (k >= 3)
                rmost = ButtonPos[2] + 12;
            else
                rmost = ButtonPos[0] + 12;
            if (k >= 4)
                lmost = ButtonPos[3] + 12;
            else
                lmost = ButtonPos[1] + 12;
            trmost = Math.max(rmost, trmost);
            lu.setPoints(lmost, 90, rmost, 90);
        }
        if (k > 0) {
            ThickLine lux2 = new ResearchHierarchyLine(researchPanel.selectedResearch);
            lines.add(lux2);
            lux2.setPosAndDelta(ButtonPos[0] + 12, 66, 0, 24);
        }
        // }
        if (haveHScroll) {
            researchPanel.hierarchyBar.unhide();
            researchPanel.hierarchyBar.setMaxValue(trmost + 24);
        } else
            researchPanel.hierarchyBar.hide();

    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void clearWidgets() {
        super.clearWidgets();
        lines.clear();
    }

    @Override
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        theme.drawString(matrixStack, TranslateUtils.translateGui("research_hierarchy"), x + 3, y + 3, TechIcons.text, 0);
        TechIcons.HLINE_L.draw(matrixStack, x + 1, y + 13, 80, 3);
    }

    @Override
    public void drawOffsetBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
        // theme.drawPanelBackground(matrixStack, x, y, w, h);
        GuiHelper.setupDrawing();
        for (ThickLine l : lines)
            l.draw(matrixStack, x, y);
    }

    @Override
    public boolean isEnabled() {
        return researchPanel.canEnable(this);
    }
}
