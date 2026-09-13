/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, exact-sized geometry cut transferred from the Minecraft thread
 * to one dimension worker. The producer must not retain or mutate transferred
 * full-Page signature arrays.
 */
public final class ResolvedGeometryBatch {
    private static final int BLOCKS_PER_PAGE = 16 * 16 * 16;
    private static final byte[] NO_BYTES = new byte[0];
    private static final long[] NO_LONGS = new long[0];
    private static final int[] NO_INTS = new int[0];
    private static final ThermalPageHandle[] NO_PAGES = new ThermalPageHandle[0];
    private static final PageSignatures[] NO_PAGE_SIGNATURES =
            new PageSignatures[0];
    public static final ResolvedGeometryBatch EMPTY = new ResolvedGeometryBatch(
            NO_BYTES,
            NO_PAGES,
            NO_LONGS,
            NO_INTS,
            NO_INTS,
            NO_BYTES,
            NO_PAGE_SIGNATURES, MaterialChanges.EMPTY, new HaloUpdate[0]);

    private static final Kind[] KINDS = Kind.values();
    private static final ThermalPageHandle.GeometryResyncReason[] RESYNC_REASONS =
            ThermalPageHandle.GeometryResyncReason.values();

    public enum Kind {
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
    private final MaterialChanges materialChanges;
    private final HaloUpdate[] halos;
    public record HaloUpdate(ThermalPageHandle page, long geometryRevision, GeometryHalo halo) {}

    private ResolvedGeometryBatch(
            byte[] kinds,
            ThermalPageHandle[] pages,
            long[] geometryRevisions,
            int[] blockIndices,
            int[] signatureIds,
            byte[] resyncReasons,
            PageSignatures[] fullPageSignatures,
            MaterialChanges materialChanges, HaloUpdate[] halos
    ) {
        this.kinds = kinds;
        this.pages = pages;
        this.geometryRevisions = geometryRevisions;
        this.blockIndices = blockIndices;
        this.signatureIds = signatureIds;
        this.resyncReasons = resyncReasons;
        this.fullPageSignatures = fullPageSignatures;
        this.materialChanges = materialChanges;
        this.halos = halos;
    }

    public int size() {
        return kinds.length;
    }

    public boolean isEmpty() {
        return kinds.length == 0 && materialChanges.size() == 0 && halos.length == 0;
    }

    public MaterialChanges materialChanges() { return materialChanges; }
    public HaloUpdate[] halos() { return halos; }

    /** Ordered matter changes survive geometry coalescing, including A -> Air -> A. */
    public static final class MaterialChanges {
        public static final byte REPLACE = 1, MASS_CHANGE = 2, THERMAL_TRANSITION = 3, GAMEPLAY_TRANSITION = 4;
        public static final MaterialChanges EMPTY = new MaterialChanges(NO_PAGES, NO_INTS, NO_INTS, NO_INTS, NO_BYTES);
        private final ThermalPageHandle[] pages;
        private final int[] positions, previousSignatures, nextSignatures;
        private final byte[] causes;

        private MaterialChanges(ThermalPageHandle[] pages, int[] positions,
                int[] previousSignatures, int[] nextSignatures, byte[] causes) {
            this.pages = pages;
            this.positions = positions;
            this.previousSignatures = previousSignatures;
            this.nextSignatures = nextSignatures;
            this.causes = causes;
        }
        public int size() { return pages.length; }
        public ThermalPageHandle page(int index) { return pages[index]; }
        public int blockIndex(int index) { return positions[index]; }
        public int previousSignature(int index) { return previousSignatures[index]; }
        public int nextSignature(int index) { return nextSignatures[index]; }
        public byte cause(int index) { return causes[index]; }
    }

    public Kind kind(int index) {
        return KINDS[Byte.toUnsignedInt(kinds[index])];
    }

    public ThermalPageHandle page(int index) {
        return pages[index];
    }

    public long geometryRevision(int index) {
        return geometryRevisions[index];
    }

    public int blockIndex(int index) {
        return blockIndices[index];
    }

    public int signatureId(int index) {
        return signatureIds[index];
    }

    public ThermalPageHandle.GeometryResyncReason geometryResyncReason(int index) {
        int ordinal = resyncReasons[index];
        return ordinal < 0 ? null : RESYNC_REASONS[Byte.toUnsignedInt((byte) ordinal)];
    }

    /** Returns the transferred storage. Worker code must not expose it further. */
    public PageSignatures fullPageSignatures(int index) {
        return fullPageSignatures[index];
    }

    public static final class Builder {
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
        private ThermalPageHandle[] materialPages = NO_PAGES;
        private int[] materialPositions = NO_INTS, previousMaterialSignatures = NO_INTS, nextMaterialSignatures = NO_INTS;
        private byte[] materialCauses = NO_BYTES;
        private int materialCount;
        private final java.util.LinkedHashMap<ThermalPageHandle, HaloUpdate> haloUpdates = new java.util.LinkedHashMap<>();

        public void addHalo(ThermalPageHandle page, long revision, GeometryHalo halo) {
            haloUpdates.put(page, new HaloUpdate(page, revision, halo));
        }

        public void addMaterialChange(ThermalPageHandle page, int block, int previousSignature,
                int nextSignature, byte cause) {
            ensureMaterialCapacity(materialCount + 1);
            materialPages[materialCount] = page;
            materialPositions[materialCount] = block;
            previousMaterialSignatures[materialCount] = previousSignature;
            nextMaterialSignatures[materialCount] = nextSignature;
            materialCauses[materialCount++] = cause;
        }

        private void ensureMaterialCapacity(int required) {
            if (required > materialPages.length) {
                int capacity = Math.max(required, Math.max(8, materialPages.length * 2));
                materialPages = Arrays.copyOf(materialPages, capacity);
                materialPositions = Arrays.copyOf(materialPositions, capacity);
                previousMaterialSignatures = Arrays.copyOf(previousMaterialSignatures, capacity);
                nextMaterialSignatures = Arrays.copyOf(nextMaterialSignatures, capacity);
                materialCauses = Arrays.copyOf(materialCauses, capacity);
            }
        }

        public void prependMaterialChanges(MaterialChanges previous) {
            int count = previous.size();
            if (count == 0) return;
            ensureMaterialCapacity(materialCount + count);
            System.arraycopy(materialPages, 0, materialPages, count, materialCount);
            System.arraycopy(materialPositions, 0, materialPositions, count, materialCount);
            System.arraycopy(previousMaterialSignatures, 0, previousMaterialSignatures, count, materialCount);
            System.arraycopy(nextMaterialSignatures, 0, nextMaterialSignatures, count, materialCount);
            System.arraycopy(materialCauses, 0, materialCauses, count, materialCount);
            System.arraycopy(previous.pages, 0, materialPages, 0, count);
            System.arraycopy(previous.positions, 0, materialPositions, 0, count);
            System.arraycopy(previous.previousSignatures, 0, previousMaterialSignatures, 0, count);
            System.arraycopy(previous.nextSignatures, 0, nextMaterialSignatures, 0, count);
            System.arraycopy(previous.causes, 0, materialCauses, 0, count);
            materialCount += count;
        }

        public void addResolvedCenter(
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

        public void addFullResync(
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

        public ResolvedGeometryBatch buildAndReset() {
            if (size == 0 && materialCount == 0 && haloUpdates.isEmpty()) {
                return EMPTY;
            }
            ResolvedGeometryBatch batch = new ResolvedGeometryBatch(
                    Arrays.copyOf(kinds, size),
                    Arrays.copyOf(pages, size),
                    Arrays.copyOf(geometryRevisions, size),
                    Arrays.copyOf(blockIndices, size),
                    Arrays.copyOf(signatureIds, size),
                    Arrays.copyOf(resyncReasons, size),
                    Arrays.copyOf(fullPageSignatures, size), materialCount == 0 ? MaterialChanges.EMPTY
                            : new MaterialChanges(Arrays.copyOf(materialPages, materialCount),
                                    Arrays.copyOf(materialPositions, materialCount),
                                    Arrays.copyOf(previousMaterialSignatures, materialCount),
                                    Arrays.copyOf(nextMaterialSignatures, materialCount),
                                    Arrays.copyOf(materialCauses, materialCount)), haloUpdates.values().toArray(HaloUpdate[]::new));
            haloUpdates.clear();
            Arrays.fill(materialPages, 0, materialCount, null);
            materialCount = 0;
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
