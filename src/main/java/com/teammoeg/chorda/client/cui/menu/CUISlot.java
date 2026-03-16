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

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * 可停用的CUI容器槽位。继承自原版Slot，支持动态启用/禁用。
 * <p>
 * A deactivatable CUI container slot. Extends vanilla Slot with dynamic enable/disable support.
 */
public class CUISlot extends Slot implements DeactivatableSlot {
	boolean enabled = true;

	/**
	 * 构造一个CUI槽位。
	 * <p>
	 * Constructs a CUI slot.
	 *
	 * @param inventoryIn 所属容器 / the owning container
	 * @param index 槽位索引 / the slot index
	 * @param xPosition X坐标 / the x position
	 * @param yPosition Y坐标 / the y position
	 */
	public CUISlot(Container inventoryIn, int index, int xPosition, int yPosition) {
		super(inventoryIn, index, xPosition, yPosition);
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