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

/**
 * 可停用槽位接口。允许在运行时动态启用或禁用槽位。
 * <p>
 * Deactivatable slot interface. Allows dynamically enabling or disabling a slot at runtime.
 */
public interface DeactivatableSlot {
	/**
	 * 设置槽位的激活状态。
	 * <p>
	 * Sets the activation state of the slot.
	 *
	 * @param enabled 是否启用 / whether to enable
	 */
	void setActived(boolean enabled);
}