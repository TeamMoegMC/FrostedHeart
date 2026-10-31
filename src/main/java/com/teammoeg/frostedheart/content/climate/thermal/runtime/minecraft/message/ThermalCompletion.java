/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import java.util.Objects;

/** Once-only world effects; accepting input is a separate synchronous ownership receipt. */
public record ThermalCompletion(long dimensionGeneration, long effectSequence, Status status,
        RuntimeException failure, PhaseTransitionRuntime.Request[] phaseRequests,
        ThermalPageHandle.GeometryResyncToken[] committedResyncTokens, BrickResidency[] residencyUpdates) {
    public static final int MAXIMUM_PENDING = 3;
    public static final PhaseTransitionRuntime.Request[] NO_PHASE_REQUESTS = new PhaseTransitionRuntime.Request[0];
    public static final ThermalPageHandle.GeometryResyncToken[] NO_RESYNC_TOKENS = new ThermalPageHandle.GeometryResyncToken[0];
    public static final BrickResidency[] NO_RESIDENCY_UPDATES = new BrickResidency[0];

    public enum Status { COMPLETED, WORK_LIMITED, ENGINE_FAILED }

    public ThermalCompletion {
        if (dimensionGeneration < 0 || effectSequence <= 0) throw new IllegalArgumentException("thermal effect identity is invalid");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(phaseRequests, "phaseRequests");
        Objects.requireNonNull(committedResyncTokens, "committedResyncTokens");
        Objects.requireNonNull(residencyUpdates, "residencyUpdates");
        if ((status == Status.ENGINE_FAILED) != (failure != null))
            throw new IllegalArgumentException("only failed effects carry a diagnostic");
    }

    /** Absolute worker-desired Brick mask for one section. */
    public record BrickResidency(long sectionKey, long lifecycleGeneration, long desiredBrickMask) {
        public BrickResidency {
            if (lifecycleGeneration < -1) throw new IllegalArgumentException("Brick residency identity is invalid");
        }
    }
}
