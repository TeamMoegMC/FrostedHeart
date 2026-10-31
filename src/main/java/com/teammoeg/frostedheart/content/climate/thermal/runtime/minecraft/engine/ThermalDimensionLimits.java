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
        int maximumArenaSlots,
        int maximumLiveCells,
        int maximumPairOperations,
        int maximumBoundaryOperations
) {
    /**
     * Shared-grid storage grows lazily. Its representable geometry follows the
     * Page directory: at most one Air and one material cell per block, with
     * room for old/new spans during replacement. Fine numerical admission is a
     * separate memory/quality policy, not a second arbitrary live-cell ceiling.
     */
    public static ThermalDimensionLimits sharedGrid(int pages, int sources) {
        int cells = Math.multiplyExact(pages, 2 * 16 * 16 * 16);
        int overlap = Math.multiplyExact(2, cells);
        int slots = Integer.highestOneBit(overlap - 1) << 1;
        return new ThermalDimensionLimits(pages, sources, slots, cells,
                Integer.MAX_VALUE - 8, Integer.MAX_VALUE - 8);
    }

    public ThermalDimensionLimits {
        if (maximumPages <= 0 || maximumSources <= 0
                || maximumArenaSlots <= 0
                || maximumLiveCells <= 0
                || maximumPairOperations <= 0
                || maximumBoundaryOperations <= 0) {
            throw new IllegalArgumentException("thermal dimension limits are invalid");
        }
    }
}
