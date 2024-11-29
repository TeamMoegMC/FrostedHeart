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

package com.teammoeg.frostedheart.content.agriculture;

import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

public class FHCropBlock extends CropBlock {
    private int growTemperature;

    public FHCropBlock(int growTemperature, Properties builder) {
        super(builder);
        this.growTemperature = growTemperature;
    }

    @Override
    public boolean isBonemealSuccess(Level worldIn, RandomSource rand, BlockPos pos, BlockState state) {
        float temp = WorldTemperature.get(worldIn, pos);
        return temp >= growTemperature;
    }


    public int getGrowTemperature() {
        return this.growTemperature;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
        if (!worldIn.isAreaLoaded(pos, 1))
            return; // Forge: prevent loading unloaded chunks when checking neighbor's light
        if (worldIn.getRawBrightness(pos, 0) >= 9) {
            int i = this.getAge(state);
            float temp = WorldTemperature.get(worldIn, pos);
            boolean bz = WorldClimate.isBlizzard(worldIn);
            if (temp < growTemperature || bz) {
                if ((bz || temp < growTemperature - 10) && worldIn.getRandom().nextInt(3) == 0) {
                    worldIn.setBlock(pos, this.defaultBlockState(), 2);
                }
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (worldIn.getRandom().nextInt(3) == 0) {
                    worldIn.setBlock(pos, this.defaultBlockState(), 2);
                }
            } else if (i < this.getMaxAge()) {
                float f = getGrowthSpeed(this, worldIn, pos);
                if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos, state, random.nextInt((int) (25.0F / f) + 1) == 0)) {
                    worldIn.setBlock(pos, this.getStateForAge(i + 1), 2);
                    net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state);
                }
            }
        }
    }
}
