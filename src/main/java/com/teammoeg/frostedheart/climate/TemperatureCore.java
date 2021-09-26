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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.data.BlockTempData;
import com.teammoeg.frostedheart.data.FHDataManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

/**
 * The core of our dynamic body & environment temperature system
 * @author yuesha-yc
 * @author khjxiaogu
 */
public class TemperatureCore {

    public static float getBlockTemp(World world, BlockPos pos) {
        float blockTemp = 0;
        int rangeInBlocks = 2;
        for (int x = -rangeInBlocks; x <= rangeInBlocks; x++) {
            for (int y = -rangeInBlocks; y <= rangeInBlocks; y++) {
                for (int z = -rangeInBlocks; z <= rangeInBlocks; z++) {
                    BlockPos heatSource = pos.add(x, y, z);
                    float blockLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.BLOCK).getLightFor(heatSource);
                    BlockState heatState = world.getBlockState(heatSource);
                    BlockTempData b = FHDataManager.getBlockData(heatState.getBlock());
                    if (b == null) {
                        blockTemp += blockLight / 500.0F;
                        continue;
                    }

                    if (pos.withinDistance(heatSource, b.getRange())) {
                        float cblocktemp = blockLight / 500.0F;
                        if (b.isLit()) {
                            boolean litOrActive = false;
                            if (heatState.hasProperty(BlockStateProperties.LIT) && heatState.get(BlockStateProperties.LIT))
                                litOrActive = true;
                            if (litOrActive) cblocktemp += b.getTemp();
                        } else
                            cblocktemp += b.getTemp();
                        if (b.isLevel()) {
                            if (heatState.hasProperty(BlockStateProperties.LEVEL_0_15)) {
                                cblocktemp *= (heatState.get(BlockStateProperties.LEVEL_0_15) + 1) / 16;
                            } else if (heatState.hasProperty(BlockStateProperties.LEVEL_0_8)) {
                                cblocktemp *= (heatState.get(BlockStateProperties.LEVEL_0_8) + 1) / 9;
                            } else if (heatState.hasProperty(BlockStateProperties.LEVEL_1_8)) {
                                cblocktemp *= (heatState.get(BlockStateProperties.LEVEL_1_8)) / 8;
                            } else if (heatState.hasProperty(BlockStateProperties.LEVEL_0_3)) {
                                cblocktemp *= (heatState.get(BlockStateProperties.LEVEL_0_3) + 1) / 4;
                            }
                        }
                        blockTemp += cblocktemp;
                    }
                }
            }
        }
        return blockTemp;
    }

    public static final String DATA_ID = FHMain.MODID + ":data";

    public static float getBodyTemperature(PlayerEntity spe) {
        CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
        if (nc == null)
            return 0;
        return nc.getFloat("bodytemperature");
    }

    public static float getLastTemperature(PlayerEntity spe) {
        CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
        if (nc == null)
            return 0;
        return nc.getFloat("lasttemperature");
    }

    public static float getEnvTemperature(PlayerEntity spe) {
        CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
        if (nc == null)
            return 0;
        return nc.getFloat("envtemperature");
    }

    public static CompoundNBT getFHData(PlayerEntity spe) {
        CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
        if (nc == null)
            return new CompoundNBT();
        return nc;
    }

    public static void setFHData(PlayerEntity spe, CompoundNBT nc) {
        spe.getPersistentData().put(DATA_ID, nc);
    }

    public static void setBodyTemperature(PlayerEntity spe, float val) {
        CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
        if (nc == null)
            nc = new CompoundNBT();
        nc.putFloat("bodytemperature", val);
        spe.getPersistentData().put(DATA_ID, nc);
    }

    public static void setTemperature(PlayerEntity spe, float body, float env) {
        CompoundNBT nc = spe.getPersistentData().getCompound(DATA_ID);
        if (nc == null)
            nc = new CompoundNBT();
        nc.putFloat("bodytemperature", body);
        nc.putFloat("envtemperature", env);
        nc.putFloat("deltatemperature", nc.getFloat("lasttemperature") - body);
        nc.putFloat("lasttemperature", body);
        spe.getPersistentData().put(DATA_ID, nc);
    }
}
