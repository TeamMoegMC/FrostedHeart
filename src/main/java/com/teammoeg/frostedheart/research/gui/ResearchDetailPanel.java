package com.teammoeg.frostedheart.research.gui;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.clues.AbstractClue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ResearchDetailPanel extends Panel {
	Research research;
	Icon ci;
	CluesPanel cluesPanel;
	EffectsPanel effectsPanel;
	ReqPanel reqPanel;
	TextField descPanel;
	Button closeButton;
	Button commitItems;

	public PanelScrollBar scrollEffects;
	public PanelScrollBar scrollClues;

	public static final int PADDING = 5;

	public ResearchDetailPanel(Panel panel) {
		super(panel);
		//setPosAndSize(-1, -1, 0, 0);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
		descPanel = new TextField(this);
		cluesPanel = new CluesPanel(this);
		effectsPanel = new EffectsPanel(this);
		reqPanel = new ReqPanel(this);
		closeButton = new Button(this, new StringTextComponent("X"), Icon.EMPTY) {
			@Override
			public void onClicked(MouseButton mouseButton) {
				research=null;
				
			}
		};
		commitItems = new Button(this, new StringTextComponent("Commit Items"), Icon.EMPTY) {
			@Override
			public void onClicked(MouseButton mouseButton) {
				// Lookup player inventory and check whether it has the required items
				// if does
					// consume them
				// else
					// through error message
			}
		};
	}

	@Override
	public void addWidgets() {
		if(research ==null)return;
		ci=ItemIcon.getItemIcon(research.getIcon());

		add(descPanel);
		descPanel.setMaxWidth(width/2-PADDING*2);
		descPanel.setPosAndSize(PADDING, height/2+PADDING, width/2-PADDING*2, height/2-PADDING*2);
		descPanel.setText(research.getDesc());

		add(cluesPanel);
		cluesPanel.setPosAndSize(width/2+PADDING, PADDING, width/2-PADDING*2, height/3-PADDING*2);

		add(effectsPanel);
		effectsPanel.setPosAndSize(width/2+PADDING, height/3+PADDING, width/2-PADDING*2, height/3-PADDING*2);

		add(reqPanel);
		reqPanel.setPosAndSize(width/2+PADDING, height/3*2+PADDING, width/2-PADDING*2, height/3-PADDING*2);

		scrollEffects = new PanelScrollBar(this, effectsPanel);
		scrollEffects.setPosAndSize(width-PADDING-PADDING*2, height/3+PADDING, PADDING*2, height/3-PADDING*2);
		add(scrollEffects);

		scrollClues = new PanelScrollBar(this, cluesPanel);
		scrollClues.setPosAndSize(width-PADDING-PADDING*2, PADDING, PADDING*2, height/3-PADDING*2);
		add(scrollClues);

		closeButton.setPosAndSize(width-PADDING*2, 0, PADDING*2, PADDING*2);
		commitItems.setPosAndSize(PADDING+32+PADDING, PADDING+10+PADDING, 20, 10);
		add(closeButton);
		add(commitItems);
	}
	@Override
	public boolean mousePressed(MouseButton button) {
		return super.mousePressed(button) || isMouseOver();
	}
	@Override
	public void alignWidgets() {
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if(research ==null)return;
		super.draw(matrixStack,theme, x, y, w, h);

		// research info
		theme.drawString(matrixStack, research.getName(), x+PADDING, y+PADDING);
		ci.draw(matrixStack, x+PADDING, y+PADDING+10+PADDING, 32,32);
	}
	public void open(Research r) {
		this.research=r;
		this.refreshWidgets();
		//this.openGui();
	}
	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}

	public static class CluesPanel extends Panel {

		ResearchDetailPanel detailPanel;

		public CluesPanel(ResearchDetailPanel panel) {
			super(panel);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			int offset = 0;
			for (AbstractClue clue : detailPanel.research.getClues()) {
				TextField textField = new TextField(this);
				textField.setMaxWidth(width);
				textField.setPosAndSize(0, offset*10, width, 10);

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
			theme.drawString(matrixStack, "Clues", x, y);
//			int cnt = 0;
//			for (AbstractClue clue : detailPanel.research.getClues()) {
//				theme.drawString(matrixStack, clue.getName(), x + PADDING, y + PADDING + cnt * 10);
//				cnt++;
//			}
		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

	public static class ReqPanel extends Panel {

		ResearchDetailPanel detailPanel;

		public ReqPanel(ResearchDetailPanel panel) {
			super(panel);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			int offset = 0;

			for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
				if (ingredient.getMatchingStacks().length != 0) {
					Icon icon = ItemIcon.getItemIcon(ingredient.getMatchingStacks()[0]);
					Button button = new Button(this) {
						@Override
						public void onClicked(MouseButton mouseButton) {

						}
					};
					button.setPosAndSize(offset*16, PADDING*2, 16, 16);
					button.setIcon(icon);
					button.setTitle(ingredient.getMatchingStacks()[0].getTextComponent());
					
					add(button);
					offset++;
				}
			}

			this.setHeight(offset*16);
		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			// research requirements
			super.draw(matrixStack,theme,x,y,w,h);
			theme.drawString(matrixStack, "Required Items", x, y);
//			int cnt = 0;
//			for (IngredientWithSize ingredient : detailPanel.research.getRequiredItems()) {
//				if (ingredient.getMatchingStacks().length != 0) {
//					Icon icon = ItemIcon.getItemIcon(ingredient.getMatchingStacks()[0]);
//					icon.draw(matrixStack, x, y+PADDING*2 + cnt * 16, 16, 16);
//					theme.drawString(matrixStack, ingredient.getMatchingStacks()[0].getTextComponent(), x + 16 + PADDING, y + PADDING*2 + cnt * 16);
//					cnt++;
//				}
//			}
		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

	public static class EffectsPanel extends Panel {

		ResearchDetailPanel detailPanel;

		public EffectsPanel(ResearchDetailPanel panel) {
			super(panel);
			detailPanel = panel;
		}

		@Override
		public void addWidgets() {
			int offset = 0;
			for (Effect effect : detailPanel.research.getEffects()) {
				TextField textField = new TextField(this);
				textField.setMaxWidth(width);
				textField.setPosAndSize(0, offset*10, width, 10);

				textField.setText(effect.getName());
				TooltipList tooltipList = new TooltipList();
				for (ITextComponent text : effect.getTooltip()) {
					tooltipList.add(text);
				}
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
			// research effects
			super.draw(matrixStack,theme,x,y,w,h);
			theme.drawString(matrixStack, "Effects", x, y);
//			int cnt = 0;
//			for (Effect effect : detailPanel.research.getEffects()) {
//				theme.drawString(matrixStack, effect.getName(), x + PADDING, y + 60 + cnt * 10);
//				cnt++;
//			}
		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

}
