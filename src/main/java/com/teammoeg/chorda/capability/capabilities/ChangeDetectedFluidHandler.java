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

package com.teammoeg.chorda.capability.capabilities;

import com.teammoeg.chorda.block.entity.SyncableBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class ChangeDetectedFluidHandler implements IFluidHandler {
	Runnable onchange;
	IFluidHandler nested;
	public ChangeDetectedFluidHandler(IFluidHandler nested,Runnable onchange) {
		super();
		this.onchange = onchange;
		this.nested = nested;
	}

	public static <T extends BlockEntity> IFluidHandler fromBESetChanged(T blockEntity,IFluidHandler nested) {
		return new ChangeDetectedFluidHandler(nested,()->{blockEntity.setChanged();});
	}
	
	public static <T extends BlockEntity& SyncableBlockEntity> IFluidHandler fromBESynced(T blockEntity, IFluidHandler nested) {
		return new ChangeDetectedFluidHandler(nested,()->{blockEntity.setChanged();blockEntity.syncData();});
	}
	@Override
	public int getTanks() {
		return nested.getTanks();
	}

	@Override
	public FluidStack getFluidInTank(int t) {
		return nested.getFluidInTank(t);
	}

	@Override
	public int getTankCapacity(int t) {
		return nested.getTankCapacity(t);
	}

	@Override
	public boolean isFluidValid(int t, FluidStack stack) {
		return nested.isFluidValid(t, stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int filled = nested.fill(resource, action);
		if (filled != 0 && action.execute()) {
			onContentsChanged();
		}
		return filled;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		FluidStack drained = nested.drain(resource, action);
		if (!drained.isEmpty() && action.execute()) {
			onContentsChanged();
		}
		return drained;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		FluidStack drained = nested.drain(maxDrain, action);
		if (!drained.isEmpty() && action.execute()) {
			onContentsChanged();
		}
		return drained;
	}
	protected void onContentsChanged() {
		onchange.run();
	}

}
