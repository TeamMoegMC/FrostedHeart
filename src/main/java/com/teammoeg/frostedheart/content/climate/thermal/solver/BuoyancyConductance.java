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

package com.teammoeg.frostedheart.content.climate.thermal.solver;

/** Symmetric gameplay-scale buoyancy adjustment for vertical air conductance. */
public final class BuoyancyConductance {
    private BuoyancyConductance() {
    }

    public record Parameters(
            double minimumFactor,
            double maximumFactor,
            double temperatureScaleK
    ) {
        public Parameters {
            if (!Double.isFinite(minimumFactor) || minimumFactor < 0.0D) {
                throw new IllegalArgumentException("minimumFactor must be finite and non-negative");
            }
            if (!Double.isFinite(maximumFactor) || maximumFactor < minimumFactor) {
                throw new IllegalArgumentException(
                        "maximumFactor must be finite and at least minimumFactor");
            }
            if (!Double.isFinite(temperatureScaleK) || temperatureScaleK <= 0.0D) {
                throw new IllegalArgumentException("temperatureScaleK must be finite and positive");
            }
        }
    }

}
