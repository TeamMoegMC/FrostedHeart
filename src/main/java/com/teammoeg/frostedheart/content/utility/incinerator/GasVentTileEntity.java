/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility.incinerator;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces.IActiveState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import com.teammoeg.frostedheart.base.capability.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class GasVentTileEntity extends FHBaseTileEntity implements IActiveState, TickableBlockEntity {
    FluidTank input = new FluidTank(10000, s -> s.getFluid().getAttributes().isGaseous());
    private LazyOptional<IFluidHandler> holder = LazyOptional.empty();

    public GasVentTileEntity() {
        super(FHTileTypes.GAS_VENT.get());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.holder.isPresent()) {
            this.refreshCapability();
        }
        return cap == ForgeCapabilities.FLUID_HANDLER ? holder.cast() : super.getCapability(cap, side);
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean dp) {
        input.readFromNBT(nbt.getCompound("in"));
    }


    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldCap = this.holder;
        this.holder = LazyOptional.of(() -> new IFluidHandler() {
                    @Override
                    public FluidStack drain(FluidStack resource, FluidAction action) {
                        return FluidStack.EMPTY;
                    }

                    @Override
                    public FluidStack drain(int maxDrain, FluidAction action) {
                        return FluidStack.EMPTY;
                    }

                    @Override
                    public int fill(FluidStack resource, FluidAction action) {
                        return input.fill(resource, action);
                    }

                    @Override
                    public FluidStack getFluidInTank(int tank) {
                        return input.getFluidInTank(tank);
                    }

                    @Override
                    public int getTankCapacity(int tank) {
                        return input.getCapacity();
                    }

                    @Override
                    public int getTanks() {
                        return input.getTanks();
                    }

                    @Override
                    public boolean isFluidValid(int tank, FluidStack stack) {
                        return input.isFluidValid(tank, stack);
                    }

                }

        );
        oldCap.invalidate();
    }

    @Override
    public void tick() {
        if (this.level != null && !this.level.isClientSide) {
            int val = input.drain(1000, FluidAction.EXECUTE).getAmount();
            if (val > 0) {
                if (!this.getIsActive()) {
                    this.setActive(true);
                }
                this.setChanged();
            } else if (this.getIsActive())
                this.setActive(false);

        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean dp) {
        nbt.put("in", input.writeToNBT(new CompoundTag()));
    }
}
