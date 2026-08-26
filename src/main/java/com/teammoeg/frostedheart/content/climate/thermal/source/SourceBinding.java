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
 * Sink bindings keep energy observable without creating an unbounded debt.</p>
 */
public record SourceBinding(Kind kind, long targetId, int lifecycleGeneration) {
    public SourceBinding {
        Objects.requireNonNull(kind, "kind");
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
        if (kind == Kind.UNBOUND && (targetId != 0L || lifecycleGeneration != 0)) {
            throw new IllegalArgumentException("UNBOUND cannot carry a target or generation");
        }
    }

    public static SourceBinding thermalNode(long nodeId, int lifecycleGeneration) {
        return new SourceBinding(Kind.THERMAL_NODE, nodeId, lifecycleGeneration);
    }

    public static SourceBinding internalReservoir(long reservoirId) {
        return new SourceBinding(Kind.INTERNAL_RESERVOIR, reservoirId, 0);
    }

    public static SourceBinding declaredLoss(long sinkId) {
        return new SourceBinding(Kind.DECLARED_LOSS, sinkId, 0);
    }

    public static SourceBinding degradedLoss(long sinkId) {
        return new SourceBinding(Kind.DEGRADED_LOSS, sinkId, 0);
    }

    public static SourceBinding unbound() {
        return new SourceBinding(Kind.UNBOUND, 0L, 0);
    }

    public boolean isThermalNode() {
        return kind == Kind.THERMAL_NODE;
    }

    public enum Kind {
        THERMAL_NODE,
        INTERNAL_RESERVOIR,
        DECLARED_LOSS,
        DEGRADED_LOSS,
        UNBOUND
    }
}
