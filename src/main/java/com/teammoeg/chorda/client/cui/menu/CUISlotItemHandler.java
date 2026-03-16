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

package com.teammoeg.chorda.client.cui.menu;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 可停用的CUI物品处理器槽位。继承自Forge的SlotItemHandler，支持动态启用/禁用。
 * <p>
 * A deactivatable CUI item handler slot. Extends Forge's SlotItemHandler with dynamic enable/disable support.
 */
public class CUISlotItemHandler extends SlotItemHandler implements DeactivatableSlot {
	boolean enabled = true;

	/**
	 * 构造一个CUI物品处理器槽位。
	 * <p>
	 * Constructs a CUI item handler slot.
	 *
	 * @param inv 物品处理器 / the item handler
	 * @param id 槽位索引 / the slot index
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 */
	public CUISlotItemHandler(IItemHandler inv, int id, int x, int y) {
		super(inv, id, x, y);
	}

	/**
	 * 检查槽位是否激活。
	 * <p>
	 * Checks whether the slot is active.
	 *
	 * @return 如果槽位启用则返回true / true if the slot is enabled
	 */
	public boolean isActive() {
		return enabled;
	}

	/**
	 * 设置槽位的激活状态。
	 * <p>
	 * Sets the activation state of the slot.
	 *
	 * @param enabled 是否启用 / whether to enable
	 */
	public void setActived(boolean enabled) {
		this.enabled = enabled;
	}
}