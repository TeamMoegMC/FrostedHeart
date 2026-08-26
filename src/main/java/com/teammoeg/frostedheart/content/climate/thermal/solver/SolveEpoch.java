/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Objects;

/** A dimension-wide thermal interval with one sealed input watermark vector. */
public record SolveEpoch(
        long previousTick,
        long targetTick,
        long epochId,
        long dimensionGeneration,
        InputWatermarks sealedWatermarks
) {
    public static final double TICKS_PER_SECOND = 20.0D;

    public SolveEpoch {
        if (previousTick < 0L) {
            throw new IllegalArgumentException("previousTick must be non-negative");
        }
        if (targetTick < previousTick) {
            throw new IllegalArgumentException("targetTick must not precede previousTick");
        }
        if (epochId <= 0L) {
            throw new IllegalArgumentException("epochId must be positive");
        }
        if (dimensionGeneration < 0L) {
            throw new IllegalArgumentException("dimensionGeneration must be non-negative");
        }
        Objects.requireNonNull(sealedWatermarks, "sealedWatermarks");
    }

    public static SolveEpoch fromFrame(
            long previousTick,
            long epochId,
            SealedInputFrame frame
    ) {
        Objects.requireNonNull(frame, "frame");
        return new SolveEpoch(
                previousTick,
                frame.effectiveTick(),
                epochId,
                frame.dimensionGeneration(),
                frame.watermarks()
        );
    }

    public long durationTicks() {
        return targetTick - previousTick;
    }

    public double dtSeconds() {
        return durationTicks() / TICKS_PER_SECOND;
    }

    /** Checks streams that must already be applied before source integration starts. */
    public boolean nonSourceInputsSatisfiedBy(
            long appliedDimensionGeneration,
            InputWatermarks appliedWatermarks
    ) {
        return appliedDimensionGeneration == dimensionGeneration
                && appliedWatermarks != null
                && appliedWatermarks.coversNonSourceStreams(sealedWatermarks);
    }

    public long sourceWatermark() {
        return sealedWatermarks.source();
    }
}
