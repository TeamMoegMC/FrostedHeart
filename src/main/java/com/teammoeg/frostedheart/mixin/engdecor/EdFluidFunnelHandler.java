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

package com.teammoeg.frostedheart.mixin.engdecor;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import blusunrize.immersiveengineering.api.energy.immersiveflux.FluxStorageAdvanced;
import blusunrize.immersiveengineering.common.util.EnergyHelper;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.IFluidTank;
import wile.engineersdecor.blocks.EdFluidFunnel.FluidFunnelTileEntity;

@Mixin(FluidFunnelTileEntity.class)
public abstract class EdFluidFunnelHandler extends TileEntity
        implements ITickableTileEntity, ICapabilityProvider, IFluidTank, IEnergyStorage {

    public FluxStorageAdvanced energyStorage = new FluxStorageAdvanced(1000);

    EnergyHelper.IEForgeEnergyWrapper wrapper = null;

    public EdFluidFunnelHandler(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public boolean canExtract() {
        return false;
    }


    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Inject(at = @At("HEAD"), method = "readnbt", remap = false)
    public void fh$readnbt(CompoundNBT nbt, CallbackInfo cbi) {
        energyStorage.readFromNBT(nbt);
    }

    @Inject(at = @At("HEAD"), method = "try_collect", remap = false, cancellable = true)
    public void fh$try_collect(BlockPos collection_pos, CallbackInfoReturnable<Boolean> cbi) {
        if (energyStorage.getEnergyStored() < 150)
            cbi.setReturnValue(false);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/FluidUtil;getFluidHandler(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;)Lnet/minecraftforge/common/util/LazyOptional;"), method = "try_pick", remap = false, cancellable = true)
    private void fh$try_pick(BlockPos pos, FluidState fluidstate, CallbackInfoReturnable<Boolean> cbi) {
        if (energyStorage.getEnergyStored() >= 150)
            energyStorage.extractEnergy(150, false);
        else
            cbi.setReturnValue(false);
    }

    @Inject(at = @At("HEAD"), method = "writenbt", remap = false)
    public void fh$writenbt(CompoundNBT nbt, CallbackInfo cbi) {
        energyStorage.writeToNBT(nbt);
    }

    @Inject(at = @At("HEAD"), method = "getCapability", remap = false, cancellable = true)
    public void getCapability(net.minecraftforge.common.capabilities.Capability capability, @Nullable Direction facing,
                              CallbackInfoReturnable<LazyOptional> cbi) {
        if (capability == CapabilityEnergy.ENERGY && facing != Direction.UP) {

            cbi.setReturnValue(LazyOptional.of(() -> this));
        }
    }

    @Override
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return energyStorage.receiveEnergy(maxReceive, simulate);
    }
}
