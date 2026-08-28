/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

/** Local exposed-boundary calibration without room/component classification. */
public record FarFieldSettings(
        boolean enabled,
        double baseConductanceWPerK,
        double referenceOpeningAreaBlocksSquared,
        double continuationDistanceBlocks
) {
    public FarFieldSettings {
        if (!Double.isFinite(baseConductanceWPerK)
                || baseConductanceWPerK < 0.0D
                || !Double.isFinite(referenceOpeningAreaBlocksSquared)
                || referenceOpeningAreaBlocksSquared <= 0.0D
                || !Double.isFinite(continuationDistanceBlocks)
                || continuationDistanceBlocks <= 0.0D
                || enabled && baseConductanceWPerK <= 0.0D) {
            throw new IllegalArgumentException("FarField settings are invalid");
        }
    }

    public double conductanceForPatches(
            int openPatchCount,
            boolean directSkyExposure
    ) {
        if (!enabled || openPatchCount <= 0) {
            return 0.0D;
        }
        double continuation = directSkyExposure
                ? 1.0D
                : 1.0D / (1.0D + continuationDistanceBlocks);
        return baseConductanceWPerK
                * openPatchCount
                / (16.0D * referenceOpeningAreaBlocksSquared)
                * continuation;
    }
}
