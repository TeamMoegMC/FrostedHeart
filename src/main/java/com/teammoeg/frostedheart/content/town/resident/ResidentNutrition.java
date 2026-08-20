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
        return severeChannelCount(SEVERE);
    }

    public int severeChannelCount(double severeReserve) {
        double threshold = nonNegative(severeReserve);
        int count = 0;
        if (fat < threshold) count++;
        if (carbohydrate < threshold) count++;
        if (protein < threshold) count++;
        if (vegetable < threshold) count++;
        return count;
    }

    /** Applies the daily reserve loss before the resident receives a meal. */
    public ResidentNutrition decay(double amount) {
        double loss = nonNegative(amount);
        return new ResidentNutrition(
                fat - loss, carbohydrate - loss, protein - loss, vegetable - loss);
    }

    public ResidentNutrition decay(double amount, double maximumReserve) {
        double loss = nonNegative(amount);
        return boundedTo(new ResidentNutrition(
                fat - loss, carbohydrate - loss, protein - loss, vegetable - loss), maximumReserve);
    }

    /**
	 * Applies an actual meal. Each intake value is hunger-weighted nutrition
	 * percentage-points; {@code referencePerChannel} grants one coverage unit.
     */
    public ResidentNutrition withMeal(
            NutritionIntake intake,
            double referencePerChannel,
            double gainAtReference,
            double maximumCoverage
    ) {
        return withMeal(intake, referencePerChannel, gainAtReference,
                maximumCoverage, MAXIMUM);
    }

    public ResidentNutrition withMeal(
            NutritionIntake intake,
            double referencePerChannel,
            double gainAtReference,
            double maximumCoverage,
            double maximumReserve
    ) {
        double reference = nonNegative(referencePerChannel);
        if (reference <= 0.0) return this;
        double gain = nonNegative(gainAtReference);
        double cap = nonNegative(maximumCoverage);
        return boundedTo(new ResidentNutrition(
                fat + gain * coverage(intake.fat(), reference, cap),
                carbohydrate + gain * coverage(intake.carbohydrate(), reference, cap),
                protein + gain * coverage(intake.protein(), reference, cap),
                vegetable + gain * coverage(intake.vegetable(), reference, cap)), maximumReserve);
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

    public double nutritionRisk(double healthyReserve) {
        return 1.0 - availability(minimum(), healthyReserve);
    }

    private static double availability(double value) {
        return Math.max(0.0, Math.min(1.0, bounded(value) / HEALTHY));
    }

    private static double availability(double value, double healthyReserve) {
        double healthy = Math.max(0.001, finiteOrZero(healthyReserve));
        return Math.max(0.0, Math.min(1.0, finiteOrZero(value) / healthy));
    }

    private static ResidentNutrition boundedTo(ResidentNutrition value, double maximumReserve) {
        double maximum = Math.max(0.0, Math.min(MAXIMUM, finiteOrZero(maximumReserve)));
        return new ResidentNutrition(
                Math.min(maximum, value.fat), Math.min(maximum, value.carbohydrate),
                Math.min(maximum, value.protein), Math.min(maximum, value.vegetable));
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

        public NutritionIntake {
            fat = nonNegative(fat);
            carbohydrate = nonNegative(carbohydrate);
            protein = nonNegative(protein);
            vegetable = nonNegative(vegetable);
        }

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
