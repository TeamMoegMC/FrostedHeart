/*
MIT License

Copyright (c) [2019] [Alex O'Neill (AlcatrazEscapee)]
Copyright (c) [2024] [TeamMoeg]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.FHConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author alcatrazEscapee
 *
 * License: MIT
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsMixin
{
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "getSunriseColor", at = @At("RETURN"), cancellable = true)
    private void noSunriseColor(float skyAngle, float tickDelta, CallbackInfoReturnable<float[]> cir)
    {
        final float[] original = cir.getReturnValue();
        final Level level = Minecraft.getInstance().level;
        if (original != null && FHConfig.CLIENT.skyRenderChanges.get() && level != null)
        {
            final BlockPos pos = Minecraft.getInstance().gameRenderer.getMainCamera().getBlockPosition();
            final Holder<Biome> biome = level.getBiome(pos);
            if (biome.value().coldEnoughToSnow(pos) && ((Object) this) instanceof DimensionSpecialEffects.OverworldEffects)
            {
                cir.setReturnValue(null);
            }
        }
    }
}
