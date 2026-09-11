/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine;

/** Explicit memory/work limits for one worker-owned dimension engine. */
public record ThermalDimensionLimits(
        int maximumPages,
        int maximumSources,
        int maximumSourceNodes,
        int maximumArenaSlots,
        int maximumLiveCells,
        int maximumPairOperations,
        int maximumBoundaryOperations,
        int maximumPhaseOperations,
        int stableBatchesBeforeSleep,
        double sleepResidualC
) {
    public ThermalDimensionLimits {
        if (maximumPages <= 0 || maximumSources <= 0
                || maximumSourceNodes < maximumSources
                || maximumArenaSlots <= 0
                || maximumLiveCells <= 0
                || maximumPairOperations <= 0
                || maximumBoundaryOperations <= 0
                || maximumPhaseOperations <= 0
                || stableBatchesBeforeSleep <= 0
                || !Double.isFinite(sleepResidualC)
                || sleepResidualC < 0.0D) {
            throw new IllegalArgumentException("thermal dimension limits are invalid");
        }
    }
}
