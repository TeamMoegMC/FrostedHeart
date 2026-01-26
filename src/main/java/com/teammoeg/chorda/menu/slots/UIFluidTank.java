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