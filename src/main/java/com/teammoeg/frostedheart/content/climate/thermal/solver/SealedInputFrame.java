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

/** Immutable main-thread cut across every input stream for one effective tick. */
public record SealedInputFrame(
        long effectiveTick,
        long dimensionGeneration,
        InputWatermarks watermarks
) {
    public SealedInputFrame {
        if (effectiveTick < 0L) {
            throw new IllegalArgumentException("effectiveTick must be non-negative");
        }
        if (dimensionGeneration < 0L) {
            throw new IllegalArgumentException("dimensionGeneration must be non-negative");
        }
        Objects.requireNonNull(watermarks, "watermarks");
    }
}
