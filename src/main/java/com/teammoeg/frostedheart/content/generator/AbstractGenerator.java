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

package com.teammoeg.frostedheart.content.generator;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;

public abstract class AbstractGenerator<T extends AbstractGenerator<T>> extends MultiblockPartTileEntity<T> implements FHBlockInterfaces.IActiveState {

    public int temperatureLevel;
    public int rangeLevel;
    public int overdriveBoost;
    private boolean initialized;
    boolean isUserOperated;
    boolean isWorking;
    boolean isOverdrive;
    boolean isActualOverdrive;
    boolean isDirty;//mark if temperature change required

    public AbstractGenerator(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
        super(multiblockInstance, type, hasRSControl);
    }

    public int getActualRange() {
        return 8 + (getRangeLevel() - 1) * 4;
    }

    public int getActualTemp() {
        return (int) (getTemperatureLevel() * 10);
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        setWorking(nbt.getBoolean("isWorking"));
        setOverdrive(nbt.getBoolean("isOverdrive"));
        setActualOverdrive(nbt.getBoolean("Overdriven"));
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putBoolean("isWorking", isWorking());
        nbt.putBoolean("isOverdrive", isOverdrive());
        nbt.putBoolean("Overdriven", isActualOverdrive());

    }

    @Override
    public void disassemble() {
        super.disassemble();
        ChunkData.removeTempAdjust(world, getPos());
    }

    protected abstract void onShutDown();

    protected abstract void tickFuel();

    protected abstract void setAllActive(boolean state);

    protected abstract void tickEffects(boolean isActive);

    @Override
    public void tick() {
        checkForNeedlessTicking();
        // spawn smoke particle
        if (world != null && world.isRemote && formed && !isDummy()) {
            tickEffects(getIsActive());
        }
        //user set shutdown
        if (isUserOperated())
            if (!world.isRemote && formed && !isDummy() && !isWorking()) {
                setActive(false);
                onShutDown();
                ChunkData.removeTempAdjust(world, getPos());
            }
        if (!world.isRemote && formed && !isDummy() && isWorking()) {
            final boolean activeBeforeTick = getIsActive();
            tickFuel();
            // set activity status
            final boolean activeAfterTick = getIsActive();
            if (activeBeforeTick != activeAfterTick) {
                this.markDirty();
                if (activeAfterTick) {
                    ChunkData.addCubicTempAdjust(world, getPos(), getActualRange(), (byte) getActualTemp());
                } else {
                    ChunkData.removeTempAdjust(world, getPos());
                }
                setAllActive(activeAfterTick);
            } else if (activeAfterTick) {
                if (isChanged() || !initialized) {
                    initialized = true;
                    markChanged(false);
                    ChunkData.addCubicTempAdjust(world, getPos(), getActualRange(), (byte) getActualTemp());
                }
            }
        }

    }

    public void setWorking(boolean working) {
        if (master() != null) {
            master().isWorking = working;
            setUserOperated(true);
        }
    }

    public boolean isWorking() {
        if (master() != null)
            return master().isWorking;
        return false;
    }

    public boolean isOverdrive() {
        if (master() != null)
            return master().isOverdrive;
        return false;
    }

    public void markChanged(boolean dirty) {
        if (master() != null)
            master().isDirty = dirty;
    }

    public boolean isChanged() {
        if (master() != null)
            return master().isDirty;
        return false;
    }

    public void setOverdrive(boolean overdrive) {
        if (master() != null) {
            setUserOperated(true);
            master().isOverdrive = overdrive;
        }
    }

    public boolean isActualOverdrive() {
        if (master() != null)
            return master().isActualOverdrive;
        return false;
    }

    public void setActualOverdrive(boolean isActualOverdrive) {
        if (master() != null) {
            markChanged(true);
            master().isActualOverdrive = isActualOverdrive;
        }
    }

    public int getTemperatureLevel() {
        if (master() != null)
            return master().temperatureLevel * (isActualOverdrive() ? master().overdriveBoost : 1);
        return 1;
    }

    public int getRangeLevel() {
        if (master() != null)
            return master().rangeLevel;
        return 1;
    }

    public boolean isUserOperated() {
        if (master() != null)
            return master().isUserOperated;
        return false;
    }

    public void setUserOperated(boolean isUserOperated) {
        if (master() != null)
            master().isUserOperated = isUserOperated;
    }

}