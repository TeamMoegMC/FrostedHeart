/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
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

    public static float climateBlockAffection(
            int y,
            int stoneInterfaceLevel,
            int seaLevel,
            float maximumAffection
    ) {
        if (y > seaLevel) return maximumAffection;
        if (y > stoneInterfaceLevel) {
            return maximumAffection * (y - stoneInterfaceLevel) / (seaLevel - stoneInterfaceLevel);
        }
        return 0.0F;
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

    public static float blockTemperature(
            int y,
            int stoneInterfaceLevel,
            int seaLevel,
            float maximumClimateBlockAffection,
            float dimensionTemperatureCelsius,
            float biomeTemperatureCelsius,
            float altitudeTemperatureCelsius,
            float climateTemperatureCelsius,
            float maximumHeatFieldTemperatureCelsius,
            float heatApplicationMultiplier,
            float absoluteZeroCelsius
    ) {
        float alpha = climateBlockAffection(
                y, stoneInterfaceLevel, seaLevel, maximumClimateBlockAffection);
        float natural = naturalTemperature(
                dimensionTemperatureCelsius, biomeTemperatureCelsius,
                altitudeTemperatureCelsius, climateTemperatureCelsius, alpha);
        return applyHeat(
                natural, maximumHeatFieldTemperatureCelsius,
                heatApplicationMultiplier, absoluteZeroCelsius);
    }
}
