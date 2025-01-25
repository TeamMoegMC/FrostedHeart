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

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.robotics.logistics.ItemChangeListener;
import com.teammoeg.frostedheart.content.robotics.logistics.ItemHandlerListener;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticInternalPushTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

@SuppressWarnings("unused")
public class SupplierTileEntity extends CBlockEntity implements TaskableLogisticStorage,ItemChangeListener, CTickableBlockEntity {
	ItemStackHandler container=new ItemStackHandler(27);
	ItemHandlerListener handler=new ItemHandlerListener(container,this);
	LogisticTask[] tasks=new LogisticTask[27];

	public SupplierTileEntity(BlockEntityType<? extends BlockEntity> type,BlockPos pos,BlockState bs) {
		super(type,pos,bs);
	}

	@Override
	public ItemStackHandler getInventory() {
		return container;
	}



	@Override
	public LogisticTask[] getTasks() {
		return tasks;
	}

	@Override
	public void readCustomNBT(CompoundTag arg0, boolean arg1) {
		if(!arg1) {
			arg0.put("container", container.serializeNBT());
		}
	}

	@Override
	public void writeCustomNBT(CompoundTag arg0, boolean arg1) {
		container.deserializeNBT(arg0.getCompound("container"));
	}

	@Override
	public void onSlotChange(int slot, ItemStack after) {
		tasks[slot]=new LogisticInternalPushTask(this,slot);
	}

	@Override
	public void onSlotClear(int slot) {
		tasks[slot]=null;
	}

	@Override
	public void onCountChange(int slot, int before, int after) {
	}

	@Override
	public void tick() {
		FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.getCapability(this.level).resolve()
		.map(t->t.getNetworkFor(level,worldPosition))
		.ifPresent(t->t.update(this));
	}

	@Override
	public boolean isValidFor(ItemStack stack) {
		return false;
	}

}
