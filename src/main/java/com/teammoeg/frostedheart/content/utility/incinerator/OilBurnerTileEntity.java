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

import com.simibubi.create.foundation.fluid.FluidHelper;
import com.teammoeg.frostedheart.FHBlockEntityTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces.IActiveState;
import com.teammoeg.frostedheart.base.blockentity.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHTickableBlockEntity;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class OilBurnerTileEntity extends FHBaseTileEntity implements IActiveState, FHTickableBlockEntity {
    TagKey<Fluid> burnable = FluidTags.create(new ResourceLocation("frostedheart", "flammable_fluid"));
    FluidTank input = new FluidTank(10000, s -> s.getFluid().is(burnable));
    int vals;
    private LazyOptional<IFluidHandler> holder = LazyOptional.empty();

    public OilBurnerTileEntity(BlockPos bp,BlockState bs) {
        super(FHBlockEntityTypes.OIL_BURNER.get(),bp,bs);
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
        vals = nbt.getInt("burntick");
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
            BlockEntity down = Utils.getExistingTileEntity(level, worldPosition.relative(Direction.DOWN));
            if (down != null) {
                LazyOptional<IFluidHandler> cap = down.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP);
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
            int drained = input.drain(1000, FluidAction.EXECUTE).getAmount();
            if (drained >= 5) {
                vals = Math.min(vals + drained / 5, 100);
            }
            if(vals>0)
            	this.setChanged();
            if (this.getIsActive()) {
                vals--;
            } else if (vals > 20) {
                this.setActive(true);
            }
            if (vals <= 0)
                this.setActive(false);
            
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean dp) {
        nbt.put("in", input.writeToNBT(new CompoundTag()));
        nbt.putInt("burntick", vals);
    }
}
