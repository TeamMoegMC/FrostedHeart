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
 *
 */

package com.teammoeg.frostedheart.content.town.buildings.house;

/**
 * Pure numerical model for one town-house resident day.
 */
public final class HouseDailyModel {
    private HouseDailyModel() {
    }

    public static boolean isStructurallyWorkable(
            boolean baseBuildingWorkable,
            int area,
            int volume,
            int minimumArea,
            int minimumVolume
    ) {
        return baseBuildingWorkable && area >= minimumArea && volume >= minimumVolume;
    }

    public static boolean isBuildingWorkable(
            boolean baseBuildingWorkable,
            int area,
            int volume,
            boolean temperatureValid,
            int minimumArea,
            int minimumVolume
    ) {
        return isStructurallyWorkable(baseBuildingWorkable, area, volume, minimumArea, minimumVolume)
                && temperatureValid;
    }

    /**
     * Daily resident obligations depend on the house structure, not its
     * temperature. This deliberately differs from {@link #isBuildingWorkable}.
     */
    public static boolean shouldRunDailySettlement(
            boolean baseBuildingWorkable,
            int area,
            int volume,
            int minimumArea,
            int minimumVolume
    ) {
        return isStructurallyWorkable(baseBuildingWorkable, area, volume, minimumArea, minimumVolume);
    }

    public static double calculateFoodSatisfaction(double requiredFood, double consumedFood) {
        double safeRequiredFood = nonNegative(requiredFood);
        if (safeRequiredFood <= 0.0) {
            return 1.0;
        }
        return clamp(nonNegative(consumedFood) / safeRequiredFood, 0.0, 1.0);
    }

    public static double calculateNutritionQuality(
            double nutritionValue,
            double consumedFood,
            double nutritionReferencePerFoodUnit
    ) {
        double safeConsumedFood = nonNegative(consumedFood);
        double safeReference = nonNegative(nutritionReferencePerFoodUnit);
        if (safeConsumedFood <= 0.0 || safeReference <= 0.0) {
            return 0.0;
        }
        double nutritionPerFoodUnit = nonNegative(nutritionValue) / safeConsumedFood;
        return clamp(nutritionPerFoodUnit / safeReference, 0.0, 1.0);
    }

    public static double calculateNutritionRecoveryMultiplier(
            double nutritionQuality,
            double minimumNutritionRecoveryMultiplier
    ) {
        double quality = clampFinite(nutritionQuality, 0.0, 1.0, 0.0);
        double minimum = clampFinite(minimumNutritionRecoveryMultiplier, 0.0, 1.0, 0.0);
        return minimum + (1.0 - minimum) * quality;
    }

    public static double calculateComfortRating(
            double temperatureRating,
            double spaceRating,
            double decorationRating,
            double temperatureWeight,
            double spaceWeight,
            double decorationWeight
    ) {
        double temperature = clampFinite(temperatureRating, 0.0, 1.0, 0.0);
        double space = clampFinite(spaceRating, 0.0, 1.0, 0.0);
        double decoration = clampFinite(decorationRating, 0.0, 1.0, 0.0);
        double safeTemperatureWeight = nonNegative(temperatureWeight);
        double safeSpaceWeight = nonNegative(spaceWeight);
        double safeDecorationWeight = nonNegative(decorationWeight);
        double totalWeight = safeTemperatureWeight + safeSpaceWeight + safeDecorationWeight;
        if (totalWeight <= 0.0) {
            return (temperature + space + decoration) / 3.0;
        }
        return (temperature * safeTemperatureWeight
                + space * safeSpaceWeight
                + decoration * safeDecorationWeight) / totalWeight;
    }

    public static ResidentEffects calculateResidentEffects(
            double health,
            double mental,
            double foodSatisfaction,
            double nutritionRecoveryMultiplier,
            double temperatureRating,
            double comfortRating,
            double healthLossAtZeroFoodPerResidentDay,
            double mentalLossAtZeroFoodPerResidentDay,
            double maximumHealthRecoveryPerResidentDay,
            double maximumMentalRecoveryPerResidentDay
    ) {
        double safeHealth = clampFinite(health, 0.0, 100.0, 50.0);
        double safeMental = clampFinite(mental, 0.0, 100.0, 50.0);
        double food = clampFinite(foodSatisfaction, 0.0, 1.0, 0.0);
        double nutrition = clampFinite(nutritionRecoveryMultiplier, 0.0, 1.0, 0.0);
        double temperature = clampFinite(temperatureRating, 0.0, 1.0, 0.0);
        double comfort = clampFinite(comfortRating, 0.0, 1.0, 0.0);

        double healthPenalty = nonNegative(healthLossAtZeroFoodPerResidentDay) * (1.0 - food);
        double mentalPenalty = nonNegative(mentalLossAtZeroFoodPerResidentDay) * (1.0 - food);
        double healthRecovery = nonNegative(maximumHealthRecoveryPerResidentDay)
                * food * nutrition * temperature * (1.0 - safeHealth / 100.0);
        double mentalRecovery = nonNegative(maximumMentalRecoveryPerResidentDay)
                * food * nutrition * comfort * (1.0 - safeMental / 100.0);

        return new ResidentEffects(
                healthPenalty,
                healthRecovery,
                healthRecovery - healthPenalty,
                mentalPenalty,
                mentalRecovery,
                mentalRecovery - mentalPenalty
        );
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double clampFinite(double value, double minimum, double maximum, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return clamp(value, minimum, maximum);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record ResidentEffects(
            double healthPenalty,
            double healthRecovery,
            double healthDelta,
            double mentalPenalty,
            double mentalRecovery,
            double mentalDelta
    ) {
    }
}
