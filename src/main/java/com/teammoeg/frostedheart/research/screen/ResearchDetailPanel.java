package com.teammoeg.frostedheart.research.screen;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;

public class ResearchDetailPanel extends Panel {
	Research r;
	Icon ci;
	CluesPanel cluesPanel;
	EffectsPanel effectsPanel;
	ReqPanel reqPanel;

	public ResearchDetailPanel(Panel panel) {
		super(panel);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
		cluesPanel = new CluesPanel(this);
		effectsPanel = new EffectsPanel(this);
	}

	@Override
	public void addWidgets() {
		if(r==null)return;
		ci=ItemIcon.getItemIcon(r.getIcon());
		add(cluesPanel);
		add(effectsPanel);
		add(reqPanel);
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if(r==null)return;
		super.draw(matrixStack,theme, x, y, w, h);

		// research info
		theme.drawString(matrixStack,r.getName(), x+3, y+3);
		ci.draw(matrixStack, x+13, y+13, 32,32);
		theme.drawString(matrixStack, r.getDesc(), x+50, y+3);

//		// research effects
//		theme.drawString(matrixStack, "Effects", x+3, y+50);
//		int cnt = 0;
//		for (Effect effect : r.getEffects()) {
//			theme.drawString(matrixStack, effect.getName(), x + 3, y + 60 + cnt * 10);
//			cnt++;
//		}
//
//		// research requirements
//		theme.drawString(matrixStack, "Required Items", x+50, y+50);
//		cnt = 0;
//		for (IngredientWithSize ingredient : r.getRequiredItems()) {
//			theme.drawString(matrixStack, ingredient.getMatchingStacks(), x + 3, y + 60 + cnt * 10);
//			cnt++;
//		}
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

		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {

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

		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {

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

		}

		@Override
		public void alignWidgets() {

		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {

		}

		@Override
		public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
		}
	}

}
