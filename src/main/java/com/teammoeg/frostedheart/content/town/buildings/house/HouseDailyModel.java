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

import com.teammoeg.frostedheart.content.town.TownMathFunctions;

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

    /** Current bed-and-effective-floor-area capacity formula. */
    public static int calculateCapacity(
            double spaceRating,
            int floorAreaBlocks,
            double floorBlocksPerResident,
            int bedCount
    ) {
        double perResident = nonNegative(floorBlocksPerResident);
        if (perResident <= 0.0 || floorAreaBlocks <= 0 || bedCount <= 0) return 0;
        double effectiveArea = clampFinite(spaceRating, 0.0, 1.0, 0.0) * floorAreaBlocks;
        int spaceCapacity = (int) Math.floor(effectiveArea / perResident);
        return Math.min(Math.max(0, spaceCapacity), bedCount);
    }

    /** Computes every shared, non-resident-specific quantity in a house day. */
    public static SettlementReport evaluateSettlement(
            SettlementInput input,
            SettlementParameters parameters
    ) {
        int residentCount = Math.max(0, input.residentCount());
        double foodRequired = residentCount * nonNegative(parameters.foodPerResidentDay());
        double foodConsumed = nonNegative(input.foodConsumed());
        double foodSatisfaction = calculateFoodSatisfaction(foodRequired, foodConsumed);
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(
                input.temperatureCelsius(),
                parameters.comfortableTemperatureCelsius(),
                parameters.minimumTemperatureRating(),
                parameters.temperatureRatingSlopePerCelsius(),
                parameters.temperatureRatingHalfPointDifferenceCelsius());
        double spaceRating = TownMathFunctions.calculateSpaceRating(
                input.volumeBlocks(),
                input.areaBlocks(),
                parameters.spaceAreaCoefficient(),
                parameters.spaceHeightLogCoefficient(),
                parameters.spaceHeightLogOffset(),
                parameters.spaceResponseScale(),
                parameters.spaceResponseExponent());
        double comfortRating = calculateComfortRating(
                temperatureRating,
                spaceRating,
                input.decorationRating(),
                parameters.temperatureComfortWeight(),
                parameters.spaceComfortWeight(),
                parameters.decorationComfortWeight());
        return new SettlementReport(
                residentCount,
                foodRequired,
                foodConsumed,
                foodSatisfaction,
                input.temperatureCelsius(),
                temperatureRating,
                spaceRating,
                clampFinite(input.decorationRating(), 0.0, 1.0, 0.0),
                comfortRating);
    }

    public static ResidentEffects calculateResidentEffects(
            double health,
            double mental,
            double foodSatisfaction,
            double healthNutritionRecoveryMultiplier,
            double mentalNutritionRecoveryMultiplier,
            double effectiveTemperatureCelsius,
            double temperatureRating,
            double comfortRating,
            ResidentEffectParameters parameters
    ) {
        double safeHealth = clampFinite(health, 0.0, 100.0, 50.0);
        double safeMental = clampFinite(mental, 0.0, 100.0, 50.0);
        double food = clampFinite(foodSatisfaction, 0.0, 1.0, 0.0);
        double healthNutrition = clampFinite(
                healthNutritionRecoveryMultiplier, 0.0, 1.0, 0.0);
        double mentalNutrition = clampFinite(
                mentalNutritionRecoveryMultiplier, 0.0, 1.0, 0.0);
        double temperature = clampFinite(temperatureRating, 0.0, 1.0, 0.0);
        double comfort = clampFinite(comfortRating, 0.0, 1.0, 0.0);

        double foodStress = calculateFoodDeficitStress(
                food, parameters.foodDeficitPenaltyExponent());
        double temperatureStress = calculateTemperatureStress(
                effectiveTemperatureCelsius,
                parameters.minimumTemperatureCelsius(),
                parameters.maximumTemperatureCelsius(),
                parameters.temperatureFullStressDistanceCelsius(),
                parameters.temperatureStressPenaltyExponent());
        double healthFoodPenalty = nonNegative(parameters.healthLossAtZeroFoodPerResidentDay())
                * foodStress;
        double mentalFoodPenalty = nonNegative(parameters.mentalLossAtZeroFoodPerResidentDay())
                * foodStress;
        double healthTemperaturePenalty = nonNegative(
                parameters.healthLossAtFullTemperatureStressPerResidentDay()) * temperatureStress;
        double mentalTemperaturePenalty = nonNegative(
                parameters.mentalLossAtFullTemperatureStressPerResidentDay()) * temperatureStress;
        double healthPenalty = healthFoodPenalty + healthTemperaturePenalty;
        double mentalPenalty = mentalFoodPenalty + mentalTemperaturePenalty;
        double healthRecovery = nonNegative(parameters.maximumHealthRecoveryPerResidentDay())
                * food * healthNutrition * temperature * (1.0 - safeHealth / 100.0);
        double mentalRecovery = nonNegative(parameters.maximumMentalRecoveryPerResidentDay())
                * food * mentalNutrition * comfort * (1.0 - safeMental / 100.0);

        return new ResidentEffects(
                foodStress,
                temperatureStress,
                healthFoodPenalty,
                healthTemperaturePenalty,
                healthPenalty,
                healthRecovery,
                healthRecovery - healthPenalty,
                mentalFoodPenalty,
                mentalTemperaturePenalty,
                mentalPenalty,
                mentalRecovery,
                mentalRecovery - mentalPenalty
        );
    }

    /** Convex missing-food response, where 0 means fully fed and 1 means no food. */
    public static double calculateFoodDeficitStress(
            double foodSatisfaction,
            double penaltyExponent
    ) {
        double food = clampFinite(foodSatisfaction, 0.0, 1.0, 0.0);
        return Math.pow(1.0 - food, positiveExponent(penaltyExponent));
    }

    /**
     * Bounded cold/heat stress outside the inclusive safe temperature range.
     * Stress is zero inside the range and one at or beyond the configured full-stress distance.
     */
    public static double calculateTemperatureStress(
            double temperatureCelsius,
            double minimumTemperatureCelsius,
            double maximumTemperatureCelsius,
            double fullStressDistanceCelsius,
            double penaltyExponent
    ) {
        double lower = Math.min(minimumTemperatureCelsius, maximumTemperatureCelsius);
        double upper = Math.max(minimumTemperatureCelsius, maximumTemperatureCelsius);
        if (!Double.isFinite(lower) || !Double.isFinite(upper)
                || !Double.isFinite(temperatureCelsius)) {
            return 1.0;
        }
        double distance = Math.max(
                Math.max(lower - temperatureCelsius, temperatureCelsius - upper), 0.0);
        if (distance <= 0.0) return 0.0;
        double fullDistance = nonNegative(fullStressDistanceCelsius);
        if (fullDistance <= 0.0) return 1.0;
        double normalizedDistance = clamp(distance / fullDistance, 0.0, 1.0);
        return Math.pow(normalizedDistance, positiveExponent(penaltyExponent));
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double positiveExponent(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 1.0;
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
            double foodStress,
            double temperatureStress,
            double healthFoodPenalty,
            double healthTemperaturePenalty,
            double healthPenalty,
            double healthRecovery,
            double healthDelta,
            double mentalFoodPenalty,
            double mentalTemperaturePenalty,
            double mentalPenalty,
            double mentalRecovery,
            double mentalDelta
    ) {
    }

    public record ResidentEffectParameters(
            double foodDeficitPenaltyExponent,
            double healthLossAtZeroFoodPerResidentDay,
            double mentalLossAtZeroFoodPerResidentDay,
            double minimumTemperatureCelsius,
            double maximumTemperatureCelsius,
            double temperatureFullStressDistanceCelsius,
            double temperatureStressPenaltyExponent,
            double healthLossAtFullTemperatureStressPerResidentDay,
            double mentalLossAtFullTemperatureStressPerResidentDay,
            double maximumHealthRecoveryPerResidentDay,
            double maximumMentalRecoveryPerResidentDay
    ) {
    }

    public record SettlementInput(
            int residentCount,
            double foodConsumed,
            double temperatureCelsius,
            int areaBlocks,
            int volumeBlocks,
            double decorationRating
    ) {
    }

    public record SettlementParameters(
            double foodPerResidentDay,
            double comfortableTemperatureCelsius,
            double minimumTemperatureRating,
            double temperatureRatingSlopePerCelsius,
            double temperatureRatingHalfPointDifferenceCelsius,
            double spaceAreaCoefficient,
            double spaceHeightLogCoefficient,
            double spaceHeightLogOffset,
            double spaceResponseScale,
            double spaceResponseExponent,
            double temperatureComfortWeight,
            double spaceComfortWeight,
            double decorationComfortWeight
    ) {
    }

    public record SettlementReport(
            int residentCount,
            double foodRequired,
            double foodConsumed,
            double foodSatisfaction,
            double effectiveTemperature,
            double temperatureRating,
            double spaceRating,
            double decorationRating,
            double comfortRating
    ) {
    }
}
