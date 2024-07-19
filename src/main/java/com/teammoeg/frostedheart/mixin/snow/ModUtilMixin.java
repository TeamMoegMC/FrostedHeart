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

package com.teammoeg.frostedheart.mixin.snow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import snownee.snow.ModUtil;
import snownee.snow.SnowCommonConfig;

@Mixin(ModUtil.class)
public class ModUtilMixin {
    /**
     * @author yuesha-yc
     * @reason snow melting when chunk temp < 0
     */
    @Overwrite(remap = false)
    public static boolean shouldMelt(World world, BlockPos pos) {
        if (SnowCommonConfig.snowNeverMelt)
            return false;
        if (world.getLightFor(LightType.BLOCK, pos) > 11)
            return true;
        if (ChunkHeatData.getTemperature(world, pos) > 0.5)
            return true;
        Biome biome = world.getBiome(pos);
        return ModUtil.snowMeltsInWarmBiomes(biome) && !ModUtil.isColdAt(world, biome, pos) && world.canSeeSky(pos);
    }
}
