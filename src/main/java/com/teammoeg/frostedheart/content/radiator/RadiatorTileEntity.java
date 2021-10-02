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

package com.teammoeg.frostedheart.content.radiator;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.steamenergy.IConnectable;
import com.teammoeg.frostedheart.steamenergy.SteamEnergyNetwork;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;

public class RadiatorTileEntity extends IEBaseTileEntity implements
        IConnectable, IIEInventory, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, FHBlockInterfaces.IActiveState, ITickableTileEntity {
    NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    public float power = 0;
    public int process = 0;
    public int processMax = 0;
    public float tempLevelLast;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    public RadiatorTileEntity() {
        super(FHContent.FHTileTypes.RADIATOR.get());
    }

    SteamEnergyNetwork network;
    Direction last;

    SteamEnergyNetwork getNetwork() {
        if (network != null) return network;
        if (last == null) return null;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(last));
        if (te instanceof EnergyNetworkProvider) {
            network = ((EnergyNetworkProvider) te).getNetwork();
        } else {
            disconnectAt(last);
        }
        return network;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        ItemStackHelper.loadAllItems(nbt, inventory);
        power = nbt.getFloat("power");
        process = nbt.getInt("process");
        processMax = nbt.getInt("processMax");
        if (nbt.contains("dir"))
            last = Direction.values()[nbt.getInt("dir")];
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        ItemStackHelper.saveAllItems(nbt, inventory);
        nbt.putFloat("power", power);
        nbt.putInt("process", process);
        nbt.putInt("processMax", processMax);
        if (last != null)
            nbt.putInt("dir", last.ordinal());
    }

    @Override
    public boolean disconnectAt(Direction to) {
        if (last == to) {
            network = null;
            for (Direction d : Direction.values()) {
                if (d == to) continue;
                if (connectAt(d))
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean connectAt(Direction to) {
        if (to == Direction.UP) return false;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof EnergyNetworkProvider) {
            last = to;
            network = ((EnergyNetworkProvider) te).getNetwork();
            return true;
        } else
            disconnectAt(to);
        return false;
    }


    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return true;
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public void doGraphicalUpdates(int slot) {
    }
    public float getMaxPower() {
    	return 5000;
    }
    @Override
    public void tick() {
        SteamEnergyNetwork network = getNetwork();
        boolean isDirty = false;
        if (network != null) {
            float actual = network.drainHeat(Math.min(24,getMaxPower() - power));
            if (actual > 0) {
                power += actual;
                isDirty = true;
                //world.notifyBlockUpdate(this.getPos(),this.getBlockState(),this.getBlockState(),3);
            }
        }
        boolean beforeState = this.getIsActive();
        boolean afterState = false;
        if (process > 0) {
            if (network != null)
                process -= network.getTemperatureLevel();
            else
                process-=tempLevelLast;
            isDirty = true;
            afterState = true;
        } else if (network != null && power >= 4*160 * network.getTemperatureLevel()) {
            power -= 4*160 * network.getTemperatureLevel();
            process = (int) (160 * network.getTemperatureLevel());
            processMax = (int) (160 * network.getTemperatureLevel());
            isDirty = true;
            afterState = true;
        }
        if (beforeState != afterState||(network!=null&&tempLevelLast!=network.getTemperatureLevel())) {
        	
            this.setActive(afterState);
            if (afterState) {
                if (network != null) {
                	tempLevelLast=network.getTemperatureLevel();
                    ChunkData.addCubicTempAdjust(this.getWorld(), this.getPos(), 5, (byte) (10 * network.getTemperatureLevel()));
                }else
                    ChunkData.addCubicTempAdjust(this.getWorld(), this.getPos(), 5, (byte) (10*tempLevelLast));
            } else
                ChunkData.removeTempAdjust(this.getWorld(), this.getPos());
        }
        if (isDirty) {
            markDirty();
            this.markContainingBlockForUpdate(null);
        }
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
		return to == Direction.UP;
	}

}
