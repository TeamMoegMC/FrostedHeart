/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.geometry;

/**
 * Cold/debug view of one cached 4, 8, or 16 block geometry summary.
 * Runtime storage is primitive and owned by {@link GeometrySummaryCache}.
 */
public record GeometrySummary(Kind kind, int mediumId, int topologyFlags) {
    public static final int NO_MEDIUM = -1;

    public static final int SINGLE_CONNECTED_COMPONENT = 1;
    public static final int INTERNAL_GATE = 1 << 1;
    public static final int MATERIAL_INTERFACE = 1 << 2;
    public static final int PHASE_STATE = 1 << 3;
    public static final int SOURCE_CORE = 1 << 4;
    public static final int UNRESOLVED_TOPOLOGY = 1 << 5;

    private static final int KNOWN_FLAGS = SINGLE_CONNECTED_COMPONENT
            | INTERNAL_GATE
            | MATERIAL_INTERFACE
            | PHASE_STATE
            | SOURCE_CORE
            | UNRESOLVED_TOPOLOGY;
    private static final int COARSE_MERGE_BLOCKERS = INTERNAL_GATE
            | MATERIAL_INTERFACE
            | PHASE_STATE
            | SOURCE_CORE
            | UNRESOLVED_TOPOLOGY;

    public GeometrySummary {
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if ((topologyFlags & ~KNOWN_FLAGS) != 0) {
            throw new IllegalArgumentException("topologyFlags contains unknown bits");
        }
        boolean carriesMedium = kind == Kind.SINGLE_AIR || kind == Kind.SINGLE_MEDIUM;
        if (carriesMedium != (mediumId >= 0)) {
            throw new IllegalArgumentException(
                    "only single-medium summaries carry a non-negative medium ID");
        }
        if (carriesMedium && (topologyFlags & SINGLE_CONNECTED_COMPONENT) == 0) {
            throw new IllegalArgumentException(
                    "single-medium summaries must prove one connected component");
        }
    }

    public static GeometrySummary unknown() {
        return new GeometrySummary(Kind.UNKNOWN, NO_MEDIUM, 0);
    }

    public static GeometrySummary singleAir(int mediumId) {
        return new GeometrySummary(Kind.SINGLE_AIR, mediumId, SINGLE_CONNECTED_COMPONENT);
    }

    public static GeometrySummary singleMedium(int mediumId) {
        return new GeometrySummary(Kind.SINGLE_MEDIUM, mediumId, SINGLE_CONNECTED_COMPONENT);
    }

    public static GeometrySummary noAir(int topologyFlags) {
        return new GeometrySummary(Kind.NO_AIR, NO_MEDIUM, topologyFlags);
    }

    public static GeometrySummary mixed(int topologyFlags) {
        return new GeometrySummary(Kind.MIXED, NO_MEDIUM, topologyFlags);
    }

    public boolean canParticipateInCoarseMerge() {
        return (kind == Kind.SINGLE_AIR || kind == Kind.SINGLE_MEDIUM)
                && (topologyFlags & COARSE_MERGE_BLOCKERS) == 0;
    }

    public enum Kind {
        UNKNOWN,
        NO_AIR,
        SINGLE_AIR,
        SINGLE_MEDIUM,
        MIXED
    }
}
