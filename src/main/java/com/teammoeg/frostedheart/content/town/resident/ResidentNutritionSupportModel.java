/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure formulas for resident nutrition satisfaction, support, recovery, and explanations.
 *
 * <p>This class is the shared numeric authority for gameplay and simulation. Resident reserves
 * become {@link Satisfaction} values and configurable normalized weight rows convert satisfaction
 * to four current support scores. Attribute transitions are owned by
 * {@link ResidentAttributeModel}. Methods sanitize non-finite inputs and keep public
 * satisfaction/support values in {@code 0..1}.</p>
 */
public final class ResidentNutritionSupportModel {
    private static final double EPSILON = 1.0e-9;

    public static final WeightRow DEFAULT_HEALTH = new WeightRow(0.50, 0.10, 0.30, 0.10);
    public static final WeightRow DEFAULT_MENTAL = new WeightRow(0.10, 0.30, 0.20, 0.40);
    public static final WeightRow DEFAULT_STRENGTH = new WeightRow(0.75, 0.08, 0.03, 0.14);
    public static final WeightRow DEFAULT_INTELLIGENCE = new WeightRow(0.05, 0.30, 0.40, 0.25);
    public static final Weights DEFAULT_WEIGHTS = new Weights(
            DEFAULT_HEALTH, DEFAULT_MENTAL, DEFAULT_STRENGTH, DEFAULT_INTELLIGENCE);

    private ResidentNutritionSupportModel() {
    }

    /**
     * Converts resident reserves to current nutrient satisfaction.
     *
     * <p>For each channel, {@code satisfaction = clamp(reserve / healthyReserve, 0, 1)}. Reserves
     * at or above the healthy line therefore provide full support without additional benefit.</p>
     *
     * @param nutrition resident four-channel reserve state
     * @param healthyReserve reserve value treated as fully satisfied, normally {@code 70}
     * @return satisfaction ordered as protein, fat, vegetable, carbohydrate
     */
    public static Satisfaction satisfaction(ResidentNutrition nutrition, double healthyReserve) {
        ResidentNutrition safe = nutrition == null ? ResidentNutrition.DEFAULT_VALUE : nutrition;
        double healthy = Math.max(EPSILON, finiteOr(healthyReserve, ResidentNutrition.HEALTHY));
        return new Satisfaction(
                unit(safe.protein() / healthy),
                unit(safe.fat() / healthy),
                unit(safe.vegetable() / healthy),
                unit(safe.carbohydrate() / healthy));
    }

    /**
     * Applies the normalized four-by-four weight matrix to nutrient satisfaction.
     *
     * @param satisfaction current nutrient satisfaction
     * @param weights configurable health, mental, strength, and intelligence rows
     * @return four support scores in {@code 0..1}
     */
    public static Supports supports(Satisfaction satisfaction, Weights weights) {
        Satisfaction safe = satisfaction == null ? Satisfaction.FULL : satisfaction;
        Weights normalized = (weights == null ? DEFAULT_WEIGHTS : weights).normalized();
        return new Supports(
                normalized.health().apply(safe),
                normalized.mental().apply(safe),
                normalized.strength().apply(safe),
                normalized.intelligence().apply(safe));
    }

    /**
     * @param healthSupport current health nutrition support
     * @return health recovery multiplier {@code 0.25 + 0.75 * support}
     */
    public static double healthRecoveryMultiplier(double healthSupport) {
        return 0.25 + 0.75 * unit(healthSupport);
    }

    /**
     * @param mentalSupport current mental nutrition support
     * @return mental recovery multiplier {@code 0.35 + 0.65 * support}
     */
    public static double mentalRecoveryMultiplier(double mentalSupport) {
        return 0.35 + 0.65 * unit(mentalSupport);
    }

    /**
     * Selects the largest configured {@code weight * (1 - satisfaction)} terms.
     *
     * @param satisfaction nutrient satisfaction to explain
     * @param weights weight row for the explained outcome
     * @param maximum maximum number of channels to return
     * @return limiting channels ordered by contribution, with deterministic name tie-breaking
     */
    public static List<Nutrient> limitingNutrients(
            Satisfaction satisfaction,
            WeightRow weights,
            int maximum
    ) {
        Satisfaction safe = satisfaction == null ? Satisfaction.FULL : satisfaction;
        WeightRow row = (weights == null ? DEFAULT_HEALTH : weights).normalized(DEFAULT_HEALTH);
        List<NutrientDeficit> deficits = new ArrayList<>(List.of(
                new NutrientDeficit(Nutrient.PROTEIN, row.protein * (1.0 - safe.protein)),
                new NutrientDeficit(Nutrient.FAT, row.fat * (1.0 - safe.fat)),
                new NutrientDeficit(Nutrient.VEGETABLE, row.vegetable * (1.0 - safe.vegetable)),
                new NutrientDeficit(Nutrient.CARBOHYDRATE,
                        row.carbohydrate * (1.0 - safe.carbohydrate))));
        deficits.removeIf(deficit -> deficit.amount <= EPSILON);
        deficits.sort(Comparator.comparingDouble(NutrientDeficit::amount).reversed()
                .thenComparing(deficit -> deficit.nutrient.name()));
        return deficits.stream().limit(Math.max(0, maximum))
                .map(NutrientDeficit::nutrient).toList();
    }

    private static double unit(double value) {
        return Math.max(0.0, Math.min(1.0, finiteOr(value, 0.0)));
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finiteOr(value, 0.0));
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    /** Four current nutrient satisfaction values in protein, fat, vegetable, carbohydrate order. */
    public record Satisfaction(double protein, double fat, double vegetable, double carbohydrate) {
        public static final Satisfaction FULL = new Satisfaction(1.0, 1.0, 1.0, 1.0);
        public static final Codec<Satisfaction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("protein", 1.0).forGetter(Satisfaction::protein),
                Codec.DOUBLE.optionalFieldOf("fat", 1.0).forGetter(Satisfaction::fat),
                Codec.DOUBLE.optionalFieldOf("vegetable", 1.0).forGetter(Satisfaction::vegetable),
                Codec.DOUBLE.optionalFieldOf("carbohydrate", 1.0).forGetter(Satisfaction::carbohydrate)
        ).apply(instance, Satisfaction::new));

        public Satisfaction {
            protein = unit(protein);
            fat = unit(fat);
            vegetable = unit(vegetable);
            carbohydrate = unit(carbohydrate);
        }

    }

    /** Configurable weight row ordered as protein, fat, vegetable, carbohydrate. */
    public record WeightRow(double protein, double fat, double vegetable, double carbohydrate) {
        /**
         * Clamps negative weights to zero and normalizes the row to a sum of one.
         * An all-zero row uses the supplied default row.
         *
         * @param fallback row used when every configured weight is zero
         * @return a non-negative row whose channels sum to one
         */
        public WeightRow normalized(WeightRow fallback) {
            double p = nonNegative(protein);
            double f = nonNegative(fat);
            double v = nonNegative(vegetable);
            double c = nonNegative(carbohydrate);
            double total = p + f + v + c;
            if (total <= EPSILON) return fallback.normalized(DEFAULT_HEALTH);
            return new WeightRow(p / total, f / total, v / total, c / total);
        }

        /**
         * @param satisfaction four nutrient satisfaction values
         * @return weighted support for the supplied satisfaction, clamped to {@code 0..1}
         */
        public double apply(Satisfaction satisfaction) {
            return unit(protein * satisfaction.protein + fat * satisfaction.fat
                    + vegetable * satisfaction.vegetable
                    + carbohydrate * satisfaction.carbohydrate);
        }
    }

    /** Configurable four-by-four nutrition support matrix. */
    public record Weights(WeightRow health, WeightRow mental, WeightRow strength, WeightRow intelligence) {
        public Weights normalized() {
            return new Weights(
                    safe(health, DEFAULT_HEALTH).normalized(DEFAULT_HEALTH),
                    safe(mental, DEFAULT_MENTAL).normalized(DEFAULT_MENTAL),
                    safe(strength, DEFAULT_STRENGTH).normalized(DEFAULT_STRENGTH),
                    safe(intelligence, DEFAULT_INTELLIGENCE).normalized(DEFAULT_INTELLIGENCE));
        }

        private static WeightRow safe(WeightRow row, WeightRow fallback) {
            return row == null ? fallback : row;
        }
    }

    /** Current support scores for the four resident outcomes. */
    public record Supports(double health, double mental, double strength, double intelligence) {
        public static final Codec<Supports> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("health", 1.0).forGetter(Supports::health),
                Codec.DOUBLE.optionalFieldOf("mental", 1.0).forGetter(Supports::mental),
                Codec.DOUBLE.optionalFieldOf("strength", 1.0).forGetter(Supports::strength),
                Codec.DOUBLE.optionalFieldOf("intelligence", 1.0).forGetter(Supports::intelligence)
        ).apply(instance, Supports::new));

        public Supports {
            health = unit(health);
            mental = unit(mental);
            strength = unit(strength);
            intelligence = unit(intelligence);
        }
    }

    public enum Nutrient {
        PROTEIN("gui.frostedheart.town.nutrition_protein"),
        FAT("gui.frostedheart.town.nutrition_fat"),
        VEGETABLE("gui.frostedheart.town.nutrition_vegetable"),
        CARBOHYDRATE("gui.frostedheart.town.nutrition_carbohydrate");

        private final String translationKey;

        Nutrient(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    private record NutrientDeficit(Nutrient nutrient, double amount) {
    }
}
