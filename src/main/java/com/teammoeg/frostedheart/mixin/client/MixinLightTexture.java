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

package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.client.renderer.LightTexture;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    public MixinLightTexture() {
    }

    // TODO: Fix this
    /*
    @ModifyVariable(method = "updateLightTexture", index = 16, at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;apply(Lit/unimi/dsi/fastutil/floats/Float2FloatFunction;)V"))
    public float modifygamma(float val) {
        if (ClientUtils.applyspg)
            return ClientUtils.spgamma;
        return val;
    }
     */
}
