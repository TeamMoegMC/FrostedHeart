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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
/**
 * Reduces chance for big tree
 * <p>
 * */
@Mixin(AbstractMegaTreeGrower.class)
public abstract class BigTreeMixin extends AbstractTreeGrower {
    @Inject(at = @At("HEAD"), method = "placeMega", cancellable = true)
    public void placeMega(ServerLevel pLevel, ChunkGenerator pGenerator, BlockPos pPos, BlockState pState, RandomSource pRandom, int pBranchX, int pBranchY,CallbackInfoReturnable<Boolean> cr) {
        WorldTemperature.canBigTreeGenerate(pLevel, pPos, pRandom, cr);
    }
}
