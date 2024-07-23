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

package com.teammoeg.frostedheart.mixin.rankine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cannolicatfish.rankine.blocks.GrassySoilBlock;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@Mixin(GrassySoilBlock.class)
public class MixinGrassySoilBlock extends GrassBlock {

    @Inject(at = @At("HEAD"), method = "isSnowyConditions", cancellable = true, remap = false)
    private static void isSnowyConditions(BlockState state, LevelReader worldReader, BlockPos pos, CallbackInfoReturnable<Boolean> cbi) {
        if (!FHUtils.canGrassSurvive(worldReader, pos))
            cbi.setReturnValue(false);
    }

    public MixinGrassySoilBlock(Properties properties) {
        super(properties);
    }
}
