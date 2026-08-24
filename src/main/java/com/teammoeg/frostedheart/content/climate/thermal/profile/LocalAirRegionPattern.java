/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.profile;

/** Immutable correctness-width representation of one block-local air region. */
public record LocalAirRegionPattern(
        int localRegionId,
        long provenAirMicrocellMask,
        int negativeXMask,
        int positiveXMask,
        int negativeYMask,
        int positiveYMask,
        int negativeZMask,
        int positiveZMask
) {
    private static final int FULL_FACE_MASK = 0xffff;

    public LocalAirRegionPattern {
        if (localRegionId < 0) {
            throw new IllegalArgumentException("localRegionId must be non-negative");
        }
        if (provenAirMicrocellMask == 0L) {
            throw new IllegalArgumentException("air region must contain proven air microcells");
        }
        requireFaceMask(negativeXMask);
        requireFaceMask(positiveXMask);
        requireFaceMask(negativeYMask);
        requireFaceMask(positiveYMask);
        requireFaceMask(negativeZMask);
        requireFaceMask(positiveZMask);
    }

    public int microcellCount() {
        return Long.bitCount(provenAirMicrocellMask);
    }

    private static void requireFaceMask(int faceMask) {
        if ((faceMask & ~FULL_FACE_MASK) != 0) {
            throw new IllegalArgumentException("face aperture must be a 16-bit mask");
        }
    }
}
