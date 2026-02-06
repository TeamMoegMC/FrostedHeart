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

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.research.ResearchCategory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;

public class ResearchCategoryPanel extends UILayer {
	public static final int CAT_PANEL_HEIGHT = 40;
	public ResearchLayer researchScreen;

	public ResearchCategoryPanel(ResearchLayer panel) {
		super(panel);
		researchScreen = panel;
	}

	@Override
	public void addUIElements() {
		int k = 0;
		for (ResearchCategory r : ResearchCategory.values()) {
			CategoryButton button = new CategoryButton(this, r);
			button.setPosAndSize(k * 40, 0, 30, 21);
			add(button);
			k++;
		}
	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		super.render(matrixStack, x, y, w, h);
		// drawBackground(matrixStack, theme, x, y, w, h);
	}

	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}

	public static class CategoryButton extends Button {

		ResearchCategory category;
		ResearchCategoryPanel categoryPanel;

		public CategoryButton(ResearchCategoryPanel panel, ResearchCategory category) {
			super(panel, category.getName(), CIcons.getIcon(category.getIcon()));
			this.category = category;
			this.categoryPanel = panel;
		}

		@Override
		public void getTooltip(TooltipBuilder list) {
			list.accept(category.getName());
			list.accept(category.getDesc().copy().withStyle(ChatFormatting.GRAY));
		}

		@Override
		public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {

			// theme.drawHorizontalTab(matrixStack, x, y, w,
			// h,categoryPanel.researchScreen.selectedCategory==category);

			if (categoryPanel.researchScreen.selectedCategory == category) {
				TechIcons.TAB_HL.draw(matrixStack, x, y, w, 7);
				this.drawIcon(matrixStack, x + 7, y + 2, 16, 16);
			} else
				this.drawIcon(matrixStack, x + 7, y + 5, 16, 16);
			// super.drawBackground(matrixStack, theme, x, y, w, h);

			// theme.drawString(matrixStack, category.getName(), x + (w -
			// theme.getStringWidth(category.getName())) / 2, y + 24);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			categoryPanel.researchScreen.selectCategory(category);

		}
	}

}
