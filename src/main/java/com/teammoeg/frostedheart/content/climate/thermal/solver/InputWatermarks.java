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

/** Monotonic input cuts that must be applied before an epoch can be solved. */
public record InputWatermarks(
        long geometry,
        long source,
        long chunk,
        long profile,
        long transitionAck
) {
    public static final InputWatermarks ZERO = new InputWatermarks(0L, 0L, 0L, 0L, 0L);

    public InputWatermarks {
        requireNonNegative("geometry", geometry);
        requireNonNegative("source", source);
        requireNonNegative("chunk", chunk);
        requireNonNegative("profile", profile);
        requireNonNegative("transitionAck", transitionAck);
    }

    /** Returns whether every stream has reached at least the required cut. */
    public boolean covers(InputWatermarks required) {
        if (required == null) {
            return false;
        }
        return geometry >= required.geometry
                && source >= required.source
                && chunk >= required.chunk
                && profile >= required.profile
                && transitionAck >= required.transitionAck;
    }

    /** Source readiness is owned by ThermalSourceTimeline during execution. */
    public boolean coversNonSourceStreams(InputWatermarks required) {
        if (required == null) {
            return false;
        }
        return geometry >= required.geometry
                && chunk >= required.chunk
                && profile >= required.profile
                && transitionAck >= required.transitionAck;
    }

    public InputWatermarks withSource(long appliedSourceWatermark) {
        return new InputWatermarks(
                geometry,
                appliedSourceWatermark,
                chunk,
                profile,
                transitionAck
        );
    }

    private static void requireNonNegative(String name, long value) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " watermark must be non-negative");
        }
    }
}
