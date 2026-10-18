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
                || stableBatchesBeforeSleep <= 0
                || !Double.isFinite(sleepResidualC)
                || sleepResidualC < 0.0D) {
            throw new IllegalArgumentException("thermal dimension limits are invalid");
        }
    }
}
