package com.teammoeg.frostedheart.research.gui;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.clues.AbstractClue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.Research;

import com.teammoeg.frostedheart.research.effects.EffectBuilding;
import com.teammoeg.frostedheart.research.effects.EffectCrafting;
import com.teammoeg.frostedheart.research.effects.EffectItemReward;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

public class ResearchDetailPanel extends Panel {
	Research research;
	Icon ci;
	CluesPanel cluesPanel;
	EffectsPanel effectsPanel;
	ReqPanel reqPanel;
	DescPanel descPanel;
	Button closeButton;
	Button commitItems;

	public PanelScrollBar scrollEffects;
	public PanelScrollBar scrollClues;

	public static final int PADDING = 10;

	public ResearchDetailPanel(Panel panel) {
		super(panel);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
		cluesPanel = new CluesPanel(this);
		effectsPanel = new EffectsPanel(this);
		reqPanel = new ReqPanel(this);
		descPanel = new DescPanel(this);
	}

	@Override
	public void addWidgets() {
		if(research ==null)return;
		ci=ItemIcon.getItemIcon(research.getIcon());

		add(descPanel);
		descPanel.setPosAndSize(PADDING, PADDING+(height-PADDING*3)/3+PADDING, (width-PADDING*2)/2, (height-PADDING*3)/3*2);

		int rightPanelHeight = (height - 4*PADDING) / 3;
		int rightPanelWidth = (width - PADDING) / 2;

		add(cluesPanel);
		cluesPanel.setPosAndSize(rightPanelWidth+PADDING, PADDING, rightPanelWidth, rightPanelHeight);

		add(reqPanel);
		reqPanel.setPosAndSize(rightPanelWidth+PADDING, PADDING+rightPanelHeight+PADDING, rightPanelWidth, rightPanelHeight);

		add(effectsPanel);
		effectsPanel.setPosAndSize(rightPanelWidth+PADDING, PADDING+rightPanelHeight+PADDING+rightPanelHeight+PADDING, rightPanelWidth, rightPanelHeight);

		scrollEffects = new PanelScrollBar(this, effectsPanel);
		scrollEffects.setPosAndSize(width-PADDING-PADDING*2, PADDING+rightPanelHeight+PADDING+rightPanelHeight+PADDING, PADDING*2, height/3-PADDING*2);
		add(scrollEffects);

		scrollClues = new PanelScrollBar(this, cluesPanel);
		scrollClues.setPosAndSize(width-PADDING-PADDING*2, PADDING, PADDING*2, height/3-PADDING*2);
		add(scrollClues);

		closeButton = new Button(this, new StringTextComponent("X"), Icon.EMPTY) {
			@Override
			public void onClicked(MouseButton mouseButton) {
				research=null;
			}
		};
		commitItems = new SimpleTextButton(this, new StringTextComponent("Commit Items"), Icon.EMPTY) {
			@Override
			public void onClicked(MouseButton mouseButton) {
				// Lookup player inventory and check whether it has the required items
				// if does
				// consume them
				// else
				// through error message
			}
		};

		closeButton.setPosAndSize(width-PADDING*2, 0, PADDING*2, PADDING*2);
		commitItems.setPos(PADDING+32+PADDING, PADDING+10+PADDING);
		add(closeButton);
		add(commitItems);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if(research ==null) {
			closeGui();
			return;
		}

		matrixStack.push();
		matrixStack.translate(0, 0, 500);
		super.draw(matrixStack,theme, x, y, w, h);

		// left side
		theme.drawString(matrixStack, research.getName(), x+PADDING, y+PADDING);
		ci.draw(matrixStack, x+PADDING, y+PADDING+10+PADDING, 32,32);

		// right side
		int rightPanelHeight = (height - 4*PADDING) / 3;
		int rightPanelWidth = (width - PADDING) / 2 + PADDING;
		theme.drawString(matrixStack, GuiUtils.translateGui("research.clues"), x+rightPanelWidth+PADDING, y);
		theme.drawString(matrixStack, GuiUtils.translateGui("research.requirements"), x+rightPanelWidth+PADDING, y+PADDING+rightPanelHeight);
		theme.drawString(matrixStack, GuiUtils.translateGui("research.effects"), x+rightPanelWidth+PADDING, y+PADDING*2+rightPanelHeight*2);

		matrixStack.pop();
	}
	public void open(Research r) {
		this.research=r;
		this.refreshWidgets();
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.zOffset = 950;
		list.zOffsetItemTooltip = 500;
		super.addMouseOverText(list);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawBackground(matrixStack, theme, x, y, w, h);
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}

	public static class DescPanel extends Panel {
		ResearchDetailPanel detailPanel;

		public DescPanel(ResearchDetailPanel panel) {
			super(panel);
			this.setOnlyInteractWithWidgetsInside(true);
			this.setOnlyRenderWidgetsInside(true);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			TextField desc = new TextField(this);
			add(desc);
			desc.setMaxWidth(width-PADDING);
			desc.setPosAndSize(0, 0, width, height);
			desc.setText(detailPanel.research.getDesc());
		}

		@Override
		public void alignWidgets() {

		}
	}

	public static class CluesPanel extends Panel {

		ResearchDetailPanel detailPanel;

		public CluesPanel(ResearchDetailPanel panel) {
			super(panel);
			this.setOnlyInteractWithWidgetsInside(true);
			this.setOnlyRenderWidgetsInside(true);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			int offset = 0;
			for (AbstractClue clue : detailPanel.research.getClues()) {
				TextField textField = new TextField(this);
				textField.setMaxWidth(width);
				textField.setPosAndSize(0, 10+offset*10, width, 10);

				textField.setText(clue.getName());
				TooltipList tooltipList = new TooltipList();
				tooltipList.add(clue.getDescription());
				textField.addMouseOverText(tooltipList);
				add(textField);
				offset++;
			}
		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.draw(matrixStack,theme,x,y,w,h);
		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.drawBackground(matrixStack, theme, x, y, w, h);
			// theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

	public static class ReqPanel extends Panel {

		ResearchDetailPanel detailPanel;

		public ReqPanel(ResearchDetailPanel panel) {
			super(panel);
			this.setOnlyInteractWithWidgetsInside(true);
			this.setOnlyRenderWidgetsInside(true);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			int offset = PADDING;

			for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
				if (ingredient.getMatchingStacks().length != 0) {
					Icon icon = ItemIcon.getItemIcon(ingredient.getMatchingStacks()[0]);
					Button button = new Button(this) {
						@Override
						public void onClicked(MouseButton mouseButton) {

						}
					};
					button.setPosAndSize(offset, 10, 16, 16);
					button.setIcon(icon);
					button.setTitle(ingredient.getMatchingStacks()[0].getTextComponent());
					
					add(button);
					offset+=17;
				}
			}

		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.draw(matrixStack,theme,x,y,w,h);
		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.drawBackground(matrixStack, theme, x, y, w, h);
			// theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

	public static class EffectsPanel extends Panel {

		ResearchDetailPanel detailPanel;

		public EffectsPanel(ResearchDetailPanel panel) {
			super(panel);
			this.setOnlyInteractWithWidgetsInside(true);
			this.setOnlyRenderWidgetsInside(true);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			int offset = PADDING;
			for (Effect effect : detailPanel.research.getEffects()) {

				// effect type
				TextField textField = new TextField(this);
				textField.setMaxWidth(width);
				textField.setPosAndSize(PADDING, offset, width, 10);
				textField.setText(effect.getName().mergeStyle(TextFormatting.BOLD));
				add(textField);

				offset += 13;

				// item reward
				if (effect instanceof EffectItemReward) {
					List<ItemStack> rewards = ((EffectItemReward) effect).getRewards();
					for (int i = 0; i < rewards.size(); i++) {
						Icon icon = ItemIcon.getItemIcon(rewards.get(i));
						Button button = new Button(this) {
							@Override
							public void onClicked(MouseButton mouseButton) {

							}
						};
						button.setPosAndSize(PADDING+i*17, offset, 16, 16);
						button.setIcon(icon);
						button.setTitle(new TranslationTextComponent(rewards.get(i).getTranslationKey()));
						add(button);
					}
					// additional offset caused by item icons
					offset += 17;
				}

				// building
				if (effect instanceof EffectBuilding) {
					Block block = ((EffectBuilding) effect).getBlock();
					Icon icon = ItemIcon.getItemIcon(block.asItem());
					Button button = new Button(this) {
						@Override
						public void onClicked(MouseButton mouseButton) {

						}
					};
					button.setPosAndSize(0, offset, 16, 16);
					button.setIcon(icon);
					button.setTitle(block.getTranslatedName());
					add(button);
					offset += 17;
				}

				// crafting

				// use

				// stats

			}
		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.draw(matrixStack,theme,x,y,w,h);
		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.drawBackground(matrixStack, theme, x, y, w, h);
			// theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

}
