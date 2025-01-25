/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.robotics.logistics.workers;

import java.util.Objects;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityLogisticsStorage implements ILogisticsStorage {
	public ILogisticsStorage storage;
	public BlockEntity te;
	public TileEntityLogisticsStorage(ILogisticsStorage storage,BlockEntity te) {
		this.storage = storage;
		this.te=te;
	}
	public <T extends BlockEntity&ILogisticsStorage> TileEntityLogisticsStorage(T storage) {
		this.storage = storage;
		te=storage;
	}
	@Override
	public ItemStackHandler getInventory() {
		return storage.getInventory();
	}

	@Override
	public boolean isValidFor(ItemStack stack) {
		return storage.isValidFor(stack);
	}

	public BlockPos getActualPos() {
		return te.getBlockPos();
	}

	public Level getActualWorld() {
		return te.getLevel();
	}

	public boolean isRemoved() {
		return te.isRemoved();
	}
	public ILogisticsStorage getStorage() {
		return storage;
	}
	public BlockEntity getTe() {
		return te;
	}
	@Override
	public int hashCode() {
		return Objects.hash(te);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TileEntityLogisticsStorage other = (TileEntityLogisticsStorage) obj;
		return this.te==other.te;
	}

}
