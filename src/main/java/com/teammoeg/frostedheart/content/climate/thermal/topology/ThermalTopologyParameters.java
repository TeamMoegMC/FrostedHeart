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
