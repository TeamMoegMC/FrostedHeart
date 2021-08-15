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

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.plant.HempBlock;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(HempBlock.class)
public class HempBlockMixin {

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void inject$tick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        ChunkData data = ChunkData.get(world, pos);
        float temp = data.getTemperatureAtBlock(pos);
        if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
            ci.cancel();
        }
    }
}
