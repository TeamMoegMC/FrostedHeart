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

package com.teammoeg.frostedheart.events;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.foods.dailykitchen.DailyKitchen;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LivingEvents {
    @SubscribeEvent
    public static void startUsingItems(LivingEntityUseItemEvent.Start event) {
        FoodTemperatureHandler.checkFoodBeforeEating(event);
    }

    @SubscribeEvent
    public static void finishUsingItems(LivingEntityUseItemEvent.Finish event) {
        FoodTemperatureHandler.checkFoodAfterEating(event);
        DailyKitchen.tryGiveBenefits(event);
    }
}
