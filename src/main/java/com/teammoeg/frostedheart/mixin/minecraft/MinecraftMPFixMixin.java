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

import net.minecraft.client.Minecraft;
/**
 * Fix minecraft tells multiplayer is disabled
 * For removal in 1.20+
 * */
@Mixin(Minecraft.class)
public class MinecraftMPFixMixin {
    @Inject(at = @At("HEAD"), method = "isChatEnabled", cancellable = true)

    public void allowChat(CallbackInfoReturnable<Boolean> callbackInfo) {
        callbackInfo.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "isMultiplayerEnabled", cancellable = true)
    public void allowMP(CallbackInfoReturnable<Boolean> callbackInfo) {
        callbackInfo.setReturnValue(true);
    }
}
