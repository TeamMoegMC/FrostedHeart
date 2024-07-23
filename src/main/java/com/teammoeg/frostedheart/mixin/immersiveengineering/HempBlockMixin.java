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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;

import blusunrize.immersiveengineering.common.blocks.plant.EnumHempGrowth;
import blusunrize.immersiveengineering.common.blocks.plant.HempBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

@Mixin(HempBlock.class)
public class HempBlockMixin {

    private static EnumHempGrowth fh$getMaxGrowth(EnumHempGrowth current) {
        if (current == EnumHempGrowth.TOP0)
            return EnumHempGrowth.TOP0;
        else
            return EnumHempGrowth.BOTTOM4;
    }

    private float fh$getGrowthSpeed(Level world, BlockPos pos, BlockState state, int light) {
        float growth = 0.125f * (light - 11);
        if (world.canSeeSkyFromBelowWater(pos))
            growth += 2f;
        BlockState soil = world.getBlockState(pos.offset(0, -1, 0));
        if (soil.getBlock().isFertile(soil, world, pos.offset(0, -1, 0)))
            growth *= 1.5f;
        return 1f + growth;
    }

//    @Inject(method = "tick(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V", at = @At(value = "HEAD", args = {"log=true"}), cancellable = true)
//    public void inject$tick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
//        ChunkData data = ChunkData.get(world, pos);
//        float temp = data.getTemperatureAtBlock(pos);
//        if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
//            ci.cancel();
//        }
//    }

    /**
     * @author yuesha-yc
     * @reason Got some weird obfuscation issues if I use @Inject above
     */
    @Overwrite
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        int light = world.getMaxLocalRawBrightness(pos);
        if (light >= 12) {
            // FH Starts
            float temp = ChunkHeatData.getTemperature(world, pos);
            if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                return;
            }
            // FH Ends
            EnumHempGrowth growth = state.getValue(HempBlock.GROWTH);
            if (growth == EnumHempGrowth.TOP0)
                return;
            float speed = this.fh$getGrowthSpeed(world, pos, state, light);
            if (random.nextInt((int) (50F / speed) + 1) == 0) {
                if (fh$getMaxGrowth(growth) != growth) {
                    world.setBlockAndUpdate(pos, state.setValue(HempBlock.GROWTH, growth.next()));
                }
                if (growth == fh$getMaxGrowth(growth) && world.isEmptyBlock(pos.offset(0, 1, 0)))
                    world.setBlockAndUpdate(pos.offset(0, 1, 0), state.setValue(HempBlock.GROWTH, EnumHempGrowth.TOP0));
            }
        }
    }

}
