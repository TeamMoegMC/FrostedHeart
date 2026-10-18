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
