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

package com.teammoeg.frostedheart.content.generator;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3i;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Common base class for any generator like block that maintains a heat area
 */
public abstract class ZoneHeatingMultiblockTileEntity<T extends ZoneHeatingMultiblockTileEntity<T>> extends MultiblockPartTileEntity<T>
        implements FHBlockInterfaces.IActiveState {
    private float temperatureLevel;
    private float rangeLevel;
    private float lastTLevel;
    private float lastRLevel;
    private boolean initialized;
    boolean isWorking;
    boolean isOverdrive;
    boolean isDirty;// mark if temperature change required
    int heated = 0;
    float heatAddInterval = 20;    //ticks

    public ZoneHeatingMultiblockTileEntity(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
        super(multiblockInstance, type, hasRSControl);
    }

    protected abstract void callBlockConsumerWithTypeCheck(Consumer<T> consumer, TileEntity te);

    @Override
    public void disassemble() {
        if (this == master())
            ChunkHeatData.removeTempAdjust(world, getPos());
        super.disassemble();
    }

    public final void forEachBlock(Consumer<T> consumer) {
        Vector3i vec = this.multiblockInstance.getSize(world);
        for (int x = 0; x < vec.getX(); ++x)
            for (int y = 0; y < vec.getY(); ++y)
                for (int z = 0; z < vec.getZ(); ++z) {
                    BlockPos actualPos = getBlockPosForPos(new BlockPos(x, y, z));
                    TileEntity te = Utils.getExistingTileEntity(world, actualPos);
                    callBlockConsumerWithTypeCheck(consumer, te);
                }
    }

    public int getActualRange() {
        return (int) (8 + (getRangeLevel()) * 4);
    }

    public int getActualTemp() {
        return (int) (getTemperatureLevel() * 10);
    }

    public int getHeated() {
        return heated;
    }

    public int getLowerBound() {
        return MathHelper.ceil(getRangeLevel());
    }

    public int getMaxHeated() {
        if (isOverdrive()) {
            return 200;
        }
        return 100;
    }

    UUID getOwner() {
        return IOwnerTile.getOwner(this);
    }


    public final float getRangeLevel() {
        T master = master();
        if (master == this)
            return rangeLevel;
        return master == null ? 1 : master.getRangeLevel();
    }

    protected Optional<Team> getTeam() {
        UUID owner = getOwner();
        if (owner != null)
            return Optional.ofNullable(TeamManager.INSTANCE.getTeamByID(owner));
        return Optional.empty();
    }

    protected Optional<TeamResearchData> getTeamData() {
        UUID owner = getOwner();
        if (owner != null)
            return Optional.of(ResearchDataAPI.getData(owner));
        return Optional.empty();
    }


    public final float getTemperatureLevel() {
        T master = master();
        if (master == this)
            return temperatureLevel;
        return master == null ? 1 : master.getTemperatureLevel();
    }

    public int getUpperBound() {
        return MathHelper.ceil(getRangeLevel() * 4);
    }

    public boolean isChanged() {
        if (master() != null)
            return master().isDirty;
        return false;
    }

    public boolean isOverdrive() {
        if (master() != null)
            return master().isOverdrive;
        return false;
    }

    public boolean isWorking() {
        if (master() != null)
            return master().isWorking;
        return false;
    }

    public void markChanged(boolean dirty) {
        if (master() != null)
            master().isDirty = dirty;
    }

    protected abstract void onShutDown();

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        isWorking = nbt.getBoolean("isWorking");
        isOverdrive = nbt.getBoolean("isOverdrive");
        temperatureLevel = nbt.getFloat("temperatureLevel");
        rangeLevel = nbt.getFloat("rangeLevel");
        heated = nbt.getInt("heated");
    }

    protected void setAllActive(boolean state) {
        forEachBlock(s -> s.setActive(state));
    }

    public void setOverdrive(boolean overdrive) {
        if (master() != null) {
            master().isOverdrive = overdrive;
        }
    }

    public void setOwner(UUID owner) {
        forEachBlock(s -> IOwnerTile.setOwner(s, owner));
    }

    public final void setRangeLevel(float f) {
        if (master() == this) {
            if (this.rangeLevel != f)
                isDirty = true;
            this.rangeLevel = f;
        } else {
            master().setTemperatureLevel(f);
        }
    }

    public final void setTemperatureLevel(float temperatureLevel) {
        if (master() == this) {
            if (this.temperatureLevel != temperatureLevel)
                isDirty = true;
            this.temperatureLevel = temperatureLevel;
        } else {
            master().setTemperatureLevel(temperatureLevel);
        }
    }


    public void setWorking(boolean working) {
        if (master() != null) {
            master().isWorking = working;
        }
    }

    public void shutdownTick() {
    }

    @Override
    public void tick() {
        checkForNeedlessTicking();
        if (isDummy())
            return;
        // spawn smoke particle
        if (world != null && world.isRemote && formed) {
            tickEffects(getIsActive());
        }

        tickControls();

        if (!world.isRemote && formed) {

            final boolean activeBeforeTick = getIsActive();
            tickFuel();
            tickHeat();
            // set activity status
            final boolean activeAfterTick = getIsActive();
            int ntlevel = getActualTemp();
            int nrlevel = getActualRange();
            if (activeBeforeTick != activeAfterTick || lastTLevel != ntlevel || lastRLevel != nrlevel) {
                this.isDirty = false;
                lastTLevel = ntlevel;
                lastRLevel = nrlevel;
                this.markDirty();
                if (nrlevel > 0 && ntlevel > 0) {
                    ChunkHeatData.addPillarTempAdjust(world, getPos(), nrlevel, getUpperBound(),
                            getLowerBound(), ntlevel);
                }
            } else if (activeAfterTick) {
                if (isChanged() || !initialized) {
                    initialized = true;
                    markChanged(false);
                }
            }

        }
    }

    protected void tickControls() {
    }

    protected abstract void tickEffects(boolean isActive);

    protected abstract void tickFuel();

    public void tickHeat() {
        if (isWorking() && heated != getMaxHeated()) {
            Random random = world.rand;
            boolean needAdd = false;
            float heatAddProbability = 1F / heatAddInterval;
            if (isOverdrive()) {
                heatAddProbability = 2F / heatAddInterval;
            }
            if (random.nextFloat() < heatAddProbability) {
                needAdd = true;
                markContainingBlockForUpdate(null);
            }
            if (heated < getMaxHeated() && needAdd) {
                heated++;
            } else if (heated > getMaxHeated() && needAdd) {
                heated--;
            }
        } else if (!isWorking()) {
            if (heated == 0) {
                shutdownTick();
                ChunkHeatData.removeTempAdjust(world, getPos());
                setAllActive(false);
            } else {
                markContainingBlockForUpdate(null);
                Random random = world.rand;
                float heatAddProbability = 1F / heatAddInterval;
                if (random.nextFloat() < heatAddProbability) {
                    heated--;
                }
            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putBoolean("isWorking", isWorking);
        nbt.putBoolean("isOverdrive", isOverdrive);
        nbt.putFloat("temperatureLevel", temperatureLevel);
        nbt.putFloat("rangeLevel", rangeLevel);
        nbt.putInt("heated", heated);
    }
}
