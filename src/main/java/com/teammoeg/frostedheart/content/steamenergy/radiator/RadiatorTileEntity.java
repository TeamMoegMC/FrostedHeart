/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.steamenergy.radiator;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.generator.AbstractGenerator;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.NetworkHolder;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

public class RadiatorTileEntity extends AbstractGenerator<RadiatorTileEntity> implements
        INetworkConsumer, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, FHBlockInterfaces.IActiveState, ITickableTileEntity {
    public float power = 0;
    public int process = 0;
    public int processMax = 0;
    public float tempLevelLast;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    public RadiatorTileEntity() {
        super(FHMultiblocks.RADIATOR, FHTileTypes.RADIATOR.get(), false);
    }

    NetworkHolder network=new NetworkHolder();


    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        power = nbt.getFloat("power");
        process = nbt.getInt("process");
        processMax = nbt.getInt("processMax");
        tempLevelLast = nbt.getFloat("temp");
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putFloat("power", power);
        nbt.putInt("process", process);
        nbt.putInt("processMax", processMax);
        nbt.putFloat("temp", tempLevelLast);
    }

    @Override
    public boolean connect(Direction to,int dist) {
        if (this.offsetToMaster.getY() != 0) return false;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof EnergyNetworkProvider) {
            network.connect(((EnergyNetworkProvider) te).getNetwork(),dist);
            return true;
        }
        return false;
    }


    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return false;
    }

    public float getMaxPower() {
        return 5000;
    }

    @Override
    public int[] getCurrentProcessesStep() {
        return new int[]{processMax - process};
    }

    @Override
    public int[] getCurrentProcessesMax() {
        return new int[]{processMax};
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return this.offsetToMaster.getY() == 0;
    }

    @Override
    protected void onShutDown() {
    }

    @Override
    protected void tickFuel() {
        if (network.isValid()) {
        	network.tick();
            float actual = network.drainHeat(Math.min(24, getMaxPower() - power));
            if (actual > 0) {
                power += actual;
                //world.notifyBlockUpdate(this.getPos(),this.getBlockState(),this.getBlockState(),3);
            }
        }
        if (process > 0) {
            if (network.isValid())
                process -= network.getTemperatureLevel();
            else
                process -= tempLevelLast;
        } else if (network.isValid()&& power >= 4 * 160 * network.getTemperatureLevel()) {
            power -= 4 * 160 * network.getTemperatureLevel();
            process = (int) (160 * network.getTemperatureLevel());
            processMax = (int) (160 * network.getTemperatureLevel());
            this.setActive(true);
        } else {
            this.setActive(false);
        }
        if (network.isValid() && tempLevelLast != network.getTemperatureLevel()) {
            tempLevelLast = network.getTemperatureLevel();
            this.markChanged(true);
        }
    }

    @Override
    public boolean isWorking() {
        return true;
    }

    @Override
    public int getActualRange() {
        return 5;
    }

    @Override
    public int getActualTemp() {
        return (int) (tempLevelLast * 10);
    }

    @Override
    protected void tickEffects(boolean isActive) {
        if (world != null && world.isRemote && isActive && world.rand.nextFloat() < 0.2) {
            ClientUtils.spawnSteamParticles(world, this.getPos());
        }
    }

    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }
	@Override
	public void forEachBlock(Consumer<RadiatorTileEntity> consumer) {
        for (int y = 0; y < 3; ++y) {
            BlockPos actualPos = getBlockPosForPos(new BlockPos(0, y, 0));
            TileEntity te = Utils.getExistingTileEntity(world, actualPos);
            if (te instanceof RadiatorTileEntity)
               consumer.accept((RadiatorTileEntity) te);
        }
	}

	@Override
	public boolean shouldUnique() {
		return false;
	}
	@Override
	public NetworkHolder getHolder() {
		return network;
	}
}
