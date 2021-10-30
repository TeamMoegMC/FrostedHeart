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

package com.teammoeg.frostedheart.content.oilburner;

import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces.IActiveState;
import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class OilBurnerTileEntity extends IEBaseTileEntity implements IActiveState, ITickableTileEntity {
	ResourceLocation burnable=new ResourceLocation("forge","creosote");
    FluidTank input = new FluidTank(10000,s->s.getFluid().getTags().contains(burnable)) {
    };
    private LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> input);

    public OilBurnerTileEntity() {
    	super(FHContent.FHTileTypes.OIL_BURNER.get());
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt,boolean dp) {
        input.readFromNBT(nbt.getCompound("in"));
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt,boolean dp) {
        nbt.put("in", input.writeToNBT(new CompoundNBT()));
    }



    @Override
    public void tick() {
        //debug
        if (this.world != null && !this.world.isRemote) {
            if(input.drain(1000,FluidAction.EXECUTE).getAmount()>=100) {
            	this.setActive(true);
            }else
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
        this.holder = LazyOptional.of(() -> {
            return this.input;
        });
        oldCap.invalidate();
    }
}
