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

package com.teammoeg.frostedheart.content.steamenergy.radiator;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.generator.ZoneHeatingMultiblockTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.SteamNetworkConsumer;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

public class RadiatorTileEntity extends ZoneHeatingMultiblockTileEntity<RadiatorTileEntity> implements
        INetworkConsumer, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, FHBlockInterfaces.IActiveState, ITickableTileEntity {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    SteamNetworkConsumer network = new SteamNetworkConsumer(100, 4);

    public RadiatorTileEntity() {
        super(FHMultiblocks.RADIATOR, FHTileTypes.RADIATOR.get(), false);
        
    }


    @Override
    protected void callBlockConsumerWithTypeCheck(Consumer<RadiatorTileEntity> consumer, TileEntity te) {
        if (te instanceof RadiatorTileEntity)
            consumer.accept((RadiatorTileEntity) te);
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return this.offsetToMaster.getY() == 0;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }


    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return false;
    }


    @Override
    public boolean connect(HeatEnergyNetwork manager,Direction to, int dist) {
        return network.reciveConnection(world, pos,manager, to, dist);
    }

    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Override
    public int getActualRange() {
        return 7;
    }

    @Override
    public int[] getCurrentProcessesMax() {
        return new int[]{(int) network.getMaxPower()};
    }

    @Override
    public int[] getCurrentProcessesStep() {
        return new int[]{(int) network.getPower()};
    }

    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public int getLowerBound() {
        return 1;
    }

    @Override
    public int getUpperBound() {
        return 4;
    }

    @Override
    public boolean isWorking() {
        return true;
    }

    @Override
    protected void onShutDown() {
    }


    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        network.load(nbt);
    }

    @Override
    protected void tickEffects(boolean isActive) {
        if (world != null && world.isRemote && isActive && world.rand.nextFloat() < 0.2) {
            ClientUtils.spawnSteamParticles(world, this.getPos());
        }
    }
    @Override
    protected boolean tickFuel() {
        if (!isWorking()) {
            if (this.getIsActive())
                this.setAllActive(false);
            return false;
        }
        boolean hasFuel=false;
        if (network.tryDrainHeat(4)) {

            this.setTemperatureLevel(network.getTemperatureLevel());
            this.setRangeLevel(0.5f);
            this.setAllActive(true);
            hasFuel=true;
        } else {
            this.setAllActive(false);
            this.setTemperatureLevel(0);
            this.setRangeLevel(0);
            hasFuel=false;
        }
        return hasFuel;
    }



    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        network.save(nbt);
    }




	@Override
	public void tickHeat(boolean isWorking) {
	}
}
