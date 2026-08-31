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
    private final long[] pageChangeIds;
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
    private long temperatureChangeId;
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
        pageChangeIds = new long[maximumPages];
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
                dimensionBudget.tryReserve(Math.multiplyExact(
                        maximumPages, (long) Long.BYTES));
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

    /** Writes every live arena slot exactly once into the inactive buffer. */
    public synchronized boolean publish(
            ThermalCellArena arena,
            double referenceTemperatureC,
            long topologyGeneration,
            long sampleTick
    ) {
        return publish(
                arena, referenceTemperatureC,
                topologyGeneration, sampleTick, null);
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
                || !acceptingPublications
                || !beginWrite()) {
            return false;
        }

        int targetBuffer = publishedBufferIndex == 0 ? 1 : 0;
        double[] targetTemperatures = temperaturesC[targetBuffer];
        int[] targetGenerations = slotGenerations[targetBuffer];
        boolean compareInfrared = sampleTick <= infraredActiveUntilTick
                && valid && publishedBufferIndex >= 0;
        double[] previousTemperatures = compareInfrared
                ? temperaturesC[publishedBufferIndex] : null;
        int[] previousGenerations = compareInfrared
                ? slotGenerations[publishedBufferIndex] : null;
        long nextTemperatureChangeId = compareInfrared
                ? Math.incrementExact(temperatureChangeId) : temperatureChangeId;
        boolean infraredChanged = false;
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
                int brick = Math.floorMod(arena.minimum(slot, 0), 16) >>> 2
                        | (Math.floorMod(arena.minimum(slot, 2), 16) >>> 2) << 2
                        | (Math.floorMod(arena.minimum(slot, 1), 16) >>> 2) << 4;
                hotMasks.record(pageSlot, brick, temperature);
            }
            if (compareInfrared && arena.isAirCell(slot)
                    && (previousGenerations[slot] != generation
                    || quantizedInfrared(previousTemperatures[slot])
                    != quantizedInfrared(temperature))) {
                int pageSlot = arena.pageSlot(slot);
                requirePageSlot(pageSlot);
                pageChangeIds[pageSlot] = nextTemperatureChangeId;
                infraredChanged = true;
            }
        }
        if (infraredChanged) {
            temperatureChangeId = nextTemperatureChangeId;
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
        long deadline = Math.addExact(gameTick, activeTicks);
        boolean reactivated = gameTick > infraredActiveUntilTick;
        infraredActiveUntilTick = Math.max(infraredActiveUntilTick, deadline);
        if (!reactivated || !beginWrite()) {
            return false;
        }
        long next = Math.incrementExact(temperatureChangeId);
        Arrays.fill(pageChangeIds, next);
        temperatureChangeId = next;
        endWrite();
        return true;
    }

    /** Marks one topology-owned Page as changed while infrared tracking is active. */
    public synchronized void markInfraredPageChanged(
            int pageSlot,
            long sampleTick
    ) {
        requirePageSlot(pageSlot);
        requireNonNegative("sampleTick", sampleTick);
        if (sampleTick > infraredActiveUntilTick || !beginWrite()) {
            return;
        }
        long next = Math.incrementExact(temperatureChangeId);
        pageChangeIds[pageSlot] = next;
        temperatureChangeId = next;
        endWrite();
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
            long readTemperatureChangeId = temperatureChangeId;
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
                        readTemperatureChangeId,
                        readTemperatures,
                        readGenerations,
                        pageChangeIds);
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
        private long temperatureChangeId;
        private double[] temperaturesC;
        private int[] slotGenerations;
        private long[] pageChangeIds;

        public boolean valid() { return valid; }
        public long sampleTick() { return sampleTick; }
        public long temperatureChangeId() { return temperatureChangeId; }

        public long pageChangeId(int pageSlot) {
            if (pageSlot < 0 || pageChangeIds == null
                    || pageSlot >= pageChangeIds.length) {
                throw new IllegalArgumentException("Page slot is outside the read cursor");
            }
            return pageChangeIds[pageSlot];
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
                long temperatureChangeId,
                double[] temperaturesC,
                int[] slotGenerations,
                long[] pageChangeIds
        ) {
            this.owner = owner;
            this.version = version;
            this.valid = valid;
            this.capacity = capacity;
            this.topologyGeneration = topologyGeneration;
            this.sampleTick = sampleTick;
            this.temperatureChangeId = temperatureChangeId;
            this.temperaturesC = temperaturesC;
            this.slotGenerations = slotGenerations;
            this.pageChangeIds = pageChangeIds;
        }

        private void clear() {
            owner = null;
            version = -1L;
            valid = false;
            capacity = 0;
            topologyGeneration = -1L;
            sampleTick = -1L;
            temperatureChangeId = 0L;
            temperaturesC = null;
            slotGenerations = null;
            pageChangeIds = null;
        }
    }
}
