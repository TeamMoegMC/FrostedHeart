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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;

import java.util.Objects;

/** Immutable local topology and numerical calibration for one engine generation. */
public record ThermalTopologyParameters(
        double effectiveAirCapacityJPerBlockK,
        double referenceTemperatureC,
        double effectiveMixingWPerBlockK,
        BuoyancyConductance.Parameters buoyancyParameters,
        int phaseRequestCapacity,
        int maximumPhaseMutationsPerCompletion
) {
    public ThermalTopologyParameters {
        if (!positive(effectiveAirCapacityJPerBlockK)
                || !Double.isFinite(referenceTemperatureC)
                || !positive(effectiveMixingWPerBlockK)
                || phaseRequestCapacity <= 0
                || maximumPhaseMutationsPerCompletion <= 0) {
            throw new IllegalArgumentException("thermal topology parameters are invalid");
        }
        Objects.requireNonNull(buoyancyParameters, "buoyancyParameters");
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0.0D;
    }
}
