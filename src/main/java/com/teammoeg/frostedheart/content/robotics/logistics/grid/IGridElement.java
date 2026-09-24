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

package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IGridElement {

	default ItemStack pushItem(ItemStack is) {
		return pushItem(new ItemKey(is),is);
	}

	ItemStack pushItem(ItemKey ik, ItemStack is);

	ItemStack takeItem(ItemKey key, int amount);

	ItemStack takeItem(int slot, int amount);

	default ItemStack takeItem(ItemStack is) {
		return takeItem(new ItemKey(is),is.getCount());
	}
	void registerSlots(LogisticHub hub);
	
	boolean isChanged();
	default void revalidate() {
	}
	boolean consumeChange();
	BlockPos getPos();
	Level getLevel();

	boolean fillable();

	void removeSlots();

	void setLevel(Level level);

	void tick();
}
