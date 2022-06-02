package com.teammoeg.frostedheart.research.gui;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.network.FHEffectTriggerPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.clues.AbstractClue;
import com.teammoeg.frostedheart.research.effects.*;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

public class ResearchInfoPanel extends Panel {

	ResearchDetailPanel detailPanel;
	List<Panel> panels=new ArrayList<>();
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
				int ioffset = 0;
				int xoffset = 0;
				for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
					if (ingredient.getMatchingStacks().length != 0) {
						ItemStack toDisplay = ingredient.getMatchingStacks()[0];
						Icon icon = ItemIcon.getItemIcon(toDisplay);
						Button button = new Button(fp) {
							@Override
							public void onClicked(MouseButton mouseButton) {

							}
						};

						button.setPosAndSize(xoffset, ioffset, 16, 16);
						button.setIcon(icon);
						button.setTitle(new TranslationTextComponent(toDisplay.getTranslationKey())
								.appendString(" x " + toDisplay.getCount()));
						fp.add(button);

						xoffset += button.width + 1;
					}
				}
				ioffset += 17;

				// commit items button
				Button commitItems = new TechTextButton(fp,
						GuiUtils.translateGui("research.commit_material_and_start"), Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {

						// check materials
						boolean hasAllMaterials = true;
						for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
							if (!hasAllMaterials)
								break;
							// each ingredient
							ItemStack[] matchingStacks = ingredient.getMatchingStacks();
							boolean alreadyFound = false;
							for (ItemStack requiredStack : matchingStacks) {
								if (alreadyFound)
									break;
								for (ItemStack invStack : detailPanel.researchScreen.player.inventory.mainInventory) {
									if (!invStack.isEmpty() && invStack.isItemEqual(requiredStack)
											&& invStack.getCount() >= requiredStack.getCount()) {
										alreadyFound = true;
										break;
									}
								}
							}
							if (!alreadyFound)
								hasAllMaterials = false;
						}

						// commit materials
						if (hasAllMaterials) {
							ResearchData researchData = ResearchDataAPI
									.getData((ServerPlayerEntity) detailPanel.researchScreen.player)
									.getData(detailPanel.research);

							for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
								// each ingredient
								ItemStack[] matchingStacks = ingredient.getMatchingStacks();
								boolean alreadyFound = false;
								for (ItemStack requiredStack : matchingStacks) {
									if (alreadyFound)
										break;
									for (ItemStack invStack : detailPanel.researchScreen.player.inventory.mainInventory) {
										if (!invStack.isEmpty() && invStack.isItemEqual(requiredStack)
												&& invStack.getCount() >= requiredStack.getCount()) {
											invStack.shrink(requiredStack.getCount());
											// notify data
											researchData.commitItem(requiredStack);
											alreadyFound = true;
											break;
										}
									}
								}
							}

							refreshWidgets();
						}
					}
				};

				commitItems.setPos(0, ioffset);
				ioffset += commitItems.height;
				fp.add(commitItems);
				fp.setWidth(width);

				fp.setHeight(ioffset);
			});
			prl.setTitle(GuiUtils.translateGui("research.requirements"));
			prl.setPos(0, 0);
			if (!researchData.canResearch())
				panels.add(prl);
			
			add(prl);

		FramedPanel ppl = new FramedPanel(this, fp -> {
			int offset=0;
			int xoffset=0;
			boolean fX=true;
			boolean fY=true;
			boolean hasItemRewards=false;
			boolean hasB=false;
			for(Effect effect:detailPanel.research.getEffects()) {
				if(!(effect instanceof EffectBuilding))continue;
				LEffectWidget button = new LEffectWidget(fp,effect);
				button.setPos(xoffset, offset);
				fp.add(button);
				if(fX) {
					xoffset+=2;
					fX=false;
				}
				xoffset+=32;
				if(xoffset>=98) {
					if(fY) {offset+=2;fY=false;};
					offset += 32;
				}
				hasB=true;
			}
			if(hasB)
				offset+=4;
			hasB=false;
			fX=true;
			fY=true;
			xoffset=2;
			for (Effect effect : detailPanel.research.getEffects()) {

				// item reward
				if (effect instanceof EffectItemReward) {
					hasItemRewards=true;
					
				}

				// building
				if (effect instanceof EffectBuilding) {
					continue;
				}
				EffectWidget button = new EffectWidget(fp,effect);
				button.setPos(xoffset, offset);
				add(button);
				if(fX) {xoffset+=4;fX=false;}
				xoffset += 16;
				if(xoffset>=121) {
					xoffset=2;
					if(fY) {
					offset+=4;
					fY=false;
					}
					offset+=16;
				}
				hasB=true;
			}
			if(hasB)
				offset+=8;
			TeamResearchData data = TeamResearchData.INSTANCE;
			// TODO: remove || true after api works
			if (hasItemRewards&&(data.getData(detailPanel.research).isCompleted() || true)) {
				Button claimRewards = new TechTextButton(fp, GuiUtils.translateGui("research.claim_rewards"),
						Icon.EMPTY) {
					@Override
					public void onClicked(MouseButton mouseButton) {
						PacketHandler.sendToServer(new FHEffectTriggerPacket(detailPanel.research));
						refreshWidgets();
					}
				};
				claimRewards.setPos(0, offset);
				fp.add(claimRewards);
				offset += claimRewards.height + 1;
			}
			fp.setWidth(width);
			fp.setHeight(offset);
		});
		ppl.setTitle(GuiUtils.translateGui("research.effects"));
		ppl.setPos(0, 0);
		add(ppl);
		panels.add(ppl);


		FramedPanel pcl = new FramedPanel(this, fp -> {
			int offset=0;
	
			for (AbstractClue clue : detailPanel.research.getClues()) {
				TextField clueName = new TextField(fp);
				clueName.setMaxWidth(width).setText(clue.getName()).setPos(0, offset);
				fp.add(clueName);
				offset += clueName.height + 1;
	
				TextField clueDesc = new TextField(fp);
				clueDesc.setMaxWidth(width).setText(clue.getDescription()).setPos(0, offset);
				fp.add(clueDesc);
				offset += clueDesc.height + 1;
	
				TextField clueHint = new TextField(fp);
				clueHint.setMaxWidth(width).setText(GuiUtils.translateGui("research.hint").appendSibling(clue.getHint()))
						.setPos(0, offset);
				fp.add(clueHint);
				offset += clueHint.height + 1;
			}
			fp.setWidth(width);
			fp.setHeight(offset);
		});
		
		pcl.setTitle(GuiUtils.translateGui("research.clues"));
		pcl.setPos(0, 0);
		add(pcl);
		panels.add(pcl);
		if (researchData.canResearch()) 
			panels.add(prl);
	}

	@Override
	public void alignWidgets() {
		int offset=0;
		for(Panel p:panels) {
			p.setPos(0,offset);
			offset+=p.height;
		}
		detailPanel.scrollInfo.setMaxValue(offset);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
	}
}
