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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;

import java.util.Objects;

/** Immutable worker result drained by the owning Minecraft level thread. */
public record ThermalCompletion(
        long dimensionGeneration,
        long batchSequence,
        Status status,
        RuntimeException failure,
        PhaseTransitionRuntime.Request[] phaseRequests,
        ThermalPageHandle.GeometryResyncToken[] committedResyncTokens,
        BrickResidency[] residencyUpdates
) {
    public static final PhaseTransitionRuntime.Request[] NO_PHASE_REQUESTS =
            new PhaseTransitionRuntime.Request[0];
    public static final ThermalPageHandle.GeometryResyncToken[] NO_RESYNC_TOKENS =
            new ThermalPageHandle.GeometryResyncToken[0];
    public static final BrickResidency[] NO_RESIDENCY_UPDATES =
            new BrickResidency[0];

    public enum Status {
        COMPLETED,
        WORK_LIMITED,
        ENGINE_FAILED
    }

    public ThermalCompletion {
        if (dimensionGeneration < 0L || batchSequence <= 0L) {
            throw new IllegalArgumentException("thermal completion identity is invalid");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(phaseRequests, "phaseRequests");
        Objects.requireNonNull(committedResyncTokens, "committedResyncTokens");
        Objects.requireNonNull(residencyUpdates, "residencyUpdates");
        boolean failed = status == Status.ENGINE_FAILED;
        if (failed != (failure != null)) {
            throw new IllegalArgumentException(
                    "only failed completions carry a diagnostic");
        }
    }

    /** Absolute worker-desired Brick mask for one section. */
    public record BrickResidency(
            long sectionKey,
            long lifecycleGeneration,
            long desiredBrickMask
    ) {
        public BrickResidency {
            if (lifecycleGeneration < -1L) {
                throw new IllegalArgumentException(
                        "Brick residency identity is invalid");
            }
        }
    }
}
