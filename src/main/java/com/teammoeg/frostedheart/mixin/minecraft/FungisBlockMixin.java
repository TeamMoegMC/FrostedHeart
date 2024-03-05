/*
 * Copyright (c) 2024 TeamMoeg
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.FungusBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
/**
 * Make nether plants only grow in nether
 * <p>
 * */
@Mixin(FungusBlock.class)
public class FungisBlockMixin {
    @Inject(at = @At("HEAD"), method = "canGrow", cancellable = true)
    public void canGrow(IBlockReader worldIn, BlockPos pos, BlockState state, boolean isClient,
                        CallbackInfoReturnable<Boolean> cbi) {
        if (!FHUtils.canNetherTreeGrow(worldIn, pos)) {
        	cbi.setReturnValue(false);
        	cbi.cancel();
        }
    }

}
