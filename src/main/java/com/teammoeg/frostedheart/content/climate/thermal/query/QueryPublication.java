/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.query;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;

import java.util.Arrays;

/**
 * 由一个维度 worker 写、主线程无锁读取的 arena-slot 双缓冲。
 *
 * <p>slot generation 与 topology generation 共同拒绝过期读；该结构只发布
 * 查询温度，不拥有 Page topology 或求解器状态。</p>
 */
public final class QueryPublication implements AutoCloseable {
    private final ThermalMemoryBudget budget;
    private final int maximumPages;
    private int[] pageChangeEpochs;
    private int[] brickChangeEpochs;
    private long[] pendingInfraredBrickMasks;
    private double[][] temperaturesC;
    private int[][] slotGenerations;
    private ThermalMemoryBudget.Reservation cellReservation;
    private ThermalMemoryBudget.Reservation pageReservation;

    private int capacity;
    private boolean acceptingPublications = true;
    private boolean valid;
    private int publishedBufferIndex = -1;
    private long topologyGeneration = -1L;
    private long sampleTick = -1L;
    private int infraredEpoch;
    private long infraredActiveUntilTick = -1L;
    private volatile long publicationVersion;

    private QueryPublication(
            ThermalMemoryBudget budget,
            int capacity,
            int maximumPages,
            ThermalMemoryBudget.Reservation cellReservation,
            ThermalMemoryBudget.Reservation pageReservation
    ) {
        this.budget = budget;
        this.capacity = capacity;
        this.maximumPages = maximumPages;
        this.cellReservation = cellReservation;
        this.pageReservation = pageReservation;
        allocateBuffers(capacity);
    }

    /** Returns {@code null} when the complete double buffer cannot be admitted. */
    public static QueryPublication tryCreate(
            ThermalMemoryBudget dimensionBudget,
            int capacity,
            int maximumPages
    ) {
        if (dimensionBudget == null) {
            throw new IllegalArgumentException("dimensionBudget is required");
        }
        if (maximumPages <= 0) {
            throw new IllegalArgumentException("maximumPages must be positive");
        }
        ThermalMemoryBudget.Reservation pageReservation =
                dimensionBudget.tryReserve(projectedPagePayloadBytes(
                        maximumPages));
        if (pageReservation == null) {
            return null;
        }
        ThermalMemoryBudget.Reservation cellReservation =
                dimensionBudget.tryReserve(projectedPayloadBytes(capacity));
        if (cellReservation == null) {
            pageReservation.close();
            return null;
        }
        return new QueryPublication(
                dimensionBudget, capacity, maximumPages,
                cellReservation, pageReservation);
    }

    private static long projectedPayloadBytes(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        return Math.multiplyExact(
                capacity,
                2L * (Double.BYTES + Integer.BYTES));
    }

    private static long projectedPagePayloadBytes(int maximumPages) {
        return Math.multiplyExact(
                maximumPages,
                (long) Double.BYTES
                        + 3L * Long.BYTES
                        + 65L * Integer.BYTES);
    }

    /** Geometrically grows the slot-addressed backing before a topology commit. */
    public synchronized boolean tryEnsureCapacity(
            int requiredCapacity,
            int maximumCapacity
    ) {
        if (maximumCapacity < requiredCapacity) {
            return false;
        }
        if (requiredCapacity <= capacity) {
            return true;
        }
        if (!acceptingPublications) {
            return false;
        }
        int doubled = Math.multiplyExact(capacity, 2);
        int nextCapacity = Math.min(
                maximumCapacity, Math.max(requiredCapacity, doubled));
        ThermalMemoryBudget.Reservation nextReservation = budget.tryReserve(
                projectedPayloadBytes(nextCapacity));
        if (nextReservation == null) {
            return false;
        }

        double[][] nextTemperatures = new double[2][nextCapacity];
        int[][] nextSlotGenerations = new int[2][nextCapacity];
        if (valid && publishedBufferIndex >= 0) {
            System.arraycopy(
                    temperaturesC[publishedBufferIndex], 0,
                    nextTemperatures[publishedBufferIndex], 0, capacity);
            System.arraycopy(
                    slotGenerations[publishedBufferIndex], 0,
                    nextSlotGenerations[publishedBufferIndex], 0, capacity);
        }
        if (!beginWrite()) {
            nextReservation.close();
            return false;
        }
        ThermalMemoryBudget.Reservation previous = cellReservation;
        temperaturesC = nextTemperatures;
        slotGenerations = nextSlotGenerations;
        capacity = nextCapacity;
        cellReservation = nextReservation;
        endWrite();
        previous.close();
        return true;
    }

    public synchronized boolean publish(
            ThermalCellArena arena,
            double referenceTemperatureC,
            long topologyGeneration,
            long sampleTick,
            HotMaskScratch hotMasks
    ) {
        if (arena == null) {
            throw new IllegalArgumentException("arena is required");
        }
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireNonNegative("topologyGeneration", topologyGeneration);
        requireNonNegative("sampleTick", sampleTick);
        if (arena.highWaterMark() > capacity
                || !acceptingPublications) {
            return false;
        }

        int targetBuffer = publishedBufferIndex == 0 ? 1 : 0;
        double[] targetTemperatures = temperaturesC[targetBuffer];
        int[] targetGenerations = slotGenerations[targetBuffer];
        boolean infraredTracking = pageChangeEpochs != null
                && sampleTick <= infraredActiveUntilTick;
        boolean compareInfrared = infraredTracking
                && valid && publishedBufferIndex >= 0;
        double[] previousTemperatures = compareInfrared
                ? temperaturesC[publishedBufferIndex] : null;
        int[] previousGenerations = compareInfrared
                ? slotGenerations[publishedBufferIndex] : null;
        boolean infraredChanged = infraredTracking
                && hasPendingInfraredChanges();
        if (hotMasks != null) {
            hotMasks.begin();
        }
        for (int slot = arena.nextLiveSlot(0);
             slot >= 0;
             slot = arena.nextLiveSlot(slot + 1)) {
            double temperature = arena.temperatureC(
                    slot, referenceTemperatureC);
            int generation = arena.lifecycleGeneration(slot);
            targetTemperatures[slot] = temperature;
            targetGenerations[slot] = generation;
            if (hotMasks != null) {
                int pageSlot = arena.pageSlot(slot);
                int brick = brickIndex(arena, slot);
                if (arena.isPhaseReservoir(slot)) {
                    if (arena.enthalpyJ(slot) > 0.0D) {
                        hotMasks.recordHot(pageSlot, brick);
                    }
                } else {
                    hotMasks.record(pageSlot, brick, temperature);
                }
            }
            if (compareInfrared && arena.isAirCell(slot)
                    && (previousGenerations[slot] != generation
                    || quantizedInfrared(previousTemperatures[slot])
                    != quantizedInfrared(temperature))) {
                int pageSlot = arena.pageSlot(slot);
                requirePageSlot(pageSlot);
                pendingInfraredBrickMasks[pageSlot] |=
                        1L << brickIndex(arena, slot);
                infraredChanged = true;
            }
        }
        if (!beginWrite()) {
            return false;
        }
        if (infraredChanged) {
            commitInfraredChanges();
        }
        this.topologyGeneration = topologyGeneration;
        this.sampleTick = sampleTick;
        publishedBufferIndex = targetBuffer;
        valid = true;
        endWrite();
        return true;
    }

    /** Reusable Page-slot scratch populated during the existing live-slot pass. */
    public static final class HotMaskScratch {
        private final double[] naturalTemperatureC;
        private long[] previousHotMask;
        private long[] nextHotMask;
        private double refineHighC;
        private double releaseLowC;

        public HotMaskScratch(int maximumPages) {
            if (maximumPages <= 0) {
                throw new IllegalArgumentException("maximumPages must be positive");
            }
            naturalTemperatureC = new double[maximumPages];
            previousHotMask = new long[maximumPages];
            nextHotMask = new long[maximumPages];
        }

        public void configure(double refineHighC, double releaseLowC) {
            if (!Double.isFinite(refineHighC)
                    || !Double.isFinite(releaseLowC)
                    || refineHighC <= releaseLowC || releaseLowC < 0.0D) {
                throw new IllegalArgumentException("hot-mask thresholds are invalid");
            }
            this.refineHighC = refineHighC;
            this.releaseLowC = releaseLowC;
        }

        public void installPage(int pageSlot, double naturalTemperatureC) {
            requirePageSlot(pageSlot);
            requireFinite("naturalTemperatureC", naturalTemperatureC);
            this.naturalTemperatureC[pageSlot] = naturalTemperatureC;
            previousHotMask[pageSlot] = 0L;
            nextHotMask[pageSlot] = 0L;
        }

        public void updateNaturalTemperature(
                int pageSlot,
                double naturalTemperatureC
        ) {
            requirePageSlot(pageSlot);
            requireFinite("naturalTemperatureC", naturalTemperatureC);
            this.naturalTemperatureC[pageSlot] = naturalTemperatureC;
        }

        public void removePage(int pageSlot) {
            requirePageSlot(pageSlot);
            previousHotMask[pageSlot] = 0L;
            nextHotMask[pageSlot] = 0L;
        }

        private void begin() {
            Arrays.fill(nextHotMask, 0L);
        }

        private void record(int pageSlot, int brick, double temperatureC) {
            requirePageSlot(pageSlot);
            long bit = 1L << brick;
            double threshold = (previousHotMask[pageSlot] & bit) != 0L
                    ? releaseLowC : refineHighC;
            if (Math.abs(temperatureC - naturalTemperatureC[pageSlot])
                    >= threshold) {
                nextHotMask[pageSlot] |= bit;
            }
        }

        private void recordHot(int pageSlot, int brick) {
            requirePageSlot(pageSlot);
            nextHotMask[pageSlot] |= 1L << brick;
        }

        public long hotMask(int pageSlot) {
            requirePageSlot(pageSlot);
            return nextHotMask[pageSlot];
        }

        public void finish() {
            long[] previous = previousHotMask;
            previousHotMask = nextHotMask;
            nextHotMask = previous;
        }

        private void requirePageSlot(int pageSlot) {
            if (pageSlot < 0 || pageSlot >= naturalTemperatureC.length) {
                throw new IllegalArgumentException("Page slot is out of range");
            }
        }
    }

    /**
     * Extends dimension-wide infrared tracking and returns whether it was
     * reactivated after the previous window expired.
     */
    public synchronized boolean noteInfraredRequest(
            long gameTick,
            int activeTicks
    ) {
        requireNonNegative("gameTick", gameTick);
        if (activeTicks <= 0) {
            throw new IllegalArgumentException("activeTicks must be positive");
        }
        if (!acceptingPublications) {
            return false;
        }
        long deadline = Math.addExact(gameTick, activeTicks);
        boolean reactivated = gameTick > infraredActiveUntilTick;
        infraredActiveUntilTick = Math.max(infraredActiveUntilTick, deadline);
        int[] newPageEpochs = null;
        int[] newBrickEpochs = null;
        long[] newPendingMasks = null;
        if (reactivated && pageChangeEpochs == null) {
            newPageEpochs = new int[maximumPages];
            newBrickEpochs = new int[Math.multiplyExact(maximumPages, 64)];
            newPendingMasks = new long[maximumPages];
        }
        if (!reactivated || !beginWrite()) {
            return false;
        }
        if (newPageEpochs != null) {
            pageChangeEpochs = newPageEpochs;
            brickChangeEpochs = newBrickEpochs;
            pendingInfraredBrickMasks = newPendingMasks;
        }
        int next = nextInfraredEpoch();
        Arrays.fill(pageChangeEpochs, next);
        Arrays.fill(brickChangeEpochs, next);
        Arrays.fill(pendingInfraredBrickMasks, 0L);
        infraredEpoch = next;
        endWrite();
        return true;
    }

    /** Adds topology-owned Brick changes to the next successful publication. */
    public synchronized void markInfraredBricksChanged(
            int pageSlot,
            long brickMask,
            long sampleTick
    ) {
        requirePageSlot(pageSlot);
        requireNonNegative("sampleTick", sampleTick);
        if (pendingInfraredBrickMasks == null || brickMask == 0L
                || sampleTick > infraredActiveUntilTick) {
            return;
        }
        pendingInfraredBrickMasks[pageSlot] |= brickMask;
    }

    /** Begins one allocation-free coherent infrared read cut. */
    public boolean beginInfraredRead(InfraredReadCursor out) {
        if (out == null) {
            throw new IllegalArgumentException("out is required");
        }
        out.clear();
        for (int attempt = 0; attempt < 2; attempt++) {
            long firstVersion = publicationVersion;
            if ((firstVersion & 1L) != 0L) {
                continue;
            }
            boolean readValid = valid;
            int readBuffer = publishedBufferIndex;
            int readCapacity = capacity;
            long readTopology = topologyGeneration;
            long readSampleTick = sampleTick;
            int readInfraredEpoch = infraredEpoch;
            double[] readTemperatures = readBuffer < 0
                    ? null : temperaturesC[readBuffer];
            int[] readGenerations = readBuffer < 0
                    ? null : slotGenerations[readBuffer];
            long secondVersion = publicationVersion;
            if (firstVersion == secondVersion && (secondVersion & 1L) == 0L) {
                out.set(
                        this,
                        firstVersion,
                        readValid,
                        readCapacity,
                        readTopology,
                        readSampleTick,
                        readInfraredEpoch,
                        readTemperatures,
                        readGenerations,
                        pageChangeEpochs,
                        brickChangeEpochs);
                return true;
            }
        }
        return false;
    }

    /** Advances an unchanged sleeping publication without copying slot values. */
    public synchronized boolean republishUnchanged(
            long topologyGeneration,
            long sampleTick
    ) {
        requireNonNegative("topologyGeneration", topologyGeneration);
        requireNonNegative("sampleTick", sampleTick);
        if (!acceptingPublications
                || !valid
                || this.topologyGeneration != topologyGeneration
                || !beginWrite()) {
            return false;
        }
        this.sampleTick = sampleTick;
        endWrite();
        return true;
    }

    /** Allocation-free O(1) slot lookup with one seqlock retry. */
    public boolean tryRead(
            int arenaSlot,
            int expectedSlotGeneration,
            long minimumTopologyGeneration,
            MutableSample out
    ) {
        if (arenaSlot < 0 || expectedSlotGeneration < 0
                || minimumTopologyGeneration < 0L) {
            throw new IllegalArgumentException("query identity is invalid");
        }
        if (out == null) {
            throw new IllegalArgumentException("out is required");
        }
        out.clear();
        for (int attempt = 0; attempt < 2; attempt++) {
            long firstVersion = publicationVersion;
            if ((firstVersion & 1L) != 0L) {
                continue;
            }
            boolean readValid = valid;
            int readBuffer = publishedBufferIndex;
            int readCapacity = capacity;
            long readTopology = topologyGeneration;
            long readSampleTick = sampleTick;
            if (!readValid
                    || arenaSlot >= readCapacity
                    || readBuffer < 0
                    || readTopology < minimumTopologyGeneration) {
                if (firstVersion == publicationVersion) {
                    return false;
                }
                continue;
            }
            int generation = slotGenerations[readBuffer][arenaSlot];
            double temperature = temperaturesC[readBuffer][arenaSlot];
            long secondVersion = publicationVersion;
            if (firstVersion == secondVersion && (secondVersion & 1L) == 0L) {
                if (generation != expectedSlotGeneration
                        || !Double.isFinite(temperature)) {
                    return false;
                }
                out.set(
                        temperature,
                        readSampleTick);
                return true;
            }
        }
        return false;
    }

    private void invalidateLocked() {
        if (beginWrite()) {
            clearEnvelope();
            endWrite();
        }
    }

    @Override
    public synchronized void close() {
        if (cellReservation == null) {
            return;
        }
        acceptingPublications = false;
        invalidateLocked();
        cellReservation.close();
        cellReservation = null;
        pageReservation.close();
        pageReservation = null;
    }

    private void allocateBuffers(int size) {
        temperaturesC = new double[2][size];
        slotGenerations = new int[2][size];
    }

    private boolean beginWrite() {
        long version = publicationVersion;
        if ((version & 1L) != 0L || version > Long.MAX_VALUE - 2L) {
            acceptingPublications = false;
            valid = false;
            publicationVersion = Long.MAX_VALUE;
            return false;
        }
        publicationVersion = version + 1L;
        return true;
    }

    private void endWrite() {
        publicationVersion++;
    }

    private void clearEnvelope() {
        valid = false;
        publishedBufferIndex = -1;
        topologyGeneration = -1L;
        sampleTick = -1L;
    }

    private boolean hasPendingInfraredChanges() {
        for (long mask : pendingInfraredBrickMasks) {
            if (mask != 0L) {
                return true;
            }
        }
        return false;
    }

    private void commitInfraredChanges() {
        int next = nextInfraredEpoch();
        for (int pageSlot = 0; pageSlot < maximumPages; pageSlot++) {
            long remaining = pendingInfraredBrickMasks[pageSlot];
            if (remaining == 0L) {
                continue;
            }
            pageChangeEpochs[pageSlot] = next;
            int firstBrick = pageSlot << 6;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                brickChangeEpochs[firstBrick + brick] = next;
                remaining &= remaining - 1L;
            }
            pendingInfraredBrickMasks[pageSlot] = 0L;
        }
        infraredEpoch = next;
    }

    private int nextInfraredEpoch() {
        if (infraredEpoch == Integer.MAX_VALUE) {
            Arrays.fill(pageChangeEpochs, 0);
            Arrays.fill(brickChangeEpochs, 0);
            return 1;
        }
        return infraredEpoch + 1;
    }

    private static int brickIndex(ThermalCellArena arena, int slot) {
        return ((arena.minimum(slot, 0) & 15) >>> 2)
                | ((arena.minimum(slot, 2) & 15) >>> 2) << 2
                | ((arena.minimum(slot, 1) & 15) >>> 2) << 4;
    }

    private void requirePageSlot(int pageSlot) {
        if (pageSlot < 0 || pageSlot >= maximumPages) {
            throw new IllegalArgumentException("Page slot is outside the query publication");
        }
    }

    private static short quantizedInfrared(double temperatureC) {
        long value = Math.round(temperatureC * 4.0D);
        return (short) Math.max(
                -32767L, Math.min(32767L, value));
    }

    private static void requireNonNegative(String name, long value) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    public static final class MutableSample {
        private double temperatureC;
        private long sampleTick;

        public double temperatureC() { return temperatureC; }
        public long sampleTick() { return sampleTick; }

        private void set(
                double temperatureC,
                long sampleTick
        ) {
            this.temperatureC = temperatureC;
            this.sampleTick = sampleTick;
        }

        private void clear() {
            temperatureC = Double.NaN;
            sampleTick = -1L;
        }
    }

    /** Caller-owned coherent view of one published infrared/query cut. */
    public static final class InfraredReadCursor {
        private QueryPublication owner;
        private long version;
        private boolean valid;
        private int capacity;
        private long topologyGeneration;
        private long sampleTick;
        private int infraredEpoch;
        private double[] temperaturesC;
        private int[] slotGenerations;
        private int[] pageChangeEpochs;
        private int[] brickChangeEpochs;

        public boolean valid() { return valid; }
        public long sampleTick() { return sampleTick; }
        public int infraredEpoch() { return infraredEpoch; }

        public int pageChangeEpoch(int pageSlot) {
            if (pageSlot < 0 || pageChangeEpochs == null
                    || pageSlot >= pageChangeEpochs.length) {
                throw new IllegalArgumentException("Page slot is outside the read cursor");
            }
            return pageChangeEpochs[pageSlot];
        }

        public int brickChangeEpoch(int pageSlot, int brickIndex) {
            if (pageSlot < 0 || pageChangeEpochs == null
                    || pageSlot >= pageChangeEpochs.length
                    || brickIndex < 0 || brickIndex >= 64) {
                throw new IllegalArgumentException("Brick is outside the read cursor");
            }
            return brickChangeEpochs[(pageSlot << 6) + brickIndex];
        }

        public boolean tryRead(
                int arenaSlot,
                int expectedSlotGeneration,
                long minimumTopologyGeneration,
                MutableSample out
        ) {
            if (arenaSlot < 0 || expectedSlotGeneration < 0
                    || minimumTopologyGeneration < 0L || out == null) {
                throw new IllegalArgumentException("infrared query identity is invalid");
            }
            out.clear();
            if (!valid || temperaturesC == null || slotGenerations == null
                    || arenaSlot >= capacity
                    || topologyGeneration < minimumTopologyGeneration
                    || slotGenerations[arenaSlot] != expectedSlotGeneration) {
                return false;
            }
            double temperature = temperaturesC[arenaSlot];
            if (!Double.isFinite(temperature)) {
                return false;
            }
            out.set(temperature, sampleTick);
            return true;
        }

        public boolean isCurrent() {
            return owner != null
                    && owner.publicationVersion == version
                    && (version & 1L) == 0L;
        }

        private void set(
                QueryPublication owner,
                long version,
                boolean valid,
                int capacity,
                long topologyGeneration,
                long sampleTick,
                int infraredEpoch,
                double[] temperaturesC,
                int[] slotGenerations,
                int[] pageChangeEpochs,
                int[] brickChangeEpochs
        ) {
            this.owner = owner;
            this.version = version;
            this.valid = valid;
            this.capacity = capacity;
            this.topologyGeneration = topologyGeneration;
            this.sampleTick = sampleTick;
            this.infraredEpoch = infraredEpoch;
            this.temperaturesC = temperaturesC;
            this.slotGenerations = slotGenerations;
            this.pageChangeEpochs = pageChangeEpochs;
            this.brickChangeEpochs = brickChangeEpochs;
        }

        private void clear() {
            owner = null;
            version = -1L;
            valid = false;
            capacity = 0;
            topologyGeneration = -1L;
            sampleTick = -1L;
            infraredEpoch = 0;
            temperaturesC = null;
            slotGenerations = null;
            pageChangeEpochs = null;
            brickChangeEpochs = null;
        }
    }
}
