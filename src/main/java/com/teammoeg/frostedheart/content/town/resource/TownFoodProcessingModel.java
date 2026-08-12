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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scenario-only abstraction of {@code 1 raw meat -> 1 cooked meat}. */
public final class TownFoodProcessingModel {
    private TownFoodProcessingModel() {
    }

    /**
     * Uses finite daily capacity on the meats with the greatest food-unit gain
     * first. This is an explicit rational-player policy, not a simulated machine.
     */
    public static ProcessingResult process(
            Map<String, ? extends Number> rawInventory,
            double capacityItemsPerDay,
            List<MeatDefinition> meats
    ) {
        double remainingCapacity = Double.isInfinite(capacityItemsPerDay)
                ? Double.POSITIVE_INFINITY
                : nonNegative(capacityItemsPerDay);
        List<MeatDefinition> ordered = meats.stream()
                .sorted(Comparator.comparingDouble(MeatDefinition::foodGainPerItem).reversed()
                        .thenComparing(Comparator.comparingDouble(
                                MeatDefinition::cookedNutritionPerFoodUnit).reversed())
                        .thenComparing(MeatDefinition::rawItem))
                .toList();
        Map<String, Double> remainingRaw = new LinkedHashMap<>();
        Map<String, Double> cooked = new LinkedHashMap<>();
        double rawInput = 0.0;
        double processed = 0.0;
        for (MeatDefinition meat : ordered) {
            Number number = rawInventory.get(meat.rawItem());
            double amount = number == null ? 0.0 : nonNegative(number.doubleValue());
            rawInput += amount;
            double converted = Math.min(amount, remainingCapacity);
            double rawLeft = amount - converted;
            if (rawLeft > TownFoodInventoryModel.RESOURCE_EPSILON) {
                remainingRaw.put(meat.rawItem(), rawLeft);
            }
            if (converted > TownFoodInventoryModel.RESOURCE_EPSILON) {
                cooked.put(meat.cookedItem(), converted);
            }
            processed += converted;
            if (Double.isFinite(remainingCapacity)) remainingCapacity -= converted;
        }
        return new ProcessingResult(
                rawInput,
                processed,
                rawInput - processed,
                Map.copyOf(remainingRaw),
                Map.copyOf(cooked));
    }

    public static List<TownFoodInventoryModel.FoodStack> asFoodStacks(
            ProcessingResult result,
            List<MeatDefinition> meats
    ) {
        List<TownFoodInventoryModel.FoodStack> stacks = new ArrayList<>();
        for (MeatDefinition meat : meats) {
            double rawAmount = result.remainingRaw().getOrDefault(meat.rawItem(), 0.0);
            if (rawAmount > TownFoodInventoryModel.RESOURCE_EPSILON) {
                stacks.add(new TownFoodInventoryModel.FoodStack(
                        meat.rawItem(), meat.rawFoodLevel(), rawAmount,
                        meat.rawFoodUnitsPerItem(), meat.rawNutritionPerItem()));
            }
            double cookedAmount = result.cooked().getOrDefault(meat.cookedItem(), 0.0);
            if (cookedAmount > TownFoodInventoryModel.RESOURCE_EPSILON) {
                stacks.add(new TownFoodInventoryModel.FoodStack(
                        meat.cookedItem(), meat.cookedFoodLevel(), cookedAmount,
                        meat.cookedFoodUnitsPerItem(), meat.cookedNutritionPerItem()));
            }
        }
        return List.copyOf(stacks);
    }

    public static double totalFoodUnits(ProcessingResult result, List<MeatDefinition> meats) {
        return asFoodStacks(result, meats).stream()
                .mapToDouble(stack -> stack.amountItems() * stack.foodUnitsPerItem())
                .sum();
    }

    public static double totalNutrition(ProcessingResult result, List<MeatDefinition> meats) {
        return asFoodStacks(result, meats).stream()
                .mapToDouble(stack -> stack.amountItems() * stack.nutritionPerItem())
                .sum();
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    public record MeatDefinition(
            String rawItem,
            String cookedItem,
            double rawFoodUnitsPerItem,
            double cookedFoodUnitsPerItem,
            double rawNutritionPerItem,
            double cookedNutritionPerItem,
            int rawFoodLevel,
            int cookedFoodLevel
    ) {
        public MeatDefinition {
            if (rawItem == null || rawItem.isBlank()) throw new IllegalArgumentException("rawItem is required.");
            if (cookedItem == null || cookedItem.isBlank()) throw new IllegalArgumentException("cookedItem is required.");
            rawFoodUnitsPerItem = nonNegative(rawFoodUnitsPerItem);
            cookedFoodUnitsPerItem = nonNegative(cookedFoodUnitsPerItem);
            rawNutritionPerItem = nonNegative(rawNutritionPerItem);
            cookedNutritionPerItem = nonNegative(cookedNutritionPerItem);
            rawFoodLevel = Math.max(0, Math.min(4, rawFoodLevel));
            cookedFoodLevel = Math.max(0, Math.min(4, cookedFoodLevel));
        }

        public double foodGainPerItem() {
            return cookedFoodUnitsPerItem - rawFoodUnitsPerItem;
        }

        public double cookedNutritionPerFoodUnit() {
            return TownFoodInventoryModel.nutritionPerFoodUnit(
                    cookedNutritionPerItem, cookedFoodUnitsPerItem);
        }
    }

    public record ProcessingResult(
            double rawInputItems,
            double processedItems,
            double remainingRawItems,
            Map<String, Double> remainingRaw,
            Map<String, Double> cooked
    ) {
    }
}
