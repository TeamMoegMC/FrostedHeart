/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Steam Powered.
 *
 * Steam Powered is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Steam Powered is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Steam Powered. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.decoration.oilburner;

import blusunrize.immersiveengineering.common.util.Utils;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces.IActiveState;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class OilBurnerTileEntity extends FHBaseTileEntity implements IActiveState, ITickableTileEntity {
    ResourceLocation burnable = new ResourceLocation("frostedheart", "flammable_fluid");
    FluidTank input = new FluidTank(10000, s -> s.getFluid().getTags().contains(burnable));
    int vals;
    private LazyOptional<IFluidHandler> holder = LazyOptional.empty();

    public OilBurnerTileEntity() {
        super(FHTileTypes.OIL_BURNER.get());
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean dp) {
        input.readFromNBT(nbt.getCompound("in"));
        vals=nbt.getInt("burntick");
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean dp) {
        nbt.put("in", input.writeToNBT(new CompoundNBT()));
        nbt.putInt("burntick", vals);
    }


    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote) {
            TileEntity down = Utils.getExistingTileEntity(world, pos.offset(Direction.DOWN));
            if (down != null) {
                LazyOptional<IFluidHandler> cap = down.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.UP);
                if (cap.isPresent()) {
                    IFluidHandler ifh = cap.resolve().orElse(null);
                    if (ifh != null) {
                        if (!input.isEmpty()) {
                            FluidStack fs = ifh.drain(FluidHelper.copyStackWithAmount(input.getFluid(), 5), FluidAction.EXECUTE);
                            if (!fs.isEmpty())
                                input.fill(fs, FluidAction.EXECUTE);
                        } else
                            for (int i = 0; i < ifh.getTanks(); i++) {
                                if (input.isFluidValid(ifh.getFluidInTank(i))) {
                                    FluidStack fs = ifh.drain(FluidHelper.copyStackWithAmount(ifh.getFluidInTank(i), 5), FluidAction.EXECUTE);
                                    if (!fs.isEmpty()) {
                                        input.fill(fs, FluidAction.EXECUTE);
                                        break;
                                    }
                                }
                            }
                    }
                }
            }
            int drained=input.drain(1000, FluidAction.EXECUTE).getAmount();
            if (drained >= 5) {
            	vals=Math.min(vals+drained/5, 100);
            }
            if(this.getIsActive()) {
            	vals--;
            }else if(vals>20){
            	this.setActive(true);
            }
            if(vals<=0)
                this.setActive(false);
            this.markContainingBlockForUpdate(null);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.holder.isPresent()) {
            this.refreshCapability();
        }
        return cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? holder.cast() : super.getCapability(cap, side);
    }

    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldCap = this.holder;
        this.holder = LazyOptional.of(() -> new IFluidHandler() {
                    @Override
                    public int fill(FluidStack resource, FluidAction action) {
                        return input.fill(resource, action);
                    }

                    @Override
                    public FluidStack drain(int maxDrain, FluidAction action) {
                        return FluidStack.EMPTY;
                    }

                    @Override
                    public FluidStack drain(FluidStack resource, FluidAction action) {
                        return FluidStack.EMPTY;
                    }

                    @Override
                    public int getTanks() {
                        return input.getTanks();
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
                    public boolean isFluidValid(int tank, FluidStack stack) {
                        return input.isFluidValid(tank, stack);
                    }

                }

        );
        oldCap.invalidate();
    }
}
