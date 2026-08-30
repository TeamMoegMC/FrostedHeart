/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;

import java.util.Objects;

/** Immutable local topology and numerical calibration for one engine generation. */
public record ThermalTopologyParameters(
        int maximumRegionsPerBlock,
        double effectiveAirCapacityJPerBlockK,
        double referenceTemperatureC,
        double effectiveMixingWPerBlockK,
        double minimumMixedFaceDistanceBlocks,
        BuoyancyConductance.Parameters buoyancyParameters,
        int phaseRequestCapacity,
        int maximumPhaseMutationsPerCompletion
) {
    public ThermalTopologyParameters {
        if (maximumRegionsPerBlock <= 0
                || !positive(effectiveAirCapacityJPerBlockK)
                || !Double.isFinite(referenceTemperatureC)
                || !positive(effectiveMixingWPerBlockK)
                || !positive(minimumMixedFaceDistanceBlocks)
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
