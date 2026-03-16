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

package com.teammoeg.chorda.capability.capabilities;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.RangedWrapper;

/**
 * 仅允许输入的范围限制物品处理器包装器。
 * 继承自{@link RangedWrapper}，限定了可访问的槽位范围，并禁止从任何槽位提取物品。
 * 适用于需要限制外部自动化仅能向特定槽位输入物品的场景（如机器的输入槽）。
 * <p>
 * Input-only ranged item handler wrapper.
 * Extends {@link RangedWrapper} to limit accessible slot range and prohibits extracting items
 * from any slot. Useful for restricting external automation to only insert items into
 * specific slots (e.g., machine input slots).
 */
public class RangedInputWrapper extends RangedWrapper {

	/**
	 * 构造一个限定范围的仅输入包装器。
	 * <p>
	 * Constructs a ranged input-only wrapper.
	 *
	 * @param compose 被包装的物品处理器 / The item handler to wrap
	 * @param minSlot 最小槽位索引（包含） / The minimum slot index (inclusive)
	 * @param maxSlotExclusive 最大槽位索引（不包含） / The maximum slot index (exclusive)
	 */
	public RangedInputWrapper(IItemHandlerModifiable compose, int minSlot, int maxSlotExclusive) {
		super(compose, minSlot, maxSlotExclusive);
	}

	/**
	 * 始终返回空物品栈，禁止从此包装器提取物品。
	 * <p>
	 * Always returns an empty ItemStack, preventing item extraction from this wrapper.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

}
