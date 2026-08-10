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

import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Shared resident-food nutrition calculations for consumption ordering and
 * house settlement.
 */
public final class TownFoodNutritionModel {
    static final Comparator<FoodCandidate> HIGHEST_NUTRITION_FIRST =
            Comparator.comparingDouble(FoodCandidate::nutritionPerFoodUnit)
                    .reversed()
                    .thenComparing(FoodCandidate::stableKey);

    private TownFoodNutritionModel() {
    }

    /**
     * Uses the same scalar nutrition value as house recovery: the sum of the
     * four FH nutrition channels divided by four.
     */
    public static double getNutritionPerItem(
            ItemStackResourceKey item,
            List<NutritionRecipe> recipes
    ) {
        double nutrition = 0.0;
        for (NutritionRecipe recipe : recipes) {
            if (recipe.conform(item.getItem())) {
                nutrition += recipe.getNutrition().getNutritionValue() / 4.0;
            }
        }
        return Double.isFinite(nutrition) ? Math.max(0.0, nutrition) : 0.0;
    }

    /**
     * Nutrition quality used for same-level ordering. Dividing by food
     * resource amount makes this exactly the per-food-unit quantity used by
     * {@code HouseDailyModel.calculateNutritionQuality}.
     */
    public static double calculateNutritionPerFoodUnit(
            double nutritionPerItem,
            double foodResourceAmount
    ) {
        if (!Double.isFinite(nutritionPerItem)
                || !Double.isFinite(foodResourceAmount)
                || nutritionPerItem <= 0.0
                || foodResourceAmount <= TeamTownResourceHolder.DELTA) {
            return 0.0;
        }
        return nutritionPerItem / foodResourceAmount;
    }

    public static List<ItemStackResourceKey> orderByNutritionQuality(
            Collection<ItemStackResourceKey> items,
            ItemResourceAttribute foodAttribute,
            List<NutritionRecipe> recipes
    ) {
        return items.stream()
                .map(item -> new FoodCandidate(
                        item,
                        calculateNutritionPerFoodUnit(
                                getNutritionPerItem(item, recipes),
                                TeamTownResourceHolder.getResourceAmount(item, foodAttribute)),
                        stableKey(item)))
                .sorted(HIGHEST_NUTRITION_FIRST)
                .map(FoodCandidate::item)
                .toList();
    }

    private static String stableKey(ItemStackResourceKey item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        String tag = item.getCompoundTag() == null ? "" : item.getCompoundTag().toString();
        return itemId + "|" + tag;
    }

    record FoodCandidate(
            ItemStackResourceKey item,
            double nutritionPerFoodUnit,
            String stableKey
    ) {
    }
}
