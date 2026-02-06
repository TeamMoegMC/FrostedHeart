/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.FramedPanel;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.TechTextButton;
import com.teammoeg.frostedresearch.network.FHEffectTriggerPacket;
import com.teammoeg.frostedresearch.network.FHResearchControlPacket;
import com.teammoeg.frostedresearch.network.FHResearchControlPacket.Operator;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectBuilding;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;

public class ResearchInfoPanel extends UILayer {

	ResearchDetailPanel detailPanel;
	List<UIElement> panels = new ArrayList<>();

	public ResearchInfoPanel(ResearchDetailPanel panel) {
		super(panel);
		detailPanel = panel;
	}

	@Override
	public void addUIElements() {
		panels.clear();

		ResearchData researchData = detailPanel.research.getData();

		// exp materials

		FramedPanel prl = new FramedPanel(this, fp -> {
			int ioffset = 4;
			int xoffset = 4;
			for (Pair<Ingredient, Integer> ingredient : detailPanel.research.getRequiredItems()) {
				if (!ingredient.getFirst().isEmpty()) {
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
		prl.setTitle(Lang.translateGui("research.requirements"));
		prl.setPos(0, 0);
		if ((!detailPanel.research.getRequiredItems().isEmpty())
			&& (FHResearch.editor || !researchData.canResearch())) {
			panels.add(prl);
			add(prl);
		}
		if (!researchData.canResearch()) {
			if (detailPanel.research.isUnlocked()) {
				// commit items button
				TechTextButton commitItems = new TechTextButton(this, Lang.translateGui("research.commit_material_and_start"),
					CIcons.nop()) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						FRNetwork.INSTANCE.sendToServer(new FHResearchControlPacket(Operator.COMMIT_ITEM, detailPanel.research));
					}
				};
				panels.add(commitItems);

				add(commitItems);
			}
		} else if (detailPanel.research.isInProgress()) {
			// commit items button
			TechTextButton commitItems = new TechTextButton(this, Lang.translateGui("research.stop"), CIcons.nop()) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					FRNetwork.INSTANCE.sendToServer(new FHResearchControlPacket(Operator.PAUSE, detailPanel.research));
				}
			};
			panels.add(commitItems);
			add(commitItems);
		} else if (!researchData.isCompleted() && !detailPanel.research.isInCompletable()) {
			// commit items button
			TechTextButton commitItems = new TechTextButton(this, Lang.translateGui("research.start"), CIcons.nop()) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					FRNetwork.INSTANCE.sendToServer(new FHResearchControlPacket(Operator.START, detailPanel.research));
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
						LargeEffectWidget button = new LargeEffectWidget(fp, effect, detailPanel.research);
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
						if (!detailPanel.research.getData().isEffectGranted(effect))
							hasUnclaimed = true;
						// building
						if (effect instanceof EffectBuilding) {
							continue;
						}
						if (effect.isHidden()) continue;
						EffectWidget button = new EffectWidget(fp, effect, detailPanel.research);
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
					if (detailPanel.research.isCompleted() && hasUnclaimed) {
						TechTextButton claimRewards = new TechTextButton(fp, Lang.translateGui("research.claim_rewards"), CIcons.nop()) {
							@Override
							public void onClicked(MouseButton mouseButton) {
								FRNetwork.INSTANCE.sendToServer(new FHEffectTriggerPacket(detailPanel.research));
							}
						};
						claimRewards.setPos(0, offset);
						fp.add(claimRewards);
						offset += claimRewards.getHeight() + 1;
					}
				} else {
					TextField rt = new TextField(fp).setColor(DrawDeskTheme.getTextColor()).setMaxWidth(width - 5).setText(Lang.translateGui("effect_unknown"));
					rt.setPos(xoffset, offset);
					offset += rt.getHeight();
					fp.add(rt);
				}
				fp.setWidth(width);
				fp.setHeight(offset);
			});
			ppl.setTitle(Lang.translateGui("research.effects"));
			ppl.setPos(0, 0);
			add(ppl);
			panels.add(ppl);
		}
		if (!detailPanel.research.getClues().isEmpty()) {
			FramedPanel pcl = new FramedPanel(this, fp -> {
				int offset = 1;
				List<Clue> clues = new ArrayList<>();
				int i = 0;
				for (Clue cl : detailPanel.research.getClues()) {
					if (cl.isRequired() && !detailPanel.research.getData().isClueTriggered(cl))
						clues.add(i++, cl);
					else clues.add(cl);
				}
				for (Clue clue : clues) {
					CluePanel cl = new CluePanel(fp, clue, detailPanel.research);
					cl.setY(offset);
					cl.setWidth(width - 5);
					cl.initWidgets();
					fp.add(cl);
					offset += cl.getHeight() + 1;

				}
				fp.setWidth(width);
				fp.setHeight(offset);
			});

			pcl.setTitle(Lang.translateGui("research.clues"));
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
		for (UIElement p : panels) {
			p.setPos(3, offset);
			offset += p.getHeight() + 2;
		}
		// detailPanel.scrollInfo.setMaxValue(offset);
	}

	@Override
	public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
	}
}
