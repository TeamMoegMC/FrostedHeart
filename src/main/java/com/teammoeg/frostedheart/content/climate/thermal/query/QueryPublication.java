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
 * Preallocated lock-free thermal projection guarded by a monotonic seqlock.
 * The single dimension writer publishes; query threads only read stable
 * primitive values into caller-owned output.
 */
public final class QueryPublication implements AutoCloseable {
    public static final int NOT_LIVE = 1;

    private final ThermalMemoryBudget budget;
    private double[][] temperaturesC;
    private int[][] mediumIds;
    private int[][] cellFlags;
    private ThermalMemoryBudget.Reservation reservation;

    private int capacity;
    private boolean acceptingPublications = true;
    private boolean valid;
    private int publishedBufferIndex = -1;
    private long lifecycleGeneration = -1L;
    private long geometryRevision = -1L;
    private long topologyGeneration = -1L;
    private long solveEpoch = -1L;
    private long sampleTick = -1L;
    private int slotStart;
    private int slotCount;
    private volatile long publicationVersion;

    private QueryPublication(
            ThermalMemoryBudget budget,
            int capacity,
            ThermalMemoryBudget.Reservation reservation
    ) {
        this.budget = budget;
        this.capacity = capacity;
        this.reservation = reservation;
        this.temperaturesC = new double[2][capacity];
        this.mediumIds = new int[2][capacity];
        this.cellFlags = new int[2][capacity];
    }

    /** Returns null when the complete double buffer cannot be admitted. */
    public static QueryPublication tryCreate(
            ThermalMemoryBudget dimensionBudget,
            int capacity
    ) {
        if (dimensionBudget == null) {
            throw new IllegalArgumentException("dimensionBudget is required");
        }
        long bytes = projectedPayloadBytes(capacity);
        ThermalMemoryBudget.Reservation reservation = dimensionBudget.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, bytes);
        return reservation == null
                ? null
                : new QueryPublication(dimensionBudget, capacity, reservation);
    }

    public static long projectedPayloadBytes(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        return Math.multiplyExact(
                capacity,
                2L * (Double.BYTES + Integer.BYTES + Integer.BYTES));
    }

    public synchronized int capacity() {
        return capacity;
    }

    public synchronized long reservedBytes() {
        return reservation.bytes();
    }

    public long publicationVersion() {
        return publicationVersion;
    }

    /**
     * Admits and allocates complete replacement buffers before invalidating
     * and releasing the old backing storage.
     */
    public synchronized boolean tryEnsureCapacity(int requiredCapacity) {
        if (requiredCapacity <= capacity) {
            return true;
        }
        if (!acceptingPublications) {
            return false;
        }
        int newCapacity = Math.max(requiredCapacity, Math.multiplyExact(capacity, 2));
        ThermalMemoryBudget.Reservation newReservation = budget.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL,
                projectedPayloadBytes(newCapacity));
        if (newReservation == null) {
            return false;
        }

        double[][] newTemperatures = new double[2][newCapacity];
        int[][] newMediumIds = new int[2][newCapacity];
        int[][] newCellFlags = new int[2][newCapacity];
        if (!beginWrite()) {
            newReservation.close();
            return false;
        }
        ThermalMemoryBudget.Reservation oldReservation = reservation;
        temperaturesC = newTemperatures;
        mediumIds = newMediumIds;
        cellFlags = newCellFlags;
        capacity = newCapacity;
        reservation = newReservation;
        clearEnvelope();
        endWrite();
        oldReservation.close();
        return true;
    }

    /** Copies a query-only projection from the authoritative H/C arena. */
    public synchronized boolean publish(
            ThermalCellArena arena,
            double referenceTemperatureC,
            long lifecycleGeneration,
            long geometryRevision,
            long topologyGeneration,
            long solveEpoch,
            long sampleTick,
            int slotStart,
            int slotCount
    ) {
        if (arena == null) {
            throw new IllegalArgumentException("arena is required");
        }
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requireNonNegative("lifecycleGeneration", lifecycleGeneration);
        requireNonNegative("geometryRevision", geometryRevision);
        requireNonNegative("topologyGeneration", topologyGeneration);
        requireNonNegative("solveEpoch", solveEpoch);
        requireNonNegative("sampleTick", sampleTick);
        if (slotStart < 0 || slotCount < 0
                || slotStart > arena.highWaterMark() - slotCount
                || slotCount > capacity) {
            throw new IllegalArgumentException("published arena span is invalid");
        }
        if (!acceptingPublications || !beginWrite()) {
            return false;
        }

        int targetBuffer = publishedBufferIndex == 0 ? 1 : 0;
        double[] targetTemperatures = temperaturesC[targetBuffer];
        int[] targetMediumIds = mediumIds[targetBuffer];
        int[] targetFlags = cellFlags[targetBuffer];
        for (int offset = 0; offset < slotCount; offset++) {
            int arenaSlot = slotStart + offset;
            if (arena.isLive(arenaSlot)) {
                targetTemperatures[offset] = arena.temperatureC(
                        arenaSlot, referenceTemperatureC);
                targetMediumIds[offset] = arena.mediumId(arenaSlot);
                targetFlags[offset] = arena.flags(arenaSlot);
            } else {
                targetTemperatures[offset] = Double.NaN;
                targetMediumIds[offset] = -1;
                targetFlags[offset] = NOT_LIVE;
            }
        }
        this.lifecycleGeneration = lifecycleGeneration;
        this.geometryRevision = geometryRevision;
        this.topologyGeneration = topologyGeneration;
        this.solveEpoch = solveEpoch;
        this.sampleTick = sampleTick;
        this.slotStart = slotStart;
        this.slotCount = slotCount;
        this.publishedBufferIndex = targetBuffer;
        this.valid = true;
        endWrite();
        return true;
    }

    /** Advances an unchanged sleeping publication without copying its values. */
    public synchronized boolean republishUnchanged(
            long lifecycleGeneration,
            long geometryRevision,
            long topologyGeneration,
            long solveEpoch,
            long sampleTick
    ) {
        requireNonNegative("lifecycleGeneration", lifecycleGeneration);
        requireNonNegative("geometryRevision", geometryRevision);
        requireNonNegative("topologyGeneration", topologyGeneration);
        requireNonNegative("solveEpoch", solveEpoch);
        requireNonNegative("sampleTick", sampleTick);
        if (!acceptingPublications
                || !valid
                || this.lifecycleGeneration != lifecycleGeneration
                || this.geometryRevision != geometryRevision
                || this.topologyGeneration != topologyGeneration
                || !beginWrite()) {
            return false;
        }
        this.solveEpoch = solveEpoch;
        this.sampleTick = sampleTick;
        endWrite();
        return true;
    }

    /** One initial attempt plus one retry, then deterministic caller fallback. */
    public boolean tryRead(
            int arenaSlot,
            long expectedLifecycleGeneration,
            long expectedGeometryRevision,
            MutableSample out
    ) {
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
            long readLifecycle = lifecycleGeneration;
            long readGeometry = geometryRevision;
            long readTopology = topologyGeneration;
            long readEpoch = solveEpoch;
            long readSampleTick = sampleTick;
            int readStart = slotStart;
            int readCount = slotCount;
            int readBuffer = publishedBufferIndex;
            int offset = arenaSlot - readStart;
            if (!readValid
                    || readLifecycle != expectedLifecycleGeneration
                    || readGeometry != expectedGeometryRevision
                    || readBuffer < 0
                    || offset < 0
                    || offset >= readCount) {
                if (firstVersion == publicationVersion) {
                    return false;
                }
                continue;
            }

            double temperature = temperaturesC[readBuffer][offset];
            int medium = mediumIds[readBuffer][offset];
            int flags = cellFlags[readBuffer][offset];
            long secondVersion = publicationVersion;
            if (firstVersion == secondVersion && (secondVersion & 1L) == 0L) {
                if (!Double.isFinite(temperature) || medium < 0) {
                    return false;
                }
                out.set(
                        temperature,
                        medium,
                        flags,
                        readLifecycle,
                        readGeometry,
                        readTopology,
                        readEpoch,
                        readSampleTick);
                return true;
            }
        }
        return false;
    }

    /** Permanently rejects this lifecycle and invalidates all old readers. */
    public synchronized void retire() {
        acceptingPublications = false;
        invalidateLocked();
    }

    public synchronized void invalidate() {
        invalidateLocked();
    }

    private void invalidateLocked() {
        if (!beginWrite()) {
            return;
        }
        clearEnvelope();
        endWrite();
    }

    @Override
    public synchronized void close() {
        if (reservation.released()) {
            return;
        }
        acceptingPublications = false;
        invalidateLocked();
        reservation.close();
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
        lifecycleGeneration = -1L;
        geometryRevision = -1L;
        topologyGeneration = -1L;
        solveEpoch = -1L;
        sampleTick = -1L;
        slotStart = 0;
        slotCount = 0;
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
        private boolean valid;
        private double temperatureC;
        private int mediumId;
        private int flags;
        private long lifecycleGeneration;
        private long geometryRevision;
        private long topologyGeneration;
        private long solveEpoch;
        private long sampleTick;

        public boolean valid() {
            return valid;
        }

        public double temperatureC() {
            return temperatureC;
        }

        public int mediumId() {
            return mediumId;
        }

        public int flags() {
            return flags;
        }

        public long lifecycleGeneration() {
            return lifecycleGeneration;
        }

        public long geometryRevision() {
            return geometryRevision;
        }

        public long topologyGeneration() {
            return topologyGeneration;
        }

        public long solveEpoch() {
            return solveEpoch;
        }

        public long sampleTick() {
            return sampleTick;
        }

        private void set(
                double temperatureC,
                int mediumId,
                int flags,
                long lifecycleGeneration,
                long geometryRevision,
                long topologyGeneration,
                long solveEpoch,
                long sampleTick
        ) {
            this.valid = true;
            this.temperatureC = temperatureC;
            this.mediumId = mediumId;
            this.flags = flags;
            this.lifecycleGeneration = lifecycleGeneration;
            this.geometryRevision = geometryRevision;
            this.topologyGeneration = topologyGeneration;
            this.solveEpoch = solveEpoch;
            this.sampleTick = sampleTick;
        }

        private void clear() {
            valid = false;
            temperatureC = Double.NaN;
            mediumId = -1;
            flags = 0;
            lifecycleGeneration = -1L;
            geometryRevision = -1L;
            topologyGeneration = -1L;
            solveEpoch = -1L;
            sampleTick = -1L;
        }
    }
}
