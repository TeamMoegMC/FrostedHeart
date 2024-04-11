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

import net.minecraft.block.BlockState;
import net.minecraft.block.trees.BigTree;
import net.minecraft.block.trees.Tree;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorld;
/**
 * Reduces chance for big tree
 * <p>
 * */
@Mixin(BigTree.class)
public abstract class BigTreeMixin extends Tree {
    @Inject(at = @At("HEAD"), method = "growBigTree", cancellable = true)
    public void placeMega(ServerWorld p_235678_1_, ChunkGenerator p_235678_2_, BlockPos p_235678_3_, BlockState p_235678_4_, Random p_235678_5_, int p_235678_6_, int p_235678_7_, CallbackInfoReturnable<Boolean> cr) {
        FHUtils.canBigTreeGenerate(p_235678_1_, p_235678_3_, p_235678_5_, cr);
    }
}
