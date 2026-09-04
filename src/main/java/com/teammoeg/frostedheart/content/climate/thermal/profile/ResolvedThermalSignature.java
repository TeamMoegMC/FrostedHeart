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

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;

import java.util.Objects;

/**
 * State-static Air geometry and material identity interned on the main thread.
 */
public record ResolvedThermalSignature(
        ConservativeAirGeometry.Resolution airGeometry,
        int materialProfileId,
        int materialContactPatternId
) {
    public ResolvedThermalSignature {
        Objects.requireNonNull(airGeometry, "airGeometry");
        if (airGeometry.status() != ConservativeAirGeometry.Status.RESOLVED) {
            throw new IllegalArgumentException(
                    "thermal signature requires resolved Air geometry");
        }
        requireId("materialProfileId", materialProfileId);
        requireId("materialContactPatternId", materialContactPatternId);
        long occupied = 0L;
        var components = airGeometry.components();
        for (int expected = 0; expected < components.size(); expected++) {
            var component = components.get(expected);
            if (component.id() != expected
                    || (occupied & component.microcellMask()) != 0L) {
                throw new IllegalArgumentException(
                        "Air components must be dense and non-overlapping");
            }
            occupied |= component.microcellMask();
        }
    }

    private static void requireId(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be a non-negative int ID");
        }
    }
}
