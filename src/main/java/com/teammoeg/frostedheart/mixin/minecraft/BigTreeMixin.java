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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
/**
 * Reduces chance for big tree
 * <p>
 * */
@Mixin(AbstractMegaTreeGrower.class)
public abstract class BigTreeMixin extends AbstractTreeGrower {
    @Inject(at = @At("HEAD"), method = "growBigTree", cancellable = true)
    
    public void placeMega(ServerLevel p_235678_1_, ChunkGenerator p_235678_2_, BlockPos p_235678_3_, BlockState p_235678_4_, RandomSource p_235678_5_, int p_235678_6_, int p_235678_7_, CallbackInfoReturnable<Boolean> cr) {
        FHUtils.canBigTreeGenerate(p_235678_1_, p_235678_3_, p_235678_5_, cr);
    }
}
