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

import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.util.IOwnerTile;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;

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
    boolean isLocked = false;
    private int checkInterval = 0;

    public AbstractGenerator(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
        super(multiblockInstance, type, hasRSControl);
    }

    public int getActualRange() {
        return (int) (8 + (getRangeLevel() - 1) * 4);
    }

    public int getActualTemp() {
        return (int) (getTemperatureLevel() * 10);
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        isWorking = nbt.getBoolean("isWorking");
        isOverdrive = nbt.getBoolean("isOverdrive");
        isActualOverdrive = nbt.getBoolean("Overdriven");
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putBoolean("isWorking", isWorking);
        nbt.putBoolean("isOverdrive", isOverdrive);
        nbt.putBoolean("Overdriven", isActualOverdrive);
    }

    @Override
    public void disassemble() {
        ChunkData.removeTempAdjust(world, getPos());
        if (shouldUnique() && master() != null)
            master().unregist();
        super.disassemble();
    }

    public void unregist() {
        UUID owner = getOwner();
        if (owner == null) return;
        CompoundNBT vars = ResearchDataAPI.getVariants(owner);
        if (!vars.contains(ResearchVariant.GENERATOR_LOCATION.getToken())) return;
        long pos = vars.getLong(ResearchVariant.GENERATOR_LOCATION.getToken());
        BlockPos bp = BlockPos.fromLong(pos);
        if (bp.equals(this.pos))
            vars.remove(ResearchVariant.GENERATOR_LOCATION.getToken());
    }

    public void regist() {
        ResearchDataAPI.putVariantLong(getOwner(),ResearchVariant.GENERATOR_LOCATION,master().pos.toLong());
    }

    public void setOwner(UUID owner) {
        forEachBlock(s -> IOwnerTile.setOwner(s, owner));
    }

    public boolean shouldWork() {
        UUID owner = getOwner();
        if (owner == null) return false;
        CompoundNBT vars = ResearchDataAPI.getVariants(owner);
        if(!ResearchDataAPI.getData(owner).building.has(super.multiblockInstance))return false;
        if (!vars.contains(ResearchVariant.GENERATOR_LOCATION.getToken())) {
            vars.putLong(ResearchVariant.GENERATOR_LOCATION.getToken(), master().pos.toLong());
            return true;
        }
        long pos = vars.getLong(ResearchVariant.GENERATOR_LOCATION.getToken());
        BlockPos bp = BlockPos.fromLong(pos);
        if (bp.equals(this.pos))
            return true;
        return false;
    }

    protected abstract void onShutDown();

    protected abstract void tickFuel();

    protected abstract void tickEffects(boolean isActive);

    public abstract boolean shouldUnique();

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
                setAllActive(false);
                onShutDown();
                ChunkData.removeTempAdjust(world, getPos());
            }
        if (!world.isRemote && formed && !isDummy() && isWorking()) {
            if (shouldUnique()) {
                if (checkInterval <= 0) {
                    if (getOwner() != null)
                        checkInterval = 10;
                    isLocked = !shouldWork();
                } else checkInterval--;
            }
            final boolean activeBeforeTick = getIsActive();
            if (!isLocked)
                tickFuel();
            else
                this.setActive(false);
            // set activity status
            final boolean activeAfterTick = getIsActive();
            if (activeBeforeTick != activeAfterTick) {
                this.markDirty();
                if (activeAfterTick) {
                    ChunkData.addCubicTempAdjust(world, getPos(), getActualRange(), getActualTemp());
                } else {
                    ChunkData.removeTempAdjust(world, getPos());
                }
                setAllActive(activeAfterTick);
            } else if (activeAfterTick) {
                if (isChanged() || !initialized) {
                    initialized = true;
                    markChanged(false);
                    ChunkData.addCubicTempAdjust(world, getPos(), getActualRange(), getActualTemp());
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

    public float getTemperatureLevel() {
        if (master() != null)
            return master().temperatureLevel * (isActualOverdrive() ? master().overdriveBoost : 1);
        return 1;
    }

    public float getRangeLevel() {
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

    protected void setAllActive(boolean state) {
        forEachBlock(s -> s.setActive(state));
    }

    public abstract void forEachBlock(Consumer<T> consumer);

    UUID getOwner() {
        return IOwnerTile.getOwner(this);
    }
}
