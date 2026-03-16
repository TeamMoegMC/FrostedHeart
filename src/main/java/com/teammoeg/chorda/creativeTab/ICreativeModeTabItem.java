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

package com.teammoeg.chorda.creativeTab;
/**
 * 创造模式标签页自动事件监听器接口。
 * 用于物品的标签页注册。在方块类中实现此接口时，
 * 必须配合实现了ICreativeModeTabItem的BlockItem子类使用，否则不会生效。
 * <p>
 * Interface for automatic creative tab event listeners.
 * Useful for item tab registration. Implementing in a block without using
 * a proper BlockItem subclass that implements ICreativeModeTabItem would take no effect.
 */
public interface ICreativeModeTabItem {
	/**
	 * 将物品填充到创造模式物品栏中。
	 * <p>
	 * Fills the item into the creative mode tab.
	 *
	 * @param helper 创造模式物品栏辅助器 / The creative tab item helper
	 */
	void fillItemCategory(CreativeTabItemHelper helper);
}
