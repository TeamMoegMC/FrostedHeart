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

package com.teammoeg.frostedheart.content.robotics.logistics.data;

import java.util.Map;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.IGridElement;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.ItemCountProvider;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticChest;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.ILogisticProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LogisticSlot implements IGridElement{
	Level level;
	BlockPos pos;
	int slot;
	boolean changed;

	public LogisticSlot(BlockPos pos, int slot) {
		super();
		this.pos = pos;
		this.slot = slot;
	}
	@Override
	public int getEmptySlotCount() {
		return 0;
	}
	@Override
	public ItemStack pushItem(ItemKey ik, ItemStack is, boolean fillEmpty) {
		if(CUtils.getExistingTileEntity(getLevel(), pos) instanceof ILogisticProvider lc) {
			return lc.getContainer().getChest().insertItem(slot, is, false);
			
		}
		return is;
	}
	@Override
	public ItemStack takeItem(ItemKey key, int amount) {
		if(CUtils.getExistingTileEntity(getLevel(), pos) instanceof ILogisticProvider lc) {
			return lc.getContainer().getChest().extractItem(slot, amount, false);
			
		}
		return ItemStack.EMPTY;
	}
	@Override
	public Map<ItemKey, ? extends ItemCountProvider> getAllItems() {
		return null;
	}
	@Override
	public boolean isChanged() {
		return changed;
	}
	@Override
	public void tick() {
		
	}
	@Override
	public boolean consumeChange() {
		boolean isChange=changed;
		changed=false;
		return isChange;
	}
	@Override
	public BlockPos getPos() {
		return pos;
	}
	@Override
	public Level getLevel() {
		return level;
	}
	@Override
	public boolean fillable() {
		return true;
	}

}
