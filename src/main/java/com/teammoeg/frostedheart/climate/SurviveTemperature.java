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

package com.teammoeg.frostedheart.climate;

import com.stereowalker.survive.Survive;
import com.stereowalker.survive.events.SurviveEvents;
import com.stereowalker.survive.util.TemperatureUtil;
import com.stereowalker.survive.util.data.BlockTemperatureData;
import com.stereowalker.unionlib.state.properties.UBlockStateProperties;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

/**
 * Re-write of survive's temperature methods since SurviveEvents.TempType is private.
 * Original Author: Stereowalker
 */
public class SurviveTemperature {
	public static float getBlockTemp(World world,BlockPos pos) {
	    float blockTemp = 0;
	    int rangeInBlocks = 2;
	    for (int x = -rangeInBlocks; x <= rangeInBlocks; x++) {
	        for (int y = -rangeInBlocks; y <= rangeInBlocks; y++) {
	            for (int z = -rangeInBlocks; z <= rangeInBlocks; z++) {
	                BlockPos heatSource = pos.add(x,y,z);
	                float blockLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.BLOCK).getLightFor(heatSource);
	                BlockState heatState = world.getBlockState(heatSource);
	                int sourceRange = Survive.blockTemperatureMap.containsKey(heatState.getBlock().getRegistryName()) ? Survive.blockTemperatureMap.get(heatState.getBlock().getRegistryName()).getRange() : 5;
	                if (pos.withinDistance(heatSource, sourceRange)) {
	                    blockTemp += blockLight / 500.0F;
	                    if (Survive.blockTemperatureMap.containsKey(heatState.getBlock().getRegistryName())) {
	                        BlockTemperatureData blockTemperatureData = Survive.blockTemperatureMap.get(heatState.getBlock().getRegistryName());
	                        if (blockTemperatureData.usesLitOrActiveProperty()) {
	                            boolean litOrActive = false;
	                            if (heatState.hasProperty(BlockStateProperties.LIT) && heatState.get(BlockStateProperties.LIT))
	                                litOrActive = true;
	                            if (heatState.hasProperty(UBlockStateProperties.ACTIVE) && heatState.get(UBlockStateProperties.ACTIVE))
	                                litOrActive = true;
	                            if (litOrActive) blockTemp += blockTemperatureData.getTemperatureModifier();
	                        } else
	                            blockTemp += blockTemperatureData.getTemperatureModifier();
	                        if (blockTemperatureData.usesLevelProperty()) {
	                            if (heatState.hasProperty(BlockStateProperties.LEVEL_0_15)) {
	                                blockTemp *= (heatState.get(BlockStateProperties.LEVEL_0_15) + 1) / 16;
	                            } else if (heatState.hasProperty(BlockStateProperties.LEVEL_0_8)) {
	                                blockTemp *= (heatState.get(BlockStateProperties.LEVEL_0_8) + 1) / 9;
	                            } else if (heatState.hasProperty(BlockStateProperties.LEVEL_1_8)) {
	                                blockTemp *= (heatState.get(BlockStateProperties.LEVEL_1_8)) / 8;
	                            } else if (heatState.hasProperty(BlockStateProperties.LEVEL_0_3)) {
	                                blockTemp *= (heatState.get(BlockStateProperties.LEVEL_0_3) + 1) / 4;
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	    return blockTemp;
	}

	public static final String DATA_ID=FHMain.MODID+"_pd";

	public static float getBodyTemperature(ServerPlayerEntity spe) {
		CompoundNBT nc=spe.getPersistentData().getCompound(DATA_ID);
		if(nc==null) {
			nc=new CompoundNBT();
			spe.getPersistentData().put(DATA_ID,nc);
			nc.putFloat("bodytemperature",0);
		}
		return nc.getFloat("bodytemperature");
	}

	public static void setBodyTemperature(ServerPlayerEntity spe,float val) {
		CompoundNBT nc=spe.getPersistentData().getCompound(DATA_ID);
		if(nc==null) {
			nc=new CompoundNBT();
			spe.getPersistentData().put(DATA_ID,nc);
		}
		nc.putFloat("bodytemperature",val);
	}
}
