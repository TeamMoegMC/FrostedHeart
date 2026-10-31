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

/** Local exposed-boundary calibration without room/component classification. */
public record FarFieldSettings(
        double baseConductanceWPerK,
        double referenceOpeningAreaBlocksSquared
) {
    public FarFieldSettings {
        if (!Double.isFinite(baseConductanceWPerK)
                || baseConductanceWPerK <= 0.0D
                || !Double.isFinite(referenceOpeningAreaBlocksSquared)
                || referenceOpeningAreaBlocksSquared <= 0.0D) {
            throw new IllegalArgumentException(
                    "FarField settings are invalid");
        }
    }

    double skyFaceConductanceWPerK() {
        return baseConductanceWPerK
                * 16
                / (16.0D * referenceOpeningAreaBlocksSquared);
    }
}
