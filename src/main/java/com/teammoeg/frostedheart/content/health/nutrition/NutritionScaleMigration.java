/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.health.nutrition;

/** Pure conversion from the legacy nutrition configuration scale to percentages. */
public final class NutritionScaleMigration {
    private NutritionScaleMigration() {
    }

    /**
     * Converts version-one configuration rates to the percentage-state scale.
     *
     * <p>The caller is responsible for applying this migration only once when advancing the
     * stored configuration version.</p>
     *
     * @param gainRate legacy food gain rate
     * @param consumptionRate legacy hunger consumption rate
     * @return rates scaled by {@code 400} and {@code 100}, respectively
     */
    public static Rates fromVersionOne(double gainRate, double consumptionRate) {
        return new Rates(gainRate * 400.0, consumptionRate * 100.0);
    }

    public record Rates(double gainRate, double consumptionRate) {
    }
}
