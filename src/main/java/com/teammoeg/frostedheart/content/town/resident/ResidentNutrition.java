/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Four-channel, normalized nutrition reserve owned by one town resident. */
public record ResidentNutrition(
        double fat,
        double carbohydrate,
        double protein,
        double vegetable
) {
    public static final double MAXIMUM = 100.0;
    public static final double DEFAULT = 70.0;
    public static final double HEALTHY = 70.0;
    public static final double SEVERE = 20.0;

    public static final ResidentNutrition DEFAULT_VALUE = new ResidentNutrition(
            DEFAULT, DEFAULT, DEFAULT, DEFAULT);

    public static final Codec<ResidentNutrition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.optionalFieldOf("fat", DEFAULT).forGetter(ResidentNutrition::fat),
                    Codec.DOUBLE.optionalFieldOf("carbohydrate", DEFAULT).forGetter(ResidentNutrition::carbohydrate),
                    Codec.DOUBLE.optionalFieldOf("protein", DEFAULT).forGetter(ResidentNutrition::protein),
                    Codec.DOUBLE.optionalFieldOf("vegetable", DEFAULT).forGetter(ResidentNutrition::vegetable)
            ).apply(instance, ResidentNutrition::new));

    public ResidentNutrition {
        fat = bounded(fat);
        carbohydrate = bounded(carbohydrate);
        protein = bounded(protein);
        vegetable = bounded(vegetable);
    }

    public double minimum() {
        return Math.min(Math.min(fat, carbohydrate), Math.min(protein, vegetable));
    }

    public int severeChannelCount() {
        int count = 0;
        if (fat < SEVERE) count++;
        if (carbohydrate < SEVERE) count++;
        if (protein < SEVERE) count++;
        if (vegetable < SEVERE) count++;
        return count;
    }

    /** Applies the daily reserve loss before the resident receives a meal. */
    public ResidentNutrition decay(double amount) {
        double loss = nonNegative(amount);
        return new ResidentNutrition(
                fat - loss, carbohydrate - loss, protein - loss, vegetable - loss);
    }

    /**
     * Applies an actual meal. Each intake value is raw FH recipe nutrition;
     * {@code referencePerChannel} is the amount that grants one coverage unit.
     */
    public ResidentNutrition withMeal(
            NutritionIntake intake,
            double referencePerChannel,
            double gainAtReference,
            double maximumCoverage
    ) {
        double reference = nonNegative(referencePerChannel);
        if (reference <= 0.0) return this;
        double gain = nonNegative(gainAtReference);
        double cap = nonNegative(maximumCoverage);
        return new ResidentNutrition(
                fat + gain * coverage(intake.fat(), reference, cap),
                carbohydrate + gain * coverage(intake.carbohydrate(), reference, cap),
                protein + gain * coverage(intake.protein(), reference, cap),
                vegetable + gain * coverage(intake.vegetable(), reference, cap));
    }

    public double fatAvailability() {
        return availability(fat);
    }

    public double carbohydrateAvailability() {
        return availability(carbohydrate);
    }

    public double proteinAvailability() {
        return availability(protein);
    }

    public double vegetableAvailability() {
        return availability(vegetable);
    }

    public double nutritionRisk() {
        return 1.0 - availability(minimum());
    }

    /** Fat/protein only amplify recovery when the direct recovery nutrient exists. */
    public double mentalRecoveryMultiplier(double minimumMultiplier) {
        return recoveryMultiplier(
                carbohydrateAvailability(), fatAvailability(), proteinAvailability(),
                minimumMultiplier);
    }

    /** Fat/protein only amplify recovery when the direct recovery nutrient exists. */
    public double healthRecoveryMultiplier(double minimumMultiplier) {
        return recoveryMultiplier(
                vegetableAvailability(), fatAvailability(), proteinAvailability(),
                minimumMultiplier);
    }

    /**
     * Deficiency slows existing growth; reserves above the healthy line provide
     * at most a 25% bonus. This never creates growth where the age model has none.
     */
    public static double growthMultiplier(double value) {
        double safe = bounded(value);
        if (safe <= HEALTHY) {
            return 0.5 + 0.5 * safe / HEALTHY;
        }
        return 1.0 + 0.25 * (safe - HEALTHY) / (MAXIMUM - HEALTHY);
    }

    private static double recoveryMultiplier(
            double direct,
            double fat,
            double protein,
            double minimumMultiplier
    ) {
        double minimum = Math.max(0.0, Math.min(1.0, finiteOrZero(minimumMultiplier)));
        double support = (fat + protein) / 2.0;
        double supplied = 0.6 * direct + 0.4 * direct * support;
        return minimum + (1.0 - minimum) * supplied;
    }

    private static double availability(double value) {
        return Math.max(0.0, Math.min(1.0, bounded(value) / HEALTHY));
    }

    private static double coverage(double intake, double reference, double maximumCoverage) {
        return Math.max(0.0, Math.min(maximumCoverage, finiteOrZero(intake) / reference));
    }

    private static double bounded(double value) {
        return Math.max(0.0, Math.min(MAXIMUM, finiteOrZero(value)));
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finiteOrZero(value));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    public record NutritionIntake(
            double fat,
            double carbohydrate,
            double protein,
            double vegetable
    ) {
        public static final NutritionIntake ZERO = new NutritionIntake(0, 0, 0, 0);

        public NutritionIntake plus(NutritionIntake other) {
            return new NutritionIntake(
                    fat + other.fat,
                    carbohydrate + other.carbohydrate,
                    protein + other.protein,
                    vegetable + other.vegetable);
        }

        public NutritionIntake scale(double scale) {
            double safe = Math.max(0.0, Double.isFinite(scale) ? scale : 0.0);
            return new NutritionIntake(
                    fat * safe, carbohydrate * safe, protein * safe, vegetable * safe);
        }
    }
}
