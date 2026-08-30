/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, exact-sized geometry cut transferred from the Minecraft thread
 * to one dimension worker. The producer must not retain or mutate transferred
 * full-Page signature arrays.
 */
final class ResolvedGeometryBatch {
    private static final int BLOCKS_PER_PAGE = 16 * 16 * 16;
    private static final byte[] NO_BYTES = new byte[0];
    private static final long[] NO_LONGS = new long[0];
    private static final int[] NO_INTS = new int[0];
    private static final ThermalPageHandle[] NO_PAGES = new ThermalPageHandle[0];
    private static final PageSignatures[] NO_PAGE_SIGNATURES =
            new PageSignatures[0];
    static final ResolvedGeometryBatch EMPTY = new ResolvedGeometryBatch(
            NO_BYTES,
            NO_PAGES,
            NO_LONGS,
            NO_INTS,
            NO_INTS,
            NO_BYTES,
            NO_PAGE_SIGNATURES);

    private static final Kind[] KINDS = Kind.values();
    private static final ThermalPageHandle.GeometryResyncReason[] RESYNC_REASONS =
            ThermalPageHandle.GeometryResyncReason.values();

    enum Kind {
        RESOLVED_CENTER,
        FULL_RESYNC_REQUIRED
    }

    private final byte[] kinds;
    private final ThermalPageHandle[] pages;
    private final long[] geometryRevisions;
    private final int[] blockIndices;
    private final int[] signatureIds;
    private final byte[] resyncReasons;
    private final PageSignatures[] fullPageSignatures;

    private ResolvedGeometryBatch(
            byte[] kinds,
            ThermalPageHandle[] pages,
            long[] geometryRevisions,
            int[] blockIndices,
            int[] signatureIds,
            byte[] resyncReasons,
            PageSignatures[] fullPageSignatures
    ) {
        this.kinds = kinds;
        this.pages = pages;
        this.geometryRevisions = geometryRevisions;
        this.blockIndices = blockIndices;
        this.signatureIds = signatureIds;
        this.resyncReasons = resyncReasons;
        this.fullPageSignatures = fullPageSignatures;
    }

    int size() {
        return kinds.length;
    }

    boolean isEmpty() {
        return kinds.length == 0;
    }

    Kind kind(int index) {
        return KINDS[Byte.toUnsignedInt(kinds[index])];
    }

    ThermalPageHandle page(int index) {
        return pages[index];
    }

    long geometryRevision(int index) {
        return geometryRevisions[index];
    }

    int blockIndex(int index) {
        return blockIndices[index];
    }

    int signatureId(int index) {
        return signatureIds[index];
    }

    ThermalPageHandle.GeometryResyncReason geometryResyncReason(int index) {
        int ordinal = resyncReasons[index];
        return ordinal < 0 ? null : RESYNC_REASONS[Byte.toUnsignedInt((byte) ordinal)];
    }

    /** Returns the transferred storage. Worker code must not expose it further. */
    PageSignatures fullPageSignatures(int index) {
        return fullPageSignatures[index];
    }

    static final class Builder {
        private static final int INITIAL_CAPACITY = 16;

        private byte[] kinds = new byte[INITIAL_CAPACITY];
        private ThermalPageHandle[] pages = new ThermalPageHandle[INITIAL_CAPACITY];
        private long[] geometryRevisions = new long[INITIAL_CAPACITY];
        private int[] blockIndices = new int[INITIAL_CAPACITY];
        private int[] signatureIds = new int[INITIAL_CAPACITY];
        private byte[] resyncReasons = new byte[INITIAL_CAPACITY];
        private PageSignatures[] fullPageSignatures =
                new PageSignatures[INITIAL_CAPACITY];
        private int size;

        void addResolvedCenter(
                ThermalPageHandle page,
                long geometryRevision,
                int blockIndex,
                int signatureId
        ) {
            if (blockIndex < 0 || blockIndex >= BLOCKS_PER_PAGE) {
                throw new IllegalArgumentException(
                        "blockIndex must be within [0, 4095]");
            }
            add(
                    Kind.RESOLVED_CENTER,
                    page,
                    geometryRevision,
                    blockIndex,
                    signatureId,
                    -1,
                    null);
        }

        void addFullResync(
                ThermalPageHandle page,
                long geometryRevision,
                ThermalPageHandle.GeometryResyncReason reason,
                PageSignatures pageSignatures
        ) {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(pageSignatures, "pageSignatures");
            add(
                    Kind.FULL_RESYNC_REQUIRED,
                    page,
                    geometryRevision,
                    -1,
                    -1,
                    reason.ordinal(),
                    pageSignatures);
        }

        ResolvedGeometryBatch buildAndReset() {
            if (size == 0) {
                return EMPTY;
            }
            ResolvedGeometryBatch batch = new ResolvedGeometryBatch(
                    Arrays.copyOf(kinds, size),
                    Arrays.copyOf(pages, size),
                    Arrays.copyOf(geometryRevisions, size),
                    Arrays.copyOf(blockIndices, size),
                    Arrays.copyOf(signatureIds, size),
                    Arrays.copyOf(resyncReasons, size),
                    Arrays.copyOf(fullPageSignatures, size));
            Arrays.fill(pages, 0, size, null);
            Arrays.fill(fullPageSignatures, 0, size, null);
            size = 0;
            return batch;
        }

        private void add(
                Kind kind,
                ThermalPageHandle page,
                long geometryRevision,
                int blockIndex,
                int signatureId,
                int resyncReason,
                PageSignatures fullSnapshot
        ) {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(page, "page");
            if (page.lifecycleGeneration() < 0L
                    || geometryRevision <= 0L) {
                throw new IllegalArgumentException(
                        "generation must be non-negative and revision positive");
            }
            ensureCapacity(size + 1);
            kinds[size] = (byte) kind.ordinal();
            pages[size] = page;
            geometryRevisions[size] = geometryRevision;
            blockIndices[size] = blockIndex;
            signatureIds[size] = signatureId;
            resyncReasons[size] = (byte) resyncReason;
            fullPageSignatures[size] = fullSnapshot;
            size++;
        }

        private void ensureCapacity(int required) {
            if (required <= kinds.length) {
                return;
            }
            int capacity = Math.max(required, kinds.length + (kinds.length >>> 1));
            kinds = Arrays.copyOf(kinds, capacity);
            pages = Arrays.copyOf(pages, capacity);
            geometryRevisions = Arrays.copyOf(geometryRevisions, capacity);
            blockIndices = Arrays.copyOf(blockIndices, capacity);
            signatureIds = Arrays.copyOf(signatureIds, capacity);
            resyncReasons = Arrays.copyOf(resyncReasons, capacity);
            fullPageSignatures = Arrays.copyOf(fullPageSignatures, capacity);
        }
    }
}
