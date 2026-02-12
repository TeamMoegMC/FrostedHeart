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

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;

public class RequirementSlot extends UIElement {
	ItemStack[] i;
	int cnt;

	public RequirementSlot(UIElement panel, Pair<Ingredient, Integer> iws) {
		super(panel);
		this.i = iws.getFirst().getItems();
		this.cnt = iws.getSecond();
		this.setSize(16, 16);
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		ItemStack cur = i[(int) ((System.currentTimeMillis() / 1000) % i.length)];
		// list.add(cur.getDisplayName());
		cur.getTooltipLines(ClientUtils.getPlayer(), TooltipFlag.Default.NORMAL).forEach(list::accept);
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		ItemStack cur = i[(int) ((System.currentTimeMillis() / 1000) % i.length)];
		CGuiHelper.resetGuiDrawing();
		getTheme().drawSlot(matrixStack, x, y, w, h);
		CGuiHelper.drawItem(matrixStack, cur, x, y, 0, w / 16F, h / 16F, true, cnt != 0 ? String.valueOf(cnt) : null);
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				// TODO edit ingredient
				JEICompat.showJEIFor(i[(int) ((System.currentTimeMillis() / 1000) % i.length)]);
			}

			return true;
		}

		return false;
	}

}
