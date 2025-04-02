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

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.util.List;
import java.util.Optional;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GeneratorState extends HeatingState {
    /**
     * Remaining ticks to explode
     */
    int explodeTicks;
    boolean hasFuel;
    //int upgradeProcess;
    public boolean hasFuel() {
		return hasFuel;
	}

	public void setHasFuel(boolean hasFuel) {
		this.hasFuel = hasFuel;
	}

	public HeatEndpoint ep = new HeatEndpoint(200, 0);
    public GeneratorState() {
        super();
    }

    @Override
    public void writeSaveNBT(CompoundTag nbt) {
        super.writeSaveNBT(nbt);
        nbt.putInt("explodeTicks", explodeTicks);
		nbt.putBoolean("hasFuel", hasFuel);
		nbt.putBoolean("isActive", isActive());
        //upgradeProcess=nbt.getInt("upgradeProcess");
    }

    @Override
    public void readSaveNBT(CompoundTag nbt) {
        super.readSaveNBT(nbt);
        //upgradeProcess = nbt.getInt("upgradeProcess");
        //Optional<GeneratorData> data = this.getDataNoCheck();
        explodeTicks = nbt.getInt("explodeTicks");
		hasFuel=nbt.getBoolean("hasFuel");
		super.setActive(nbt.getBoolean("isActive"));
    }

    /**
     * @return the GeneratorData from the owned team.
     */
    public final Optional<GeneratorData> getDataNoCheck() {
        return getTeamData().map(t -> t.getData(FHSpecialDataTypes.GENERATOR_DATA));
    }

    /**
     * @param origin the origin to check
     * @return the GeneratorData from the owned team with the given origin.
     */
    public final Optional<GeneratorData> getData(BlockPos origin) {
        return getTeamData().map(t -> t.getData(FHSpecialDataTypes.GENERATOR_DATA)).filter(t -> origin.equals(t.actualPos));
    }
    public final void tickData(Level level,BlockPos origin) {
        Optional<TeamDataHolder> data= getTeamData();
        if(data.isPresent()) {
        	TeamDataHolder teamData=data.get();
        	GeneratorData dat=teamData.getData(FHSpecialDataTypes.GENERATOR_DATA);
        	this.setRangeLevel(0);
        	this.setTempLevel(0);
        	if(origin.equals(dat.actualPos)) {
        		dat.tick(level, teamData);
        		ep.setHeat(dat.lastPower);
        		this.setRangeLevel(dat.RLevel);
        		this.setTempLevel(dat.TLevel);
        		dat.lastPower=0;
        	}
        	
        }
    }
    /**
     * @param origin the origin to check
     * @return if the GeneratorData from the owned team with the given origin is present.
     */
    public boolean isDataPresent(BlockPos origin) {
        return getData(origin).isPresent();
    }

    /**
     * Register the given origin to the GeneratorData from the owned team.
     *
     * @param level  the level to check
     * @param origin the origin to check
     */
    public void regist(Level level, BlockPos origin) {
        getDataNoCheck().ifPresent(t -> {
            if (!origin.equals(t.actualPos)) {
                t.onPosChange();
                onDataChange();
            }
            t.actualPos = origin;
            t.dimension = level.dimension();
        });
    }

    /**
     * Try to register the given origin to the GeneratorData from the owned team.
     * Check if the origin is not the zero position.
     *
     * @param level  the level to check
     * @param origin the origin to check
     */
    public void tryRegist(Level level, BlockPos origin) {
        getDataNoCheck().ifPresent(t -> {
            if (t.actualPos==null) {
                if (!origin.equals(t.actualPos)) {
                    t.onPosChange();
                    onDataChange();
                }
                t.actualPos = origin;
                t.dimension = level.dimension();
            }
        });
    }

    @Override
    public int getDownwardRange() {
        return Mth.ceil(getRangeLevel() * 2 + 1);
    }

    @Override
    public int getUpwardRange() {
        return Mth.ceil(getRangeLevel() * 4 + 1);
    }
    public void onDataChange() {
    	
    }

	@Override
	public void writeSyncNBT(CompoundTag nbt) {
		super.writeSyncNBT(nbt);
		nbt.putBoolean("hasFuel", hasFuel);
		nbt.putBoolean("isActive", isActive());
	}

	@Override
	public void readSyncNBT(CompoundTag nbt) {
		super.readSyncNBT(nbt);
		hasFuel=nbt.getBoolean("hasFuel");
		super.setActive(nbt.getBoolean("isActive"));
	}

}
