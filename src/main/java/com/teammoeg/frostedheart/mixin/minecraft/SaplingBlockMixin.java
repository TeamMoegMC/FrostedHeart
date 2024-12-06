/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Disable mushroom from spreading in low
 * */
@Mixin({MushroomBlock.class})
public abstract class SaplingBlockMixin extends BushBlock implements BonemealableBlock {

    public SaplingBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(at = @At("HEAD"), method = "randomTick", cancellable = true)
    public void fh$randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random, CallbackInfo cbi) {
        if (WorldTemperature.isBlizzardHarming(worldIn, pos)) {
            worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            cbi.cancel();
        } else if (!WorldTemperature.canTreeGrow(worldIn, pos, random))
            cbi.cancel();
    }

}
