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

public class PagedSlot extends Slot {
	Supplier<Integer> pager;
	int page;

	public PagedSlot(Container pContainer, int pSlot, int pX, int pY, Supplier<Integer> pager, int page) {
		super(pContainer, pSlot, pX, pY);
		this.pager = pager;
		this.page = page;
	}

	@Override
	public boolean isActive() {
		return pager.get()==page;
	}

}
