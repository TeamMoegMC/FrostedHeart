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
        PageContinuation[] continuations
) {
    public static final PhaseTransitionRuntime.Request[] NO_PHASE_REQUESTS =
            new PhaseTransitionRuntime.Request[0];
    public static final ThermalPageHandle.GeometryResyncToken[] NO_RESYNC_TOKENS =
            new ThermalPageHandle.GeometryResyncToken[0];
    public static final PageContinuation[] NO_CONTINUATIONS =
            new PageContinuation[0];

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
        Objects.requireNonNull(continuations, "continuations");
        boolean failed = status == Status.ENGINE_FAILED;
        if (failed != (failure != null)) {
            throw new IllegalArgumentException(
                    "only failed completions carry a diagnostic");
        }
    }

    /** Page-scoped continuation publication with exact lifecycle identity. */
    public record PageContinuation(
            long sectionKey,
            long lifecycleGeneration,
            long geometryRevision,
            long topologyGeneration,
            byte faceMask
    ) {
        public PageContinuation {
            if (lifecycleGeneration < 0L || geometryRevision < 0L
                    || topologyGeneration < 0L
                    || (Byte.toUnsignedInt(faceMask) & ~0x3f) != 0) {
                throw new IllegalArgumentException(
                        "Page continuation identity is invalid");
            }
        }
    }
}
