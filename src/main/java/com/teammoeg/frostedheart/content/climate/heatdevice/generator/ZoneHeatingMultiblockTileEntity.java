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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3i;

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
    	float rlevel=getRangeLevel();
    	if(rlevel<=1)
    		return (int) (12*rlevel);
    	return (int) (12+(rlevel-1)*4);
    }

    public int getActualTemp() {
        return (int) (getTemperatureLevel() * 10);
    }



    public int getLowerBound() {
        return MathHelper.ceil(getRangeLevel());
    }



    UUID getOwner() {
        return IOwnerTile.getOwner(this);
    }


    public final float getRangeLevel() {
    	if(master()==this)
    		return rangeLevel;
    	return master().getRangeLevel();
    }


    protected Optional<TeamDataHolder> getTeamData() {
        UUID owner = getOwner();
        if (owner != null)
            return Optional.ofNullable(FHTeamDataManager.getDataByResearchID(owner));
        return Optional.empty();
    }


    public final float getTemperatureLevel() {
    	if(master()==this)
    		return temperatureLevel;
    	return master().getTemperatureLevel();
    }

    public int getUpperBound() {
        return MathHelper.ceil(getRangeLevel() * 4);
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

    protected abstract void onShutDown();

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        
        isWorking = nbt.getBoolean("isWorking");
        isOverdrive = nbt.getBoolean("isOverdrive");
        temperatureLevel = nbt.getFloat("temperatureLevel");
        rangeLevel = nbt.getFloat("rangeLevel");
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

    public void setRangeLevel(float f) {
    	if(master()==this)
    		this.rangeLevel = f;
    	else
    		master().setRangeLevel(f);
    }

    public void setTemperatureLevel(float temperatureLevel) {
    	if(master()==this)
    		this.temperatureLevel = temperatureLevel;
    	else
    		master().setTemperatureLevel(temperatureLevel);
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

        if (!world.isRemote && formed) {
        	
            final boolean activeBeforeTick = getIsActive();
            boolean isActive=tickFuel();
            tickHeat(isActive);
            setAllActive(isActive);
            // set activity status
            final boolean activeAfterTick = getIsActive();
            int ntlevel = getActualTemp();
            int nrlevel = getActualRange();
            if (activeBeforeTick != activeAfterTick || lastTLevel != ntlevel || lastRLevel != nrlevel) {
                lastTLevel = ntlevel;
                lastRLevel = nrlevel;
                
                if (nrlevel > 0 && ntlevel > 0) {
                    ChunkHeatData.addPillarTempAdjust(world, getPos(), nrlevel, getUpperBound(),
                            getLowerBound(), ntlevel);
                }else {
                	ChunkHeatData.removeTempAdjust(world, getPos());
                }
            } else if (activeAfterTick) {
                if (!initialized) {
                    initialized = true;
                }
            }
            this.markDirty();
            shutdownTick();
        }
    }

    protected void tickControls() {
    }

    protected abstract void tickEffects(boolean isActive);

    protected abstract boolean tickFuel();

    public abstract void tickHeat(boolean isWorking);
    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        if(!this.isDummy()||descPacket) {
	        nbt.putBoolean("isWorking", isWorking);
	        nbt.putBoolean("isOverdrive", isOverdrive);
	        nbt.putFloat("temperatureLevel", temperatureLevel);
	        nbt.putFloat("rangeLevel", rangeLevel);
        }
    }
}
