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

import org.jetbrains.annotations.NotNull;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class UIFluidTank implements IFluidTank{
	CDataSlot<FluidStack> FLUID;
	int capacity;
	public UIFluidTank(CBaseMenu menu,int capacity) {
		FLUID=CCustomMenuSlot.SLOT_TANK.create(menu);
		this.capacity=capacity;
	}
	public void bind(FluidTank tank) {
		FLUID.bind(()->tank.getFluid(),t->tank.setFluid(t));
	}

	@Override
	public @NotNull FluidStack getFluid() {
		return FLUID.getValue();
	}

	@Override
	public int getFluidAmount() {
		return FLUID.getValue().getAmount();
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public boolean isFluidValid(FluidStack stack) {
		return true;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}
	
}