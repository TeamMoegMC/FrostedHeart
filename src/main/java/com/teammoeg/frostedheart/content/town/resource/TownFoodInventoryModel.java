/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure resident-food priority and fractional inventory-consumption model. */
public final class TownFoodInventoryModel {
    /** Matches {@link TeamTownResourceHolder#DELTA} without depending on a town instance. */
    public static final double RESOURCE_EPSILON = 1.0 / 8192.0;

    private static final Comparator<FoodStack> CONSUMPTION_ORDER =
            Comparator.comparingInt(FoodStack::foodLevel).reversed()
                    .thenComparing(Comparator.comparingDouble(FoodStack::nutritionPerFoodUnit).reversed())
                    .thenComparing(FoodStack::item);

    private TownFoodInventoryModel() {
    }

    /** Same nutrition-quality scalar used by game-side same-level ordering. */
    public static double nutritionPerFoodUnit(double nutritionPerItem, double foodUnitsPerItem) {
        if (!Double.isFinite(nutritionPerItem)
                || !Double.isFinite(foodUnitsPerItem)
                || nutritionPerItem <= 0.0
                || foodUnitsPerItem <= RESOURCE_EPSILON) {
            return 0.0;
        }
        return nutritionPerItem / foodUnitsPerItem;
    }

    /**
     * Consumes high food levels first, then the highest nutrition per food
     * unit within a level, exactly matching the game-side ordering rules.
     */
    public static Consumption consume(double requiredFoodUnits, List<FoodStack> inventory) {
        double remainingFood = nonNegative(requiredFoodUnits);
        List<FoodStack> ordered = inventory.stream()
                .filter(stack -> stack.amountItems() > RESOURCE_EPSILON)
                .sorted(CONSUMPTION_ORDER)
                .toList();
        List<FoodUse> uses = new ArrayList<>();
        double consumedFood = 0.0;
        double consumedNutrition = 0.0;
        for (FoodStack stack : ordered) {
            if (remainingFood <= RESOURCE_EPSILON) break;
            if (stack.foodUnitsPerItem() <= RESOURCE_EPSILON) continue;
            double availableFood = stack.amountItems() * stack.foodUnitsPerItem();
            double foodTaken = Math.min(remainingFood, availableFood);
            double itemAmount = foodTaken / stack.foodUnitsPerItem();
            double nutritionTaken = itemAmount * stack.nutritionPerItem();
            uses.add(new FoodUse(stack.item(), stack.foodLevel(), itemAmount, foodTaken, nutritionTaken));
            consumedFood += foodTaken;
            consumedNutrition += nutritionTaken;
            remainingFood -= foodTaken;
        }
        return new Consumption(
                nonNegative(requiredFoodUnits),
                consumedFood,
                consumedNutrition,
                Math.max(0.0, remainingFood),
                List.copyOf(uses));
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    public record FoodStack(
            String item,
            int foodLevel,
            double amountItems,
            double foodUnitsPerItem,
            double nutritionPerItem
    ) {
        public FoodStack {
            if (item == null || item.isBlank()) throw new IllegalArgumentException("item is required.");
            foodLevel = Math.max(0, Math.min(4, foodLevel));
            amountItems = nonNegative(amountItems);
            foodUnitsPerItem = nonNegative(foodUnitsPerItem);
            nutritionPerItem = nonNegative(nutritionPerItem);
        }

        public double nutritionPerFoodUnit() {
            return TownFoodInventoryModel.nutritionPerFoodUnit(nutritionPerItem, foodUnitsPerItem);
        }
    }

    public record FoodUse(
            String item,
            int foodLevel,
            double amountItems,
            double foodUnits,
            double nutrition
    ) {
    }

    public record Consumption(
            double requiredFoodUnits,
            double consumedFoodUnits,
            double consumedNutrition,
            double missingFoodUnits,
            List<FoodUse> uses
    ) {
    }
}
