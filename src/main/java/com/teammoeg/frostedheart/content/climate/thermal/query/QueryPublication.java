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

/**
 * 由一个维度 worker 写、主线程无锁读取的 arena-slot 双缓冲。
 *
 * <p>slot generation 与 topology generation 共同拒绝过期读；该结构只发布
 * 查询温度，不拥有 Page topology 或求解器状态。</p>
 */
public final class QueryPublication implements AutoCloseable {
    private final ThermalMemoryBudget budget;
    private double[][] temperaturesC;
    private int[][] slotGenerations;
    private ThermalMemoryBudget.Reservation reservation;

    private int capacity;
    private boolean acceptingPublications = true;
    private boolean valid;
    private int publishedBufferIndex = -1;
    private long topologyGeneration = -1L;
    private long sampleTick = -1L;
    private volatile long publicationVersion;

    private QueryPublication(
            ThermalMemoryBudget budget,
            int capacity,
            ThermalMemoryBudget.Reservation reservation
    ) {
        this.budget = budget;
        this.capacity = capacity;
        this.reservation = reservation;
        allocateBuffers(capacity);
    }

    /** Returns {@code null} when the complete double buffer cannot be admitted. */
    public static QueryPublication tryCreate(
            ThermalMemoryBudget dimensionBudget,
            int capacity
    ) {
        if (dimensionBudget == null) {
            throw new IllegalArgumentException("dimensionBudget is required");
        }
        long bytes = projectedPayloadBytes(capacity);
        ThermalMemoryBudget.Reservation reservation =
                dimensionBudget.tryReserve(bytes);
        return reservation == null
                ? null
                : new QueryPublication(dimensionBudget, capacity, reservation);
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
        ThermalMemoryBudget.Reservation previous = reservation;
        temperaturesC = nextTemperatures;
        slotGenerations = nextSlotGenerations;
        capacity = nextCapacity;
        reservation = nextReservation;
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
        for (int slot = arena.nextLiveSlot(0);
             slot >= 0;
             slot = arena.nextLiveSlot(slot + 1)) {
            targetTemperatures[slot] = arena.temperatureC(
                    slot, referenceTemperatureC);
            targetGenerations[slot] = arena.lifecycleGeneration(slot);
        }
        this.topologyGeneration = topologyGeneration;
        this.sampleTick = sampleTick;
        publishedBufferIndex = targetBuffer;
        valid = true;
        endWrite();
        return true;
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
        if (reservation == null) {
            return;
        }
        acceptingPublications = false;
        invalidateLocked();
        reservation.close();
        reservation = null;
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
}
