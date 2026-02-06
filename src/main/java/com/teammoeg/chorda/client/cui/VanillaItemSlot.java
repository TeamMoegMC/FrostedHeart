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

package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class VanillaItemSlot extends ItemSlot {
	
	public VanillaItemSlot(UIElement parent) {
		super(parent);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		VanillaTheme.drawSlot(graphics, x, y, w, h);
		//super.renderBackground(graphics, x, y, w, h);
	}

	public VanillaItemSlot(UIElement parent, ItemStack item) {
		super(parent, item);
	}

	public VanillaItemSlot(UIElement parent, Ingredient item) {
		super(parent, item);
	}

	public VanillaItemSlot(UIElement parent, ItemStack[] item) {
		super(parent, item);
	}

}
