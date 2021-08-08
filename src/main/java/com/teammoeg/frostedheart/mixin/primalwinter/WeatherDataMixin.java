/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.mixin.primalwinter;

import com.alcatrazescapee.primalwinter.util.WeatherData;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherData.class)
public class WeatherDataMixin {

    /**
     * Enables weather cycle in Primal Winter for our needs
     */
    @Inject(method = "trySetEndlessStorm(Lnet/minecraft/world/server/ServerWorld;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;getGameRules()Lnet/minecraft/world/GameRules;"), cancellable = true, remap = false)
    private static void frostedheart$trySetEndlessStorm(ServerWorld world, CallbackInfo ci) {
        world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(true, world.getServer());
        world.setWeather(10000, Integer.MAX_VALUE, false, false);
        ci.cancel();
    }
}
