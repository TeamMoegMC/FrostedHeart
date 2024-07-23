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

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.block.AbstractBlock.Properties;

/**
 * Disable any tree from growing when blizzard
 * */
@Mixin({SaplingBlock.class, MushroomBlock.class})
public abstract class SaplingBlockMixin extends BushBlock implements IGrowable {

    public SaplingBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(at = @At("HEAD"), method = "randomTick", cancellable = true)
    public void fh$randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random, CallbackInfo cbi) {
        if (FHUtils.isBlizzardHarming(worldIn, pos)) {
            worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            cbi.cancel();
        } else if (!FHUtils.canTreeGrow(worldIn, pos, random))
            cbi.cancel();
    }

}
