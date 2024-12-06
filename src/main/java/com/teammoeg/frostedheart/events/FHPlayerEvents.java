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

package com.teammoeg.frostedheart.events;


import com.teammoeg.frostedheart.*;
import com.teammoeg.frostedheart.content.climate.ForecastHandler;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.climate.player.TemperatureUpdate;
import com.teammoeg.frostedheart.content.research.insight.InsightHandler;
import com.teammoeg.frostedheart.content.utility.transportation.MovementModificationHandler;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHPlayerEvents {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        FoodTemperatureHandler.onPlayerTick(event);
        ForecastHandler.sendForecastMessages(event);
        MovementModificationHandler.movementModifier(event);
        InsightHandler.onPlayerTick(event);
        TemperatureUpdate.updateTemperature(event);
        TemperatureUpdate.regulateTemperature(event);
    }

}
