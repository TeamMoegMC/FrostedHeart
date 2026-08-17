/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;

import java.util.Arrays;
import java.util.Collection;

/** Daily town-wide nutrition averages and low-tail values for history charts. */
public record TownNutritionHistory(
        boolean available,
        double averageFat,
        double p10Fat,
        double averageCarbohydrate,
        double p10Carbohydrate,
        double averageProtein,
        double p10Protein,
        double averageVegetable,
        double p10Vegetable
) {
    public static final TownNutritionHistory EMPTY = new TownNutritionHistory(
            false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    public static final Codec<TownNutritionHistory> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("available", false)
                            .forGetter(TownNutritionHistory::available),
                    Codec.DOUBLE.optionalFieldOf("averageFat", 0.0)
                            .forGetter(TownNutritionHistory::averageFat),
                    Codec.DOUBLE.optionalFieldOf("p10Fat", 0.0)
                            .forGetter(TownNutritionHistory::p10Fat),
                    Codec.DOUBLE.optionalFieldOf("averageCarbohydrate", 0.0)
                            .forGetter(TownNutritionHistory::averageCarbohydrate),
                    Codec.DOUBLE.optionalFieldOf("p10Carbohydrate", 0.0)
                            .forGetter(TownNutritionHistory::p10Carbohydrate),
                    Codec.DOUBLE.optionalFieldOf("averageProtein", 0.0)
                            .forGetter(TownNutritionHistory::averageProtein),
                    Codec.DOUBLE.optionalFieldOf("p10Protein", 0.0)
                            .forGetter(TownNutritionHistory::p10Protein),
                    Codec.DOUBLE.optionalFieldOf("averageVegetable", 0.0)
                            .forGetter(TownNutritionHistory::averageVegetable),
                    Codec.DOUBLE.optionalFieldOf("p10Vegetable", 0.0)
                            .forGetter(TownNutritionHistory::p10Vegetable)
            ).apply(instance, TownNutritionHistory::new));

    public TownNutritionHistory {
        averageFat = bounded(averageFat);
        p10Fat = bounded(p10Fat);
        averageCarbohydrate = bounded(averageCarbohydrate);
        p10Carbohydrate = bounded(p10Carbohydrate);
        averageProtein = bounded(averageProtein);
        p10Protein = bounded(p10Protein);
        averageVegetable = bounded(averageVegetable);
        p10Vegetable = bounded(p10Vegetable);
    }

    /** Captures one post-settlement nutrition sample, including an empty town as zero. */
    public static TownNutritionHistory capture(Collection<ResidentNutrition> residents) {
        if (residents == null || residents.isEmpty()) {
            return new TownNutritionHistory(true,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double[] fat = new double[residents.size()];
        double[] carbohydrate = new double[residents.size()];
        double[] protein = new double[residents.size()];
        double[] vegetable = new double[residents.size()];
        int index = 0;
        for (ResidentNutrition value : residents) {
            ResidentNutrition nutrition = value == null
                    ? ResidentNutrition.DEFAULT_VALUE : value;
            fat[index] = nutrition.fat();
            carbohydrate[index] = nutrition.carbohydrate();
            protein[index] = nutrition.protein();
            vegetable[index] = nutrition.vegetable();
            index++;
        }
        Arrays.sort(fat);
        Arrays.sort(carbohydrate);
        Arrays.sort(protein);
        Arrays.sort(vegetable);
        return new TownNutritionHistory(true,
                average(fat), percentile(fat, 0.10),
                average(carbohydrate), percentile(carbohydrate, 0.10),
                average(protein), percentile(protein, 0.10),
                average(vegetable), percentile(vegetable, 0.10));
    }

    private static double average(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private static double percentile(double[] sorted, double probability) {
        double position = probability * Math.max(0, sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) return sorted[lower];
        double weight = position - lower;
        return sorted[lower] * (1.0 - weight) + sorted[upper] * weight;
    }

    private static double bounded(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(ResidentNutrition.MAXIMUM, value));
    }
}
