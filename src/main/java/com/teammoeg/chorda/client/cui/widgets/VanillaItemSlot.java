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

package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 原版风格物品槽控件。继承自{@link ItemSlot}，使用主题的槽位样式绘制背景，
 * 模拟原版Minecraft的物品槽外观。
 * <p>
 * Vanilla-style item slot widget. Extends {@link ItemSlot} and uses the theme's
 * slot style to draw the background, simulating the vanilla Minecraft item slot appearance.
 */
public class VanillaItemSlot extends ItemSlot {

	/**
	 * 创建空的原版风格物品槽。
	 * <p>
	 * Creates an empty vanilla-style item slot.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 */
	public VanillaItemSlot(UIElement parent) {
		super(parent);
	}

	/** {@inheritDoc} */
	@Override
	public void renderBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		theme().drawSlot(graphics, x, y, w, h);
		//super.renderBackground(graphics, x, y, w, h);
	}

	/**
	 * 创建包含单个物品堆的原版风格物品槽。
	 * <p>
	 * Creates a vanilla-style item slot with a single item stack.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param item 物品堆 / Item stack
	 */
	public VanillaItemSlot(UIElement parent, ItemStack item) {
		super(parent, item);
	}

	/**
	 * 创建包含配方原料的原版风格物品槽。
	 * <p>
	 * Creates a vanilla-style item slot with an ingredient.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param item 配方原料 / Recipe ingredient
	 */
	public VanillaItemSlot(UIElement parent, Ingredient item) {
		super(parent, item);
	}

	/**
	 * 创建包含物品堆数组的原版风格物品槽。
	 * <p>
	 * Creates a vanilla-style item slot with an array of item stacks.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param item 物品堆数组 / Array of item stacks
	 */
	public VanillaItemSlot(UIElement parent, ItemStack[] item) {
		super(parent, item);
	}

}
