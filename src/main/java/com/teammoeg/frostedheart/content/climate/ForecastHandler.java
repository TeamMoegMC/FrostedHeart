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

package com.teammoeg.frostedheart.content.climate;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.foods.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

public class ForecastHandler {
    public static void sendForecastMessages(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) event.player;
            boolean configAllows = FHConfig.COMMON.enablesTemperatureForecast.get();
            if (configAllows && ResearchDataAPI.getVariants(serverPlayer).getDouble("has_forecast")>0) {
                // Blizzard warning
                //float thisHour = WorldClimate.getTemp(serverPlayer.world);
                boolean thisHourB = WorldClimate.isBlizzard(serverPlayer.level());
                //float nextHour = WorldClimate.getFutureTemp(serverPlayer.world, 1);
                boolean nextHourB = WorldClimate.isFutureBlizzard(serverPlayer.level(), 1);
                if (!thisHourB) { // not in blizzard yet
                    if (nextHourB) {
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_warning")
                                .withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD), true);
                        // serverPlayer.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
                        // GuiUtils.translateMessage("forecast.blizzard_warning")));
                    }
                } else { // in blizzard now
                    if (!nextHourB) {
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_retreating")
                                .withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD), true);
                        // serverPlayer.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
                        // GuiUtils.translateMessage("forecast.blizzard_retreating")));
                    }
                }

                // Morning forecast wakeup time
                if (serverPlayer.level().getDayTime() % 24000 == 40) {
                    float morningTemp = Math.round(WorldClimate.getTemp(serverPlayer.level()) * 10) / 10.0F;
                    float noonTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 6) * 10) / 10.0F;
                    float nightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 12) * 10) / 10.0F;
                    float midnightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 18) * 10) / 10.0F;
                    float tomorrowMorningTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 1, 0) * 10) / 10.0F;
                    TemperatureDisplayHelper.sendTemperatureStatus(serverPlayer, "forecast.morning", false, morningTemp-10, noonTemp-10,
                            nightTemp-10, midnightTemp-10, tomorrowMorningTemp-10);
                    boolean snow = morningTemp < WorldTemperature.SNOW_TEMPERATURE
                            || noonTemp < WorldTemperature.SNOW_TEMPERATURE || nightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || midnightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowMorningTemp < WorldTemperature.SNOW_TEMPERATURE;
                    boolean blizzard = WorldClimate.isBlizzard(serverPlayer.level())
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 6)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 12)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 18)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 1, 0);
                    if (blizzard)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_today"), false);
                    else if (snow)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.snow_today"), false);
                    else
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.clear_today"), false);

                }

                // Night forecast bedtime
                if (serverPlayer.level().getDayTime() % 24000 == 12542) {
                    float nightTemp = Math.round(WorldClimate.getTemp(serverPlayer.level()) * 10) / 10.0F;
                    float midnightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 6) * 10) / 10.0F;
                    float tomorrowMorningTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 12) * 10)
                            / 10.0F;
                    float tomorrowNoonTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 0, 18) * 10)
                            / 10.0F;
                    float tomorrowNightTemp = Math.round(WorldClimate.getFutureTemp(serverPlayer.level(), 1, 0) * 10)
                            / 10.0F;
                    TemperatureDisplayHelper.sendTemperatureStatus(serverPlayer, "forecast.night", false, nightTemp-10, midnightTemp-10,
                            tomorrowMorningTemp-10, tomorrowNoonTemp-10, tomorrowNightTemp-10);
                    boolean snow = nightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || midnightTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowMorningTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowNoonTemp < WorldTemperature.SNOW_TEMPERATURE
                            || tomorrowNightTemp < WorldTemperature.SNOW_TEMPERATURE;
                    boolean blizzard = WorldClimate.isBlizzard(serverPlayer.level())
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 6)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 12)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 0, 18)
                            || WorldClimate.isFutureBlizzard(serverPlayer.level(), 1, 0);
                    if (blizzard)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.blizzard_tomorrow"), false);
                    else if (snow)
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.snow_tomorrow"), false);
                    else
                        serverPlayer.displayClientMessage(TranslateUtils.translateMessage("forecast.clear_tomorrow"), false);
                }
            }

            if (serverPlayer.level().getDayTime() % 24000 == 41 && FHConfig.COMMON.enableDailyKitchen.get())
                DailyKitchen.generateWantedFood(serverPlayer);//This is daily kitchen thing,not forecast message.
        }
    }
}
