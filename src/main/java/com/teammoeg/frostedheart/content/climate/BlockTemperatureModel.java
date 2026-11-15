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

package com.teammoeg.frostedheart.content.climate;

/**
 * Pure block-temperature formulas shared by gameplay and the town simulator.
 *
 * <p>The heat input is the maximum heat-field value at the queried block. It
 * is a temperature control value, not an additive energy quantity.</p>
 */
public final class BlockTemperatureModel {
    private BlockTemperatureModel() {
    }

    public static float naturalTemperature(
            float dimensionTemperatureCelsius,
            float biomeTemperatureCelsius,
            float altitudeTemperatureCelsius,
            float climateTemperatureCelsius,
            float climateBlockAffection
    ) {
        return dimensionTemperatureCelsius
                + biomeTemperatureCelsius
                + altitudeTemperatureCelsius
                + climateTemperatureCelsius * climateBlockAffection;
    }

    /**
     * Applies the current generator heat-field rule:
     * {@code nature} when nature is above the heat ceiling, otherwise
     * {@code min(nature + 2 * heat, heat)}.
     */
    public static float applyHeat(
            float naturalTemperatureCelsius,
            float maximumHeatFieldTemperatureCelsius,
            float heatApplicationMultiplier,
            float absoluteZeroCelsius
    ) {
        float result = naturalTemperatureCelsius > maximumHeatFieldTemperatureCelsius
                ? naturalTemperatureCelsius
                : Math.min(
                        naturalTemperatureCelsius
                                + maximumHeatFieldTemperatureCelsius * heatApplicationMultiplier,
                        maximumHeatFieldTemperatureCelsius);
        return Math.max(absoluteZeroCelsius, result);
    }
}
