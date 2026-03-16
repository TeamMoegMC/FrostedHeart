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

package com.teammoeg.chorda.menu.slots;

import java.util.function.Supplier;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * 分页槽位，仅在当前页码与槽位所属页码匹配时才处于激活状态。
 * <p>
 * Paged slot that is only active when the current page number matches the page this slot belongs to.
 * Used in paginated GUI menus where only slots on the currently visible page should be interactive.
 * The active page is determined by a {@link Supplier} that provides the current page number.
 */
public class PagedSlot extends Slot {
	/** 当前页码提供器 / Supplier that provides the current page number */
	Supplier<Integer> pager;
	/** 此槽位所属的页码 / The page number this slot belongs to */
	int page;

	/**
	 * 构造一个分页槽位。
	 * <p>
	 * Constructs a paged slot.
	 *
	 * @param pContainer 容器 / The container
	 * @param pSlot 槽位索引 / The slot index
	 * @param pX 槽位X坐标 / The slot X position
	 * @param pY 槽位Y坐标 / The slot Y position
	 * @param pager 当前页码提供器 / Supplier that provides the current page number
	 * @param page 此槽位所属的页码 / The page number this slot belongs to
	 */
	public PagedSlot(Container pContainer, int pSlot, int pX, int pY, Supplier<Integer> pager, int page) {
		super(pContainer, pSlot, pX, pY);
		this.pager = pager;
		this.page = page;
	}

	/**
	 * 判断此槽位是否处于激活状态。仅当当前页码与此槽位所属页码相同时返回true。
	 * <p>
	 * Determines whether this slot is active. Returns true only when the current page matches
	 * the page this slot belongs to.
	 *
	 * @return 当前页码是否匹配此槽位的页码 / Whether the current page matches this slot's page
	 */
	@Override
	public boolean isActive() {
		return pager.get()==page;
	}

}
