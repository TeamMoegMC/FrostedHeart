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

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.EffectBuilding;
import com.teammoeg.frostedheart.research.gui.FramedPanel;
import com.teammoeg.frostedheart.research.gui.RTextField;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechTextButton;
import com.teammoeg.frostedheart.research.network.FHEffectTriggerPacket;
import com.teammoeg.frostedheart.research.network.FHResearchControlPacket;
import com.teammoeg.frostedheart.research.network.FHResearchControlPacket.Operator;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class ResearchInfoPanel extends Panel {

    ResearchDetailPanel detailPanel;
    List<Widget> panels = new ArrayList<>();

    public ResearchInfoPanel(ResearchDetailPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        detailPanel = panel;
    }

    @Override
    public void addWidgets() {
        panels.clear();

        ResearchData researchData = detailPanel.research.getData();

        // exp materials

        FramedPanel prl = new FramedPanel(this, fp -> {
            int ioffset = 4;
            int xoffset = 4;
            for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
                if (ingredient.getMatchingStacks().length != 0) {
                    RequirementSlot button = new RequirementSlot(fp, ingredient);

                    button.setPosAndSize(xoffset, ioffset, 16, 16);
                    fp.add(button);

                    xoffset += 17;
                    if (xoffset >= 121) {
                        xoffset = 4;
                        ioffset += 17;
                    }
                }
            }
            ioffset += 23;
            fp.setWidth(width);

            fp.setHeight(ioffset);
        });
        prl.setTitle(GuiUtils.translateGui("research.requirements"));
        prl.setPos(0, 0);
        if ((!detailPanel.research.getRequiredItems().isEmpty())
                && (FHResearch.editor || !researchData.canResearch())) {
            panels.add(prl);
            add(prl);
        }
        if (!researchData.canResearch()) {
        	if(researchData.isUnlocked()) {
	            // commit items button
	            Button commitItems = new TechTextButton(this, GuiUtils.translateGui("research.commit_material_and_start"),
	                    Icon.EMPTY) {
	                @Override
	                public void onClicked(MouseButton mouseButton) {
	                    FHPacketHandler.sendToServer(new FHResearchControlPacket(Operator.COMMIT_ITEM, detailPanel.research));
	                }
	            };
	            panels.add(commitItems);
	
	            add(commitItems);
        	}
        } else if (researchData.isInProgress()) {
            // commit items button
            Button commitItems = new TechTextButton(this, GuiUtils.translateGui("research.stop"), Icon.EMPTY) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    FHPacketHandler.sendToServer(new FHResearchControlPacket(Operator.PAUSE, detailPanel.research));
                }
            };
            panels.add(commitItems);
            add(commitItems);
        } else if (!researchData.isCompleted()&&!detailPanel.research.isInCompletable()) {
            // commit items button
            Button commitItems = new TechTextButton(this, GuiUtils.translateGui("research.start"), Icon.EMPTY) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    FHPacketHandler.sendToServer(new FHResearchControlPacket(Operator.START, detailPanel.research));
                }
            };

            panels.add(commitItems);
            add(commitItems);
        }

        if (!detailPanel.research.getEffects().isEmpty()) {
            FramedPanel ppl = new FramedPanel(this, fp -> {
                int offset = 2;
                int xoffset = 2;
                if ((!detailPanel.research.isHideEffects()) || detailPanel.research.isCompleted() || FHResearch.editor) {
                    boolean hasB = false;
                    for (Effect effect : detailPanel.research.getEffects()) {
                        if (!(effect instanceof EffectBuilding))
                            continue;
                        if (effect.isHidden()) continue;
                        LargeEffectWidget button = new LargeEffectWidget(fp, effect);
                        button.setPos(xoffset, offset);
                        fp.add(button);
                        xoffset += 32;
                        if (xoffset >= 98) {
                            offset += 32;
                            xoffset = 2;
                        }
                        hasB = true;
                    }
                    if (hasB)
                        offset += 42;
                    hasB = false;
                    xoffset = 4;
                    boolean hasUnclaimed = false;
                    for (Effect effect : detailPanel.research.getEffects()) {
                        if (!effect.isGranted())
                            hasUnclaimed = true;
                        // building
                        if (effect instanceof EffectBuilding) {
                            continue;
                        }
                        if (effect.isHidden()) continue;
                        EffectWidget button = new EffectWidget(fp, effect);
                        button.setPos(xoffset, offset);
                        fp.add(button);
                        xoffset += 17;
                        if (xoffset >= 121) {
                            xoffset = 4;
                            offset += 17;
                        }
                        hasB = true;
                    }

                    if (hasB)
                        offset += 24;
                    TeamResearchData data = TeamResearchData.getClientInstance();
                    if (data.getData(detailPanel.research).isCompleted() && hasUnclaimed) {
                        Button claimRewards = new TechTextButton(fp, GuiUtils.translateGui("research.claim_rewards"),
                                Icon.EMPTY) {
                            @Override
                            public void onClicked(MouseButton mouseButton) {
                                FHPacketHandler.sendToServer(new FHEffectTriggerPacket(detailPanel.research));
                            }
                        };
                        claimRewards.setPos(0, offset);
                        fp.add(claimRewards);
                        offset += claimRewards.height + 1;
                    }
                } else {
                    RTextField rt = new RTextField(fp).setColor(TechIcons.text).setMaxWidth(width - 5).setText(GuiUtils.translateGui("effect_unknown"));
                    rt.setPos(xoffset, offset);
                    offset += rt.height;
                    fp.add(rt);
                }
                fp.setWidth(width);
                fp.setHeight(offset);
            });
            ppl.setTitle(GuiUtils.translateGui("research.effects"));
            ppl.setPos(0, 0);
            add(ppl);
            panels.add(ppl);
        }
        if (!detailPanel.research.getClues().isEmpty()) {
            FramedPanel pcl = new FramedPanel(this, fp -> {
                int offset = 1;
                List<Clue> clues=new ArrayList<>();
                int i=0;
                for(Clue cl:detailPanel.research.getClues()) {
                	if(cl.isRequired()&&!cl.isCompleted()) 
                		clues.add(i++, cl);
                	else clues.add(cl);
                }
                for (Clue clue : clues) {
                    CluePanel cl = new CluePanel(fp, clue, detailPanel.research);
                    cl.setY(offset);
                    cl.setWidth(width - 5);
                    cl.initWidgets();
                    fp.add(cl);
                    offset += cl.height + 1;

                }
                fp.setWidth(width);
                fp.setHeight(offset);
            });

            pcl.setTitle(GuiUtils.translateGui("research.clues"));
            pcl.setPos(0, 0);
            add(pcl);
            panels.add(pcl);
        }
        if (!detailPanel.research.getRequiredItems().isEmpty() && (!FHResearch.editor) && researchData.canResearch()) {
            panels.add(prl);
            add(prl);
        }
    }

    @Override
    public void alignWidgets() {
        int offset = 0;
        for (Widget p : panels) {
            p.setPos(3, offset);
            offset += p.height + 2;
        }
        detailPanel.scrollInfo.setMaxValue(offset);
    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
    }
}
