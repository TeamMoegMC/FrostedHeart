/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft.misc;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 优化：Blender.empty() 时 BlendDensity 是恒等函数，直接短路。
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$BlendDensity")
public abstract class BlendDensityFoldMixin {

    @Inject(method = "transform", at = @At("HEAD"), cancellable = true)
    private void fast$identityWhenNoBlending(DensityFunction.FunctionContext ctx, double value,
                                             CallbackInfoReturnable<Double> cir) {
        if (ctx.getBlender() == Blender.empty()) {
            cir.setReturnValue(value);
        }
    }
}
