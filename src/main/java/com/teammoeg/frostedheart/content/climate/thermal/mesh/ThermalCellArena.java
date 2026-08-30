/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;

import java.util.Arrays;

/**
 * Primitive structure-of-arrays storage for air cells and sparse material poles.
 *
 * <p>The arena is owned by one logical thermal writer. Page compilation uses
 * {@link ArenaSpan} allocations to install fixed 4x4x4 Brick fragments while
 * enthalpy and capacity remain authoritative here.</p>
 */
public final class ThermalCellArena {
    private static final int NO_SLOT = -1;
    private static final int[] NO_SLOTS = new int[0];

    private static final byte FREE = 0;
    private static final byte LIVE = 1;
    private static final byte RESERVED = 2;
    private static final byte REGULAR_CELL = 0;
    private static final byte MIXED_COMPONENT = 1;
    private static final byte MATERIAL_CELL = 2;
    private static final byte PHASE_RESERVOIR = 3;

    private double[] enthalpyJ;
    private double[] capacityJPerK;
    private double[] inverseCapacityKPerJ;
    private int[] pageSlots;
    private int[] lifecycleGenerations;
    private int[] supportRefs;
    private int[] minimumX;
    private int[] minimumY;
    private int[] minimumZ;
    private byte[] cellKinds;
    private int[] mixedComponentIds;
    private ComponentBrickCompiler.CompiledBrick[] mixedBrickGeometries;
    private byte[] allocationState;
    private long[] liveSlots;
    private long[] liveWordSummary;
    private final ThermalFreeSpanIndex freeSpans = new ThermalFreeSpanIndex();
    private final ThermalPhaseReservoirStore phases;

    private int highWaterMark;
    private int allocationHighWaterMark;
    private int liveCellCount;

    public ThermalCellArena(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        enthalpyJ = new double[initialCapacity];
        capacityJPerK = new double[initialCapacity];
        inverseCapacityKPerJ = new double[initialCapacity];
        pageSlots = new int[initialCapacity];
        lifecycleGenerations = new int[initialCapacity];
        supportRefs = new int[initialCapacity];
        minimumX = new int[initialCapacity];
        minimumY = new int[initialCapacity];
        minimumZ = new int[initialCapacity];
        cellKinds = new byte[initialCapacity];
        mixedComponentIds = new int[initialCapacity];
        mixedBrickGeometries = new ComponentBrickCompiler.CompiledBrick[initialCapacity];
        phases = new ThermalPhaseReservoirStore(initialCapacity);
        allocationState = new byte[initialCapacity];
        liveSlots = new long[(initialCapacity + 63) >>> 6];
        liveWordSummary = new long[(liveSlots.length + 63) >>> 6];
        Arrays.fill(pageSlots, NO_SLOT);
        Arrays.fill(supportRefs, NO_SLOT);
        Arrays.fill(mixedComponentIds, NO_SLOT);
    }

    public int highWaterMark() {
        return highWaterMark;
    }

    public int liveCellCount() {
        return liveCellCount;
    }

    /** Address limit required by committed and currently staged spans. */
    public int requiredSlotCapacity() {
        return allocationHighWaterMark;
    }

    public boolean isLive(int slot) {
        return slot >= 0 && slot < highWaterMark && allocationState[slot] == LIVE;
    }

    public boolean isStagedCell(int slot) {
        return slot >= 0 && slot < allocationHighWaterMark
                && allocationState[slot] == RESERVED;
    }

    /** Returns the next live slot at or after {@code fromInclusive}, or -1. */
    public int nextLiveSlot(int fromInclusive) {
        int start = Math.max(0, fromInclusive);
        if (start >= highWaterMark) {
            return NO_SLOT;
        }
        int wordIndex = start >>> 6;
        long word = liveSlots[wordIndex] & (-1L << (start & 63));
        if (word != 0L) {
            return (wordIndex << 6) + Long.numberOfTrailingZeros(word);
        }
        for (wordIndex = nextOccupiedWord(wordIndex + 1);
             wordIndex >= 0;
             wordIndex = nextOccupiedWord(wordIndex + 1)) {
            word = liveSlots[wordIndex];
            if (word != 0L) {
                int slot = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return slot < highWaterMark ? slot : NO_SLOT;
            }
        }
        return NO_SLOT;
    }

    /** Reserves and fills one worker-private Brick without publishing live cells. */
    public BrickAllocation stageBrickCells(
            int pageSlot,
            int lifecycleGeneration,
            ThermalBrickCellLayout layout,
            double initialAirTemperatureC,
            double referenceTemperatureC,
            int maximumSlots
    ) {
        if (pageSlot < 0 || layout == null || maximumSlots <= 0
                || maximumSlots < allocationHighWaterMark) {
            throw new IllegalArgumentException("Brick allocation identity is invalid");
        }
        requireLifecycleGeneration(lifecycleGeneration);
        requireFinite("initialAirTemperatureC", initialAirTemperatureC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        layout.requireReady();
        int airCells = switch (layout.airKind) {
            case NONE -> 0;
            case REGULAR -> 1;
            case MIXED -> layout.mixedGeometry.componentCount();
        };
        int totalCells = Math.addExact(
                airCells,
                Math.addExact(layout.materialCount, layout.phaseCount));
        if (totalCells == 0) {
            return BrickAllocation.EMPTY;
        }

        int firstSlot = findFreeSpan(totalCells);
        int required = Math.addExact(firstSlot, totalCells);
        if (required > maximumSlots) {
            return null;
        }
        allocationHighWaterMark = Math.max(
                allocationHighWaterMark, required);
        try {
            ensureCapacity(required, maximumSlots);
            int write = firstSlot;
            double airOffset = initialAirTemperatureC - referenceTemperatureC;
            if (layout.airKind == ThermalBrickCellLayout.AirKind.REGULAR) {
                double capacity = finiteProduct(
                        "regular air capacity",
                        layout.airCapacityJPerBlockK,
                        64.0D);
                writeRegularCell(
                        write++,
                        pageSlot,
                        lifecycleGeneration,
                        layout.minX,
                        layout.minY,
                        layout.minZ,
                        capacity,
                        finiteProduct("initial enthalpy", capacity, airOffset));
            } else if (layout.airKind == ThermalBrickCellLayout.AirKind.MIXED) {
                int support = write;
                mixedBrickGeometries[support] = layout.mixedGeometry;
                for (int component = 0;
                     component < layout.mixedGeometry.componentCount();
                     component++) {
                    double capacity = finiteProduct(
                            "mixed component capacity",
                            layout.airCapacityJPerBlockK,
                            layout.mixedGeometry.componentVolume(component));
                    writeMixedComponent(
                            write++,
                            pageSlot,
                            lifecycleGeneration,
                            support,
                            layout.minX,
                            layout.minY,
                            layout.minZ,
                            component,
                            capacity,
                            finiteProduct(
                                    "initial enthalpy", capacity, airOffset));
                }
            }

            int[] materialSlots = layout.materialCount == 0
                    ? NO_SLOTS : new int[layout.materialCount];
            for (int index = 0; index < layout.materialCount; index++) {
                materialSlots[index] = write;
                double offset = layout.materialInitialTemperatureC[index]
                        - referenceTemperatureC;
                writeMaterialPole(
                        write++,
                        pageSlot,
                        lifecycleGeneration,
                        layout.materialBlockX[index],
                        layout.materialBlockY[index],
                        layout.materialBlockZ[index],
                        layout.materialCapacityJPerK[index],
                        finiteProduct(
                                "initial material enthalpy",
                                layout.materialCapacityJPerK[index],
                                offset));
            }

            int[] phaseSlots = layout.phaseCount == 0
                    ? NO_SLOTS : new int[layout.phaseCount];
            for (int index = 0; index < layout.phaseCount; index++) {
                phaseSlots[index] = write;
                writePhaseReservoir(
                        write++,
                        pageSlot,
                        lifecycleGeneration,
                        layout.phaseBrickMinX[index],
                        layout.phaseBrickMinY[index],
                        layout.phaseBrickMinZ[index],
                        layout.phaseProfileId[index],
                        layout.phaseCandidateMask[index],
                        layout.phaseTransitionTemperatureC[index],
                        layout.phaseTransitionEnergyJPerUnit[index]);
            }
            if (write != required) {
                throw new IllegalStateException(
                        "staged Brick cell count changed during construction");
            }
            return new BrickAllocation(
                    new ArenaSpan(firstSlot, totalCells),
                    materialSlots,
                    phaseSlots);
        } catch (RuntimeException | Error failure) {
            clearRange(firstSlot, required);
            addFreeSpan(firstSlot, totalCells);
            throw failure;
        }
    }

    public boolean ownsStagedCells(ArenaSpan span) {
        return ownsSpan(span, RESERVED, NO_SLOT, 0, false);
    }

    /** Allocation-free authoritative state transition after global preflight. */
    public void commitStagedCells(ArenaSpan span) {
        if (span.count() == 0) {
            return;
        }
        int nextLiveCellCount = Math.addExact(
                liveCellCount, span.count());
        int nextHighWaterMark = Math.max(
                highWaterMark, span.endSlotExclusive());
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            allocationState[slot] = LIVE;
            liveSlots[slot >>> 6] |= 1L << slot;
        }
        setLiveWordSummary(span.firstSlot(), span.endSlotExclusive());
        highWaterMark = nextHighWaterMark;
        liveCellCount = nextLiveCellCount;
    }

    public void discardStagedCells(ArenaSpan span) {
        if (span.count() == 0) {
            return;
        }
        if (!ownsStagedCells(span)) {
            throw new IllegalStateException("Brick staging span is not reserved");
        }
        clearRange(span.firstSlot(), span.endSlotExclusive());
        addFreeSpan(span.firstSlot(), span.count());
    }

    public void stageEnthalpyJ(int slot, double value) {
        requireFinite("enthalpyJ", value);
        requireReservedSlot(slot);
        enthalpyJ[slot] = value;
    }

    public double enthalpyJ(int slot) {
        requireAllocatedSlot(slot);
        return enthalpyJ[slot];
    }

    public double capacityJPerK(int slot) {
        requireAllocatedSlot(slot);
        return capacityJPerK[slot];
    }

    public double inverseCapacityKPerJ(int slot) {
        requireAllocatedSlot(slot);
        return inverseCapacityKPerJ[slot];
    }

    public double temperatureC(int slot, double referenceTemperatureC) {
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireLiveSlot(slot);
        return cellKinds[slot] == PHASE_RESERVOIR
                ? phases.transitionTemperatureC(slot)
                : referenceTemperatureC
                        + enthalpyJ[slot] * inverseCapacityKPerJ[slot];
    }

    public void setEnthalpyJ(int slot, double value) {
        requireFinite("enthalpyJ", value);
        requireLiveSlot(slot);
        enthalpyJ[slot] = value;
    }

    public void addEnthalpyJ(int slot, double deltaJ) {
        requireFinite("deltaJ", deltaJ);
        requireLiveSlot(slot);
        double result = enthalpyJ[slot] + deltaJ;
        requireFinite("updated enthalpy", result);
        enthalpyJ[slot] = result;
    }

    public int lifecycleGeneration(int slot) {
        requireAllocatedSlot(slot);
        return lifecycleGenerations[slot];
    }

    /** Adds energy only when the source binding still names this exact cell incarnation. */
    public void addNodeEnthalpyJ(long nodeId, int lifecycleGeneration, double deltaJ) {
        int slot = requireNodeTarget(nodeId, lifecycleGeneration);
        addEnthalpyJ(slot, deltaJ);
    }

    private int requireNodeTarget(long nodeId, int lifecycleGeneration) {
        if (nodeId < 0L || nodeId > Integer.MAX_VALUE) {
            throw new IllegalStateException("thermal node ID is not an arena slot: " + nodeId);
        }
        int slot = (int) nodeId;
        requireLiveSlot(slot);
        if (lifecycleGenerations[slot] != lifecycleGeneration) {
            throw new IllegalStateException(
                    "stale thermal node generation for slot " + slot);
        }
        return slot;
    }

    public int minimum(int slot, int axis) {
        requireAllocatedSlot(slot);
        return switch (axis) {
            case 0 -> minimumX[slot];
            case 1 -> minimumY[slot];
            case 2 -> minimumZ[slot];
            default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
        };
    }

    public boolean isMixedComponent(int slot) {
        requireAllocatedSlot(slot);
        return cellKinds[slot] == MIXED_COMPONENT;
    }

    public boolean isMaterialPole(int slot) {
        requireAllocatedSlot(slot);
        return isMaterialKind(cellKinds[slot]);
    }

    public boolean isPhaseReservoir(int slot) {
        requireAllocatedSlot(slot);
        return cellKinds[slot] == PHASE_RESERVOIR;
    }

    public int phaseProfileId(int slot) {
        requirePhaseReservoir(slot);
        return phases.profileId(slot);
    }

    public long phaseCandidateMask(int slot) {
        requirePhaseReservoir(slot);
        return phases.candidateMask(slot);
    }

    public double phaseTransitionTemperatureC(int slot) {
        requirePhaseReservoir(slot);
        return phases.transitionTemperatureC(slot);
    }

    public double phaseTransitionEnergyJPerUnit(int slot) {
        requirePhaseReservoir(slot);
        return phases.transitionEnergyJPerUnit(slot);
    }

    public double phaseAvailableEnergyJ(int slot) {
        requirePhaseReservoir(slot);
        return phases.availableEnergyJ(slot, enthalpyJ[slot]);
    }

    public double phaseMaximumEnergyJ(int slot) {
        requirePhaseReservoir(slot);
        return phases.maximumEnergyJ(slot);
    }

    public boolean phaseRequestOutstanding(int slot) {
        requirePhaseReservoir(slot);
        return phases.requestOutstanding(slot);
    }

    public boolean phaseRequestNeedsOffer(int slot) {
        requirePhaseReservoir(slot);
        return phases.requestNeedsOffer(slot);
    }

    public long phaseRequestSequence(int slot) {
        requirePhaseReservoir(slot);
        return phases.requestSequence(slot);
    }

    public int phaseRequestCandidateBit(int slot) {
        requirePhaseReservoir(slot);
        return phases.requestCandidateBit(slot);
    }

    public void beginPhaseRequest(
            int slot,
            long requestSequence,
        int candidateBit
    ) {
        requirePhaseReservoir(slot);
        phases.beginRequest(
                slot, requestSequence, candidateBit, enthalpyJ[slot]);
    }

    public void markPhaseRequestEnqueued(int slot, long requestSequence) {
        requirePhaseReservoir(slot);
        phases.markRequestEnqueued(slot, requestSequence);
    }

    public void retryPhaseRequest(int slot, long requestSequence) {
        requirePhaseReservoir(slot);
        phases.retryRequest(slot, requestSequence);
    }

    public double completePhaseRequest(
            int slot,
            long requestSequence,
            boolean mutationApplied
    ) {
        requirePhaseReservoir(slot);
        double consumed = phases.completeRequest(
                slot, requestSequence, mutationApplied, enthalpyJ[slot]);
        if (consumed != 0.0D) {
            enthalpyJ[slot] = Math.max(0.0D, enthalpyJ[slot] - consumed);
        }
        return consumed;
    }

    public void copyPhaseRequestState(int oldSlot, int newSlot) {
        requirePhaseReservoir(oldSlot);
        requirePhaseReservoir(newSlot);
        if (phases.profileId(oldSlot) != phases.profileId(newSlot)
                || minimumX[oldSlot] != minimumX[newSlot]
                || minimumY[oldSlot] != minimumY[newSlot]
                || minimumZ[oldSlot] != minimumZ[newSlot]) {
            throw new IllegalArgumentException("phase reservoir ownership key changed");
        }
        phases.copyRequest(oldSlot, newSlot);
    }

    private boolean isMixedSupport(int supportRef) {
        return isAllocated(supportRef)
                && cellKinds[supportRef] == MIXED_COMPONENT
                && supportRefs[supportRef] == supportRef
                && mixedBrickGeometries[supportRef] != null;
    }

    public double center(int slot, int axis) {
        requireAllocatedSlot(slot);
        int origin = switch (axis) {
            case 0 -> minimumX[slot];
            case 1 -> minimumY[slot];
            case 2 -> minimumZ[slot];
            default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
        };
        if (isMaterialKind(cellKinds[slot])) {
            return origin + 0.5D;
        }
        if (cellKinds[slot] == REGULAR_CELL) {
            return origin + 2.0D;
        }
        ComponentBrickCompiler.CompiledBrick geometry = mixedGeometry(supportRefs[slot]);
        int component = mixedComponentIds[slot];
        return origin + switch (axis) {
            case 0 -> geometry.componentCentroidX(component);
            case 1 -> geometry.componentCentroidY(component);
            case 2 -> geometry.componentCentroidZ(component);
            default -> throw new IllegalArgumentException("axis must be 0, 1, or 2");
        };
    }

    ComponentBrickCompiler.CompiledBrick mixedGeometry(int supportRef) {
        if (!isMixedSupport(supportRef)) {
            throw new IllegalArgumentException("slot is not a mixed-Brick support: " + supportRef);
        }
        return mixedBrickGeometries[supportRef];
    }

    /** Releases one retired Page span after its replacement sweep is installed. */
    public void releasePageCells(
            int expectedPageSlot,
            int expectedLifecycleGeneration,
            ArenaSpan span
    ) {
        if (span == null) {
            throw new IllegalArgumentException("span is required");
        }
        if (span.count() == 0) {
            return;
        }
        requireLifecycleGeneration(expectedLifecycleGeneration);
        if (!ownsLiveCells(
                expectedPageSlot, expectedLifecycleGeneration, span)) {
            throw new IllegalArgumentException(
                    "Page release does not own the complete arena span");
        }
        releaseSpan(span);
    }

    public boolean ownsLiveCells(
            int expectedPageSlot,
            int expectedLifecycleGeneration,
            ArenaSpan span
    ) {
        return ownsSpan(
                span,
                LIVE,
                expectedPageSlot,
                expectedLifecycleGeneration,
                true);
    }

    private int findFreeSpan(int count) {
        long fit = freeSpans.takeBestFit(count);
        if (fit == ThermalFreeSpanIndex.NO_SPAN) {
            return allocationHighWaterMark;
        }
        int spanLength = (int) (fit >>> 32);
        int firstSlot = (int) fit;
        if (spanLength > count) {
            addFreeSpan(firstSlot + count, spanLength - count);
        }
        return firstSlot;
    }

    private void addFreeSpan(int firstSlot, int count) {
        if (count <= 0 || firstSlot < 0
                || firstSlot + count > allocationHighWaterMark) {
            throw new IllegalArgumentException("free arena span is invalid");
        }
        allocationHighWaterMark = freeSpans.addAndMerge(
                firstSlot, count, allocationHighWaterMark);
    }

    private void ensureCapacity(int requiredCapacity, int maximumCapacity) {
        if (requiredCapacity <= allocationState.length) {
            return;
        }
        int oldCapacity = allocationState.length;
        int grown = Math.max(8, oldCapacity);
        while (grown < requiredCapacity) {
            int next = grown + Math.max(8, grown >>> 1);
            if (next < 0 || next < grown) {
                grown = Integer.MAX_VALUE;
                break;
            }
            grown = next;
        }
        grown = Math.min(grown, maximumCapacity);
        if (grown < requiredCapacity) {
            throw new IllegalStateException("thermal cell arena exceeded int address space");
        }
        enthalpyJ = Arrays.copyOf(enthalpyJ, grown);
        capacityJPerK = Arrays.copyOf(capacityJPerK, grown);
        inverseCapacityKPerJ = Arrays.copyOf(inverseCapacityKPerJ, grown);
        pageSlots = Arrays.copyOf(pageSlots, grown);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, grown);
        supportRefs = Arrays.copyOf(supportRefs, grown);
        minimumX = Arrays.copyOf(minimumX, grown);
        minimumY = Arrays.copyOf(minimumY, grown);
        minimumZ = Arrays.copyOf(minimumZ, grown);
        cellKinds = Arrays.copyOf(cellKinds, grown);
        mixedComponentIds = Arrays.copyOf(mixedComponentIds, grown);
        mixedBrickGeometries = Arrays.copyOf(mixedBrickGeometries, grown);
        phases.ensureCapacity(grown);
        allocationState = Arrays.copyOf(allocationState, grown);
        liveSlots = Arrays.copyOf(liveSlots, (grown + 63) >>> 6);
        liveWordSummary = Arrays.copyOf(
                liveWordSummary, (liveSlots.length + 63) >>> 6);
        Arrays.fill(pageSlots, oldCapacity, grown, NO_SLOT);
        Arrays.fill(supportRefs, oldCapacity, grown, NO_SLOT);
        Arrays.fill(mixedComponentIds, oldCapacity, grown, NO_SLOT);
    }

    private void releaseSpan(ArenaSpan span) {
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            liveSlots[slot >>> 6] &= ~(1L << slot);
        }
        clearRange(span.firstSlot(), span.endSlotExclusive());
        clearEmptyLiveWordSummary(
                span.firstSlot(), span.endSlotExclusive());
        liveCellCount -= span.count();
        recomputeHighWaterMark();
        addFreeSpan(span.firstSlot(), span.count());
    }

    private boolean isAllocated(int slot) {
        return slot >= 0 && slot < allocationHighWaterMark
                && allocationState[slot] != FREE;
    }

    private boolean ownsSpan(
            ArenaSpan span,
            byte expectedState,
            int expectedPageSlot,
            int expectedLifecycleGeneration,
            boolean validateOwner
    ) {
        if (span == null || span.firstSlot() < 0
                || span.endSlotExclusive() > allocationHighWaterMark) {
            return false;
        }
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (allocationState[slot] != expectedState
                    || validateOwner
                    && (pageSlots[slot] != expectedPageSlot
                    || lifecycleGenerations[slot]
                            != expectedLifecycleGeneration)) {
                return false;
            }
        }
        return true;
    }

    private void setLiveWordSummary(int firstSlot, int endSlotExclusive) {
        int firstWord = firstSlot >>> 6;
        int lastWord = (endSlotExclusive - 1) >>> 6;
        for (int word = firstWord; word <= lastWord; word++) {
            liveWordSummary[word >>> 6] |= 1L << (word & 63);
        }
    }

    private void clearEmptyLiveWordSummary(
            int firstSlot,
            int endSlotExclusive
    ) {
        int firstWord = firstSlot >>> 6;
        int lastWord = Math.min(
                liveSlots.length - 1,
                Math.max(firstWord, (endSlotExclusive - 1) >>> 6));
        for (int word = firstWord; word <= lastWord; word++) {
            if (liveSlots[word] == 0L) {
                liveWordSummary[word >>> 6] &= ~(1L << (word & 63));
            }
        }
    }

    private void recomputeHighWaterMark() {
        int word = previousOccupiedWord(liveSlots.length - 1);
        while (word >= 0) {
            long live = liveSlots[word];
            if (live != 0L) {
                highWaterMark = (word << 6)
                        + Long.SIZE - Long.numberOfLeadingZeros(live);
                return;
            }
            word = previousOccupiedWord(word - 1);
        }
        highWaterMark = 0;
    }

    private int nextOccupiedWord(int fromInclusive) {
        if (fromInclusive < 0 || fromInclusive >= liveSlots.length) {
            return NO_SLOT;
        }
        int summary = fromInclusive >>> 6;
        long word = liveWordSummary[summary]
                & (-1L << (fromInclusive & 63));
        while (true) {
            if (word != 0L) {
                int result = (summary << 6)
                        + Long.numberOfTrailingZeros(word);
                return result < liveSlots.length ? result : NO_SLOT;
            }
            if (++summary >= liveWordSummary.length) {
                return NO_SLOT;
            }
            word = liveWordSummary[summary];
        }
    }

    private int previousOccupiedWord(int fromInclusive) {
        if (fromInclusive < 0 || liveWordSummary.length == 0) {
            return NO_SLOT;
        }
        int summary = Math.min(
                fromInclusive >>> 6, liveWordSummary.length - 1);
        int bit = Math.min(fromInclusive & 63, 63);
        long word = liveWordSummary[summary]
                & (-1L >>> (63 - bit));
        while (true) {
            if (word != 0L) {
                return summary << 6
                        | 63 - Long.numberOfLeadingZeros(word);
            }
            if (--summary < 0) {
                return NO_SLOT;
            }
            word = liveWordSummary[summary];
        }
    }

    private void clearRange(int firstSlot, int endSlotExclusive) {
        int end = Math.min(endSlotExclusive, allocationState.length);
        for (int slot = firstSlot; slot < end; slot++) {
            allocationState[slot] = FREE;
            enthalpyJ[slot] = 0.0D;
            capacityJPerK[slot] = 0.0D;
            inverseCapacityKPerJ[slot] = 0.0D;
            pageSlots[slot] = NO_SLOT;
            lifecycleGenerations[slot] = 0;
            supportRefs[slot] = NO_SLOT;
            minimumX[slot] = 0;
            minimumY[slot] = 0;
            minimumZ[slot] = 0;
            cellKinds[slot] = REGULAR_CELL;
            mixedComponentIds[slot] = NO_SLOT;
            mixedBrickGeometries[slot] = null;
            phases.clear(slot);
        }
    }

    private void requireLiveSlot(int slot) {
        if (!isLive(slot)) {
            throw new IllegalArgumentException("cell slot is not live: " + slot);
        }
    }

    private void requireAllocatedSlot(int slot) {
        if (!isAllocated(slot)) {
            throw new IllegalArgumentException(
                    "cell slot is not allocated: " + slot);
        }
    }

    private void requireReservedSlot(int slot) {
        if (slot < 0 || slot >= allocationHighWaterMark
                || allocationState[slot] != RESERVED) {
            throw new IllegalArgumentException(
                    "cell slot is not staged: " + slot);
        }
    }

    private void requirePhaseReservoir(int slot) {
        requireAllocatedSlot(slot);
        if (cellKinds[slot] != PHASE_RESERVOIR) {
            throw new IllegalArgumentException("slot is not a phase reservoir: " + slot);
        }
    }

    private void writeRegularCell(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int minX,
            int minY,
            int minZ,
            double capacity,
            double initialEnthalpyJ
    ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = initialEnthalpyJ;
        writeCapacity(slot, capacity);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = minX;
        minimumY[slot] = minY;
        minimumZ[slot] = minZ;
        cellKinds[slot] = REGULAR_CELL;
        mixedComponentIds[slot] = NO_SLOT;
        mixedBrickGeometries[slot] = null;
    }

    private void writeMixedComponent(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int supportRef,
            int minX,
            int minY,
            int minZ,
            int componentId,
            double capacity,
            double initialEnthalpyJ
    ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = initialEnthalpyJ;
        writeCapacity(slot, capacity);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = supportRef;
        minimumX[slot] = minX;
        minimumY[slot] = minY;
        minimumZ[slot] = minZ;
        cellKinds[slot] = MIXED_COMPONENT;
        mixedComponentIds[slot] = componentId;
    }

    private void writeMaterialPole(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int blockX,
            int blockY,
            int blockZ,
            double capacity,
            double initialEnthalpyJ
    ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = initialEnthalpyJ;
        writeCapacity(slot, capacity);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = blockX;
        minimumY[slot] = blockY;
        minimumZ[slot] = blockZ;
        cellKinds[slot] = MATERIAL_CELL;
        mixedComponentIds[slot] = NO_SLOT;
        mixedBrickGeometries[slot] = null;
    }

    private void writePhaseReservoir(
            int slot,
            int pageSlot,
            int lifecycleGeneration,
            int brickMinX,
            int brickMinY,
            int brickMinZ,
            int materialProfileId,
            long candidateMask,
            double transitionTemperatureC,
            double transitionEnergyJPerUnit
    ) {
        allocationState[slot] = RESERVED;
        enthalpyJ[slot] = 0.0D;
        writeCapacity(slot, transitionEnergyJPerUnit);
        pageSlots[slot] = pageSlot;
        lifecycleGenerations[slot] = lifecycleGeneration;
        supportRefs[slot] = slot;
        minimumX[slot] = brickMinX;
        minimumY[slot] = brickMinY;
        minimumZ[slot] = brickMinZ;
        cellKinds[slot] = PHASE_RESERVOIR;
        mixedComponentIds[slot] = NO_SLOT;
        mixedBrickGeometries[slot] = null;
        phases.write(
                slot,
                materialProfileId,
                candidateMask,
                transitionTemperatureC,
                transitionEnergyJPerUnit);
    }

    private void writeCapacity(int slot, double capacity) {
        if (!Double.isFinite(capacity) || capacity <= 0.0D) {
            throw new IllegalArgumentException("cell capacity must be finite and positive");
        }
        double inverse = 1.0D / capacity;
        if (!Double.isFinite(inverse) || inverse <= 0.0D) {
            throw new IllegalArgumentException("inverse cell capacity is invalid");
        }
        capacityJPerK[slot] = capacity;
        inverseCapacityKPerJ[slot] = inverse;
    }

    private static double finiteProduct(String name, double left, double right) {
        double result = left * right;
        requireFinite(name, result);
        return result;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireLifecycleGeneration(int lifecycleGeneration) {
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
    }

    public record BrickAllocation(
            ArenaSpan cellSpan,
            int[] materialPoleSlots,
            int[] phaseReservoirSlots
    ) {
        private static final BrickAllocation EMPTY = new BrickAllocation(
                ArenaSpan.EMPTY, NO_SLOTS, NO_SLOTS);
    }

    private static boolean isMaterialKind(byte kind) {
        return kind == MATERIAL_CELL;
    }
}
