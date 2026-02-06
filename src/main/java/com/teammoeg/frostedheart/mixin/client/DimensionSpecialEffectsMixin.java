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

package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
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
