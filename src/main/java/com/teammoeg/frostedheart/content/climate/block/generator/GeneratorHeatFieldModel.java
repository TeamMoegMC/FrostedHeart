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
 */

package com.teammoeg.frostedheart.content.climate.block.generator;

/** Shared level-to-spherical-heat-field formulas for generators. */
public final class GeneratorHeatFieldModel {
    private GeneratorHeatFieldModel() {
    }

    public static int radiusBlocks(
            double rangeLevel,
            int baseRadiusBlocks,
            int additionalRadiusPerLevelBlocks
    ) {
        if (!Double.isFinite(rangeLevel) || rangeLevel <= 0.0) {
            return 0;
        }
        if (rangeLevel <= 1.0) {
            return (int) (baseRadiusBlocks * rangeLevel);
        }
        return (int) (baseRadiusBlocks
                + (rangeLevel - 1.0) * additionalRadiusPerLevelBlocks);
    }

    public static int temperatureCelsius(double temperatureLevel, int temperaturePerLevelCelsius) {
        if (!Double.isFinite(temperatureLevel) || temperatureLevel <= 0.0) {
            return 0;
        }
        return (int) (temperatureLevel * temperaturePerLevelCelsius);
    }
}
