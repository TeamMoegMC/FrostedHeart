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

import com.teammoeg.frostedheart.content.climate.render.sky.CalculatedSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes the fog colour from the calculated sky outside fluids.
 * It replaces vanilla's colour from sky, biome, sunrise, and weather, and vanilla still darkens it afterwards
 * for blindness, darkness, the void, and bosses. ViewportEvent.ComputeFogColor handlers such as the snow fog
 * of FogModification run after it.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/FogRenderer;getPriorityFogFunction(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/FogRenderer$MobEffectFogFunction;"), require = 0)
    private static void frostedheart$useCalculatedSkyFogColor(Camera camera, float partialTick, ClientLevel level,
            int renderDistanceChunks, float bossColorModifier, CallbackInfo callbackInfo) {
        if (camera.getFluidInCamera() != FogType.NONE) {
            return;
        }
        float[] color = CalculatedSkyRenderer.fogColor(level, camera, partialTick);
        if (color != null) {
            fogRed = color[0];
            fogGreen = color[1];
            fogBlue = color[2];
        }
    }
}
