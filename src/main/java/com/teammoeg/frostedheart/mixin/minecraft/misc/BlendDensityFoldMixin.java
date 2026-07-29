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
