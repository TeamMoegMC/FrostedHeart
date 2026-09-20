/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import java.util.Objects;

/**
 * Versioned destination of one source port.
 *
 * <p>A thermal-node binding includes the chunk incarnation that resolved it.
 * Loss bindings prevent unresolved targets from accumulating unbounded energy
 * debt.</p>
 */
public record SourceBinding(Kind kind, long targetId, int lifecycleGeneration) {
    private static final long AIR_TARGET_BIT = 1L << 31;
    public SourceBinding {
        Objects.requireNonNull(kind, "kind");
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
    }

    public static SourceBinding thermalNode(long nodeId, int lifecycleGeneration) {
        return new SourceBinding(Kind.THERMAL_NODE, nodeId, lifecycleGeneration);
    }

    public static SourceBinding declaredLoss(long sinkId) {
        return new SourceBinding(Kind.DECLARED_LOSS, sinkId, 0);
    }

    public static SourceBinding degradedLoss(long sinkId) {
        return new SourceBinding(Kind.DEGRADED_LOSS, sinkId, 0);
    }

    public boolean isThermalNode() {
        return kind == Kind.THERMAL_NODE;
    }

    public static SourceBinding airStencil(int loadId, int generation) {
        return new SourceBinding(Kind.AIR_STENCIL, loadId, generation);
    }

    public boolean isThermalTarget() {
        return kind == Kind.THERMAL_NODE || kind == Kind.AIR_STENCIL;
    }

    /** Separate material and Air load address spaces in the primitive accumulator index. */
    public long accumulatorTargetId() {
        return kind == Kind.AIR_STENCIL ? targetId | AIR_TARGET_BIT : targetId;
    }

    public static boolean isAirTarget(long accumulatorTargetId) {
        return (accumulatorTargetId & AIR_TARGET_BIT) != 0;
    }

    public static int targetIndex(long accumulatorTargetId) {
        return (int) (accumulatorTargetId & Integer.MAX_VALUE);
    }

    public enum Kind {
        THERMAL_NODE,
        DECLARED_LOSS,
        DEGRADED_LOSS,
        AIR_STENCIL
    }
}
