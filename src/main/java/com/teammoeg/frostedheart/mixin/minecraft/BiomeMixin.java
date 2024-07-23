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

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
/**
 * Freeze water when cold
 * <p>
 * */
@Mixin(Biome.class)
public abstract class BiomeMixin {
    public boolean doesWaterFreeze(LevelReader worldIn, BlockPos water, boolean mustBeAtEdge) {
        if (this.getTemperature(water) >= 0.15F) {
            return false;
        }
        if (water.getY() >= 0 && water.getY() < 256 && worldIn.getBrightness(LightLayer.BLOCK, water) < 10 && ChunkHeatData.getTemperature(worldIn, water) < 0) {
            BlockState blockstate = worldIn.getBlockState(water);
            FluidState fluidstate = worldIn.getFluidState(water);
            if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                if (!mustBeAtEdge) {
                    return true;
                }

                boolean flag = worldIn.isWaterAt(water.west()) && worldIn.isWaterAt(water.east())
                        && worldIn.isWaterAt(water.north()) && worldIn.isWaterAt(water.south());
                return !flag;
            }
        }

        return false;
    }

    @Shadow
    public abstract float getTemperature(BlockPos pos);
}
