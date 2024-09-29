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

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CokeOvenLogic.State;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.core.Vec3i;

/**
 * Common base class for any generator like block that maintains a heat area
 */
public abstract class ZoneHeatingMultiblockLogic<T extends ZoneHeatingMultiblockLogic<T>> implements  IServerTickableComponent<State> {
    private float temperatureLevel;
    private float rangeLevel;
    private float lastTLevel;
    private float lastRLevel;
    private boolean initialized;
    boolean isWorking;
    boolean isOverdrive;



    protected abstract void callBlockConsumerWithTypeCheck(Consumer<T> consumer, BlockEntity te);
    

    public final void forEachBlock(Consumer<T> consumer) {
        Vec3i vec = this.multiblockInstance.getSize(level);
        for (int x = 0; x < vec.getX(); ++x)
            for (int y = 0; y < vec.getY(); ++y)
                for (int z = 0; z < vec.getZ(); ++z) {
                    BlockPos actualPos = getBlockPosForPos(new BlockPos(x, y, z));
                    BlockEntity te = Utils.getExistingTileEntity(level, actualPos);
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
        return Mth.ceil(getRangeLevel());
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
        return Mth.ceil(getRangeLevel() * 4);
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
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
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
        if (level != null && level.isClientSide && formed) {
            tickEffects(getIsActive());
        }

        if (!level.isClientSide && formed) {
        	
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
                    ChunkHeatData.addPillarTempAdjust(level, getBlockPos(), nrlevel, getUpperBound(),
                            getLowerBound(), ntlevel);
                }else {
                	ChunkHeatData.removeTempAdjust(level, getBlockPos());
                }
            } else if (activeAfterTick) {
                if (!initialized) {
                    initialized = true;
                }
            }
            this.setChanged();
            shutdownTick();
        }
    }

    protected void tickControls() {
    }

    protected abstract void tickEffects(boolean isActive);

    protected abstract boolean tickFuel();

    public abstract void tickHeat(boolean isWorking);
    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        if(!this.isDummy()||descPacket) {
	        nbt.putBoolean("isWorking", isWorking);
	        nbt.putBoolean("isOverdrive", isOverdrive);
	        nbt.putFloat("temperatureLevel", temperatureLevel);
	        nbt.putFloat("rangeLevel", rangeLevel);
        }
    }
}
