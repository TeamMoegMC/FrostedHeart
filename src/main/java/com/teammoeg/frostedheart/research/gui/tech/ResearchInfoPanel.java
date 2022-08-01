package com.teammoeg.frostedheart.research.gui.tech;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.research.FHEffectTriggerPacket;
import com.teammoeg.frostedheart.network.research.FHResearchControlPacket;
import com.teammoeg.frostedheart.network.research.FHResearchControlPacket.Operator;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.EffectBuilding;
import com.teammoeg.frostedheart.research.gui.FramedPanel;
import com.teammoeg.frostedheart.research.gui.RTextField;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechTextButton;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

import java.util.ArrayList;
import java.util.List;

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
            // commit items button
            Button commitItems = new TechTextButton(this, GuiUtils.translateGui("research.commit_material_and_start"),
                    Icon.EMPTY) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    PacketHandler.sendToServer(new FHResearchControlPacket(Operator.COMMIT_ITEM, detailPanel.research));
                }
            };
            panels.add(commitItems);

            add(commitItems);
        } else if (researchData.isInProgress()) {
            // commit items button
            Button commitItems = new TechTextButton(this, GuiUtils.translateGui("research.stop"), Icon.EMPTY) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    PacketHandler.sendToServer(new FHResearchControlPacket(Operator.PAUSE, detailPanel.research));
                }
            };
            panels.add(commitItems);
            add(commitItems);
        } else if (!researchData.isCompleted()) {
            // commit items button
            Button commitItems = new TechTextButton(this, GuiUtils.translateGui("research.start"), Icon.EMPTY) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                    PacketHandler.sendToServer(new FHResearchControlPacket(Operator.START, detailPanel.research));
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
                        LEffectWidget button = new LEffectWidget(fp, effect);
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
                                PacketHandler.sendToServer(new FHEffectTriggerPacket(detailPanel.research));
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

                for (Clue clue : detailPanel.research.getClues()) {
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
