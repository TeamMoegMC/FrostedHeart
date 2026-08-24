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

import java.util.Arrays;

/** Fixed primitive cache for the 64 base, eight octant, and one section summaries. */
public final class GeometrySummaryCache {
    public static final int BASE_SUMMARY_COUNT = 64;
    public static final int OCTANT_SUMMARY_COUNT = 8;
    public static final int OCTANT_SUMMARY_OFFSET = BASE_SUMMARY_COUNT;
    public static final int SECTION_SUMMARY_INDEX = BASE_SUMMARY_COUNT + OCTANT_SUMMARY_COUNT;
    public static final int SUMMARY_COUNT = SECTION_SUMMARY_INDEX + 1;

    private final byte[] kinds = new byte[SUMMARY_COUNT];
    private final int[] mediumIds = new int[SUMMARY_COUNT];
    private final int[] topologyFlags = new int[SUMMARY_COUNT];

    public GeometrySummaryCache() {
        Arrays.fill(mediumIds, GeometrySummary.NO_MEDIUM);
    }

    public GeometrySummary summary(int summaryIndex) {
        requireSummaryIndex(summaryIndex);
        return new GeometrySummary(
                GeometrySummary.Kind.values()[kinds[summaryIndex]],
                mediumIds[summaryIndex],
                topologyFlags[summaryIndex]
        );
    }

    public GeometrySummary baseSummary(int baseIndex) {
        requireBaseIndex(baseIndex);
        return summary(baseIndex);
    }

    public GeometrySummary octantSummary(int octantIndex) {
        requireOctantIndex(octantIndex);
        return summary(OCTANT_SUMMARY_OFFSET + octantIndex);
    }

    public GeometrySummary sectionSummary() {
        return summary(SECTION_SUMMARY_INDEX);
    }

    public void setBaseSummary(int baseIndex, GeometrySummary summary) {
        requireBaseIndex(baseIndex);
        set(baseIndex, summary);
        rebuildOctantAndSection(octantIndexForBase(baseIndex));
    }

    public void replaceAll(GeometrySummary[] summaries) {
        if (summaries == null || summaries.length != SUMMARY_COUNT) {
            throw new IllegalArgumentException("summaries must contain exactly 73 entries");
        }
        for (int index = 0; index < summaries.length; index++) {
            set(index, summaries[index]);
        }
    }

    public GeometrySummary[] snapshot() {
        GeometrySummary[] result = new GeometrySummary[SUMMARY_COUNT];
        for (int index = 0; index < result.length; index++) {
            result[index] = summary(index);
        }
        return result;
    }

    /** Installs Minecraft's cheap empty-section proof without scanning child states. */
    public void installAllAirProof(int airMediumId) {
        GeometrySummary allAir = GeometrySummary.singleAir(airMediumId);
        for (int index = 0; index < SUMMARY_COUNT; index++) {
            set(index, allAir);
        }
    }

    /** Invalidates only the changed base summary and its two cached ancestors. */
    public void invalidateBaseBrick(int baseIndex) {
        requireBaseIndex(baseIndex);
        setUnknown(baseIndex);
        setUnknown(OCTANT_SUMMARY_OFFSET + octantIndexForBase(baseIndex));
        setUnknown(SECTION_SUMMARY_INDEX);
    }

    public void rebuildAllParents() {
        for (int octant = 0; octant < OCTANT_SUMMARY_COUNT; octant++) {
            rebuildOctant(octant);
        }
        rebuildSection();
    }

    public static int baseIndex(int localX, int localY, int localZ) {
        requireLocalCoordinate("localX", localX);
        requireLocalCoordinate("localY", localY);
        requireLocalCoordinate("localZ", localZ);
        int brickX = localX >>> 2;
        int brickY = localY >>> 2;
        int brickZ = localZ >>> 2;
        return brickX | (brickZ << 2) | (brickY << 4);
    }

    public static int brickVoxelIndex(int localX, int localY, int localZ) {
        requireLocalCoordinate("localX", localX);
        requireLocalCoordinate("localY", localY);
        requireLocalCoordinate("localZ", localZ);
        return (localX & 3) | ((localZ & 3) << 2) | ((localY & 3) << 4);
    }

    public static int octantIndexForBase(int baseIndex) {
        requireBaseIndex(baseIndex);
        int brickX = baseIndex & 3;
        int brickZ = (baseIndex >>> 2) & 3;
        int brickY = (baseIndex >>> 4) & 3;
        return (brickX >>> 1) | ((brickZ >>> 1) << 1) | ((brickY >>> 1) << 2);
    }

    private void rebuildOctantAndSection(int octantIndex) {
        rebuildOctant(octantIndex);
        rebuildSection();
    }

    private void rebuildOctant(int octantIndex) {
        int originBrickX = (octantIndex & 1) << 1;
        int originBrickZ = ((octantIndex >>> 1) & 1) << 1;
        int originBrickY = ((octantIndex >>> 2) & 1) << 1;
        int[] children = new int[8];
        int write = 0;
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 2; x++) {
                    children[write++] = (originBrickX + x)
                            | ((originBrickZ + z) << 2)
                            | ((originBrickY + y) << 4);
                }
            }
        }
        mergeChildrenInto(children, OCTANT_SUMMARY_OFFSET + octantIndex);
    }

    private void rebuildSection() {
        int[] children = new int[OCTANT_SUMMARY_COUNT];
        for (int index = 0; index < children.length; index++) {
            children[index] = OCTANT_SUMMARY_OFFSET + index;
        }
        mergeChildrenInto(children, SECTION_SUMMARY_INDEX);
    }

    private void mergeChildrenInto(int[] children, int target) {
        int first = children[0];
        byte expectedKind = kinds[first];
        int expectedMedium = mediumIds[first];
        int combinedFlags = 0;
        if (expectedKind == (byte) GeometrySummary.Kind.UNKNOWN.ordinal()) {
            setUnknown(target);
            return;
        }
        boolean homogeneous = isMergeCandidate(first);
        for (int child : children) {
            if (kinds[child] == (byte) GeometrySummary.Kind.UNKNOWN.ordinal()) {
                setUnknown(target);
                return;
            }
            combinedFlags |= topologyFlags[child];
            homogeneous &= kinds[child] == expectedKind
                    && mediumIds[child] == expectedMedium
                    && isMergeCandidate(child);
        }
        if (homogeneous) {
            int mergedFlags = expectedKind == (byte) GeometrySummary.Kind.NO_AIR.ordinal()
                    ? combinedFlags
                    : GeometrySummary.SINGLE_CONNECTED_COMPONENT;
            setRaw(target, expectedKind, expectedMedium, mergedFlags);
        } else {
            setRaw(target, (byte) GeometrySummary.Kind.MIXED.ordinal(),
                    GeometrySummary.NO_MEDIUM, combinedFlags);
        }
    }

    private boolean isMergeCandidate(int index) {
        GeometrySummary.Kind kind = GeometrySummary.Kind.values()[kinds[index]];
        int flags = topologyFlags[index];
        return (kind == GeometrySummary.Kind.NO_AIR
                || kind == GeometrySummary.Kind.SINGLE_AIR
                || kind == GeometrySummary.Kind.SINGLE_MEDIUM)
                && (kind == GeometrySummary.Kind.NO_AIR
                        ? (flags & ~GeometrySummary.UNRESOLVED_TOPOLOGY) == 0
                        : (flags & ~GeometrySummary.SINGLE_CONNECTED_COMPONENT) == 0);
    }

    private void set(int index, GeometrySummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("summary is required");
        }
        setRaw(index, (byte) summary.kind().ordinal(), summary.mediumId(), summary.topologyFlags());
    }

    private void setUnknown(int index) {
        setRaw(index, (byte) GeometrySummary.Kind.UNKNOWN.ordinal(), GeometrySummary.NO_MEDIUM, 0);
    }

    private void setRaw(int index, byte kind, int mediumId, int flags) {
        kinds[index] = kind;
        mediumIds[index] = mediumId;
        topologyFlags[index] = flags;
    }

    private static void requireSummaryIndex(int index) {
        if (index < 0 || index >= SUMMARY_COUNT) {
            throw new IllegalArgumentException("summaryIndex must be within [0, 72]");
        }
    }

    private static void requireBaseIndex(int index) {
        if (index < 0 || index >= BASE_SUMMARY_COUNT) {
            throw new IllegalArgumentException("baseIndex must be within [0, 63]");
        }
    }

    private static void requireOctantIndex(int index) {
        if (index < 0 || index >= OCTANT_SUMMARY_COUNT) {
            throw new IllegalArgumentException("octantIndex must be within [0, 7]");
        }
    }

    private static void requireLocalCoordinate(String name, int value) {
        if (value < 0 || value >= 16) {
            throw new IllegalArgumentException(name + " must be within [0, 15]");
        }
    }
}
