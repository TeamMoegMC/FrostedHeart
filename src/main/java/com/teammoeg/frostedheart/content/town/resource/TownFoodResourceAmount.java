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
 */

package com.teammoeg.frostedheart.content.town.resource;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Converts edible items into resident food resource units.
 * <p>
 * Minecraft stores an item's hunger restoration and a saturation modifier.
 * The nominal saturation restored by an item is
 * {@code hunger * saturationModifier * 2}. One resident food unit therefore
 * represents one point of nominal hunger-or-saturation restoration:
 * {@code hunger + nominalSaturation}.
 */
public final class TownFoodResourceAmount {
    private TownFoodResourceAmount() {
    }

    /**
     * Returns the food resource amount for the stack, or {@code fallback} when
     * the item has no positive, finite vanilla food value.
     */
    public static double fromItemStack(ItemStack stack, double fallback) {
        FoodProperties food = stack.getFoodProperties(null);
        if (food == null) {
            return fallback;
        }
        double amount = fromFoodProperties(food.getNutrition(), food.getSaturationModifier());
        return Double.isFinite(amount) && amount > 0.0 ? amount : fallback;
    }

    /**
     * Pure numerical form used by the town model and future simulator.
     */
    public static double fromFoodProperties(int hunger, float saturationModifier) {
        double nonNegativeHunger = Math.max(0, hunger);
        double nominalSaturation = nonNegativeHunger * Math.max(0.0f, saturationModifier) * 2.0;
        return nonNegativeHunger + nominalSaturation;
    }
}
