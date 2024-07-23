/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.primalwinter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.alcatrazescapee.primalwinter.Config;
import com.alcatrazescapee.primalwinter.util.WeatherData;
import com.teammoeg.frostedheart.content.climate.WorldClimate;

import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerLevel;

@Mixin(WeatherData.class)
public class WeatherDataMixin {

    /**
     * @author yuesha-yc
     * @reason Disable endless storm
     */
    @Overwrite(remap = false)
    public static void trySetEndlessStorm(ServerLevel world) {
        final WeatherData cap = world.getCapability(WeatherData.CAPABILITY).orElseThrow(() -> new IllegalStateException("Expected WeatherData to exist on World " + world.dimension() + " / " + world.dimensionType()));
        WeatherDataAccess dataAccess = (WeatherDataAccess) cap;
        if (!dataAccess.getAlreadySetWorldToWinter()) {
            dataAccess.setAlreadySetWorldToWinter(true);
            if (Config.COMMON.isWinterDimension(world.dimension().location())) {
                world.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(true, world.getServer());
                //world.setWeather(0, 64000, true, true);
                WorldClimate.getCapability(world).ifPresent(t->t.addInitTempEvent(world));
            }
        }
    }
}
