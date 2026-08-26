/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded worker/main-thread bridge for Brick-local latent-energy reservoirs.
 * Reservoir authority stays in {@link ThermalCellArena}; the two rings only
 * transport one outstanding mutation request and its outcome.
 */
public final class PhaseTransitionRuntime {
    private static final AckOutcome[] ACK_OUTCOMES = AckOutcome.values();

    public enum AckOutcome {
        APPLIED,
        REJECTED,
        RETRY
    }

    private final ThermalCellArena arena;
    private final RequestRing requests;
    private final AckRing acks;
    private final PendingAckTable pendingAcks;
    private final ThermalExchangeKernel.MutableBoundaryResult boundaryScratch =
            new ThermalExchangeKernel.MutableBoundaryResult();

    private long nextRequestSequence;
    private long appliedAckWatermark;

    public PhaseTransitionRuntime(
            ThermalCellArena arena,
            int requestCapacity,
            int ackCapacity
    ) {
        this.arena = Objects.requireNonNull(arena, "arena");
        requests = new RequestRing(requestCapacity);
        acks = new AckRing(ackCapacity);
        pendingAcks = new PendingAckTable(requestCapacity);
    }

    public boolean targets(ThermalCellArena candidate) {
        return arena == candidate;
    }

    /** Applies one conservative Air-to-phase contact and offers a request if ready. */
    public boolean applyContact(
            int airSlot,
            int phaseSlot,
            double conductanceWPerK,
            double referenceTemperatureC,
            double dtSeconds
    ) {
        if (!arena.isLive(airSlot) || !arena.isLive(phaseSlot)
                || arena.isPhaseReservoir(airSlot)
                || !arena.isPhaseReservoir(phaseSlot)
                || !Double.isFinite(conductanceWPerK)
                || conductanceWPerK < 0.0D
                || !Double.isFinite(referenceTemperatureC)
                || !Double.isFinite(dtSeconds) || dtSeconds < 0.0D) {
            return false;
        }
        ThermalExchangeKernel.exchangeFixedBoundaryInto(
                arena.enthalpyJ(airSlot),
                arena.capacityJPerK(airSlot),
                referenceTemperatureC,
                arena.phaseTransitionTemperatureC(phaseSlot),
                conductanceWPerK,
                dtSeconds,
                boundaryScratch);
        if (!boundaryScratch.applied()) {
            return false;
        }

        double requestedIntoReservoir = -boundaryScratch.energyFromBoundaryJ();
        if (requestedIntoReservoir > 0.0D) {
            double remaining = Math.max(
                    0.0D,
                    arena.phaseMaximumEnergyJ(phaseSlot) - arena.enthalpyJ(phaseSlot));
            double accepted = Math.min(requestedIntoReservoir, remaining);
            if (!Double.isFinite(accepted)) {
                return false;
            }
            if (accepted > 0.0D) {
                arena.addEnthalpyJ(airSlot, -accepted);
                arena.addEnthalpyJ(phaseSlot, accepted);
            }
        }
        reserveOrRetry(phaseSlot);
        return true;
    }

    /** Worker-side ACK application covered by one sealed transition watermark. */
    public int applyAcksThrough(long maximumWatermark) {
        if (maximumWatermark < appliedAckWatermark) {
            throw new IllegalArgumentException("phase ACK watermark regressed");
        }
        int applied = 0;
        MutableAck ack = new MutableAck();
        while (acks.pollThrough(maximumWatermark, ack)) {
            int slot = findReservoir(ack);
            if (slot >= 0 && arena.phaseRequestOutstanding(slot)
                    && arena.phaseRequestSequence(slot) == ack.requestSequence) {
                if (ack.outcome == AckOutcome.RETRY) {
                    arena.retryPhaseRequest(slot, ack.requestSequence);
                } else {
                    arena.completePhaseRequest(
                            slot,
                            ack.requestSequence,
                            ack.outcome == AckOutcome.APPLIED);
                }
            }
            appliedAckWatermark = ack.watermark;
            applied++;
        }
        return applied;
    }

    /** Main-thread allocation-free request poll. */
    public boolean pollRequest(MutableRequest target) {
        return requests.poll(Objects.requireNonNull(target, "target"));
    }

    public boolean canAcceptAck() {
        return acks.canOffer() || pendingAcks.hasFreeSlot();
    }

    /**
     * Main-thread outcome submission. A full ACK ring retains the outcome in a
     * fixed per-request table until a later flush succeeds.
     */
    public void submitAck(MutableRequest request, AckOutcome outcome) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(outcome, "outcome");
        if (acks.offer(request, outcome)) {
            return;
        }
        if (!pendingAcks.retain(request, outcome)) {
            throw new IllegalStateException("phase ACK sticky table exhausted");
        }
    }

    /** Main-thread retry of retained ACK outcomes. */
    public int flushPendingAcks() {
        return pendingAcks.flushInto(acks);
    }

    public long latestOfferedAckWatermark() {
        return acks.latestOfferedWatermark();
    }

    private void reserveOrRetry(int phaseSlot) {
        if (!arena.phaseRequestOutstanding(phaseSlot)) {
            double unitEnergy = arena.phaseTransitionEnergyJPerUnit(phaseSlot);
            if (arena.phaseCandidateMask(phaseSlot) != 0L
                    && arena.phaseAvailableEnergyJ(phaseSlot) + 1.0e-12D >= unitEnergy) {
                int candidateBit = Long.numberOfTrailingZeros(
                        arena.phaseCandidateMask(phaseSlot));
                arena.beginPhaseRequest(
                        phaseSlot,
                        ++nextRequestSequence,
                        candidateBit);
            }
        }
        if (!arena.phaseRequestNeedsOffer(phaseSlot)) {
            return;
        }
        if (requests.offer(arena, phaseSlot)) {
            arena.markPhaseRequestEnqueued(
                    phaseSlot, arena.phaseRequestSequence(phaseSlot));
        }
    }

    private int findReservoir(MutableAck ack) {
        if (matches(ack.fastSlot, ack.lifecycleGeneration, ack.brickMinX,
                ack.brickMinY, ack.brickMinZ, ack.profileId)) {
            return ack.fastSlot;
        }
        for (int slot = 0; slot < arena.highWaterMark(); slot++) {
            if (matches(slot, ack.lifecycleGeneration, ack.brickMinX,
                    ack.brickMinY, ack.brickMinZ, ack.profileId)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean matches(
            int slot,
            int lifecycleGeneration,
            int brickMinX,
            int brickMinY,
            int brickMinZ,
            int profileId
    ) {
        return arena.isLive(slot)
                && arena.isPhaseReservoir(slot)
                && arena.lifecycleGeneration(slot) == lifecycleGeneration
                && arena.minimumX(slot) == brickMinX
                && arena.minimumY(slot) == brickMinY
                && arena.minimumZ(slot) == brickMinZ
                && arena.phaseProfileId(slot) == profileId;
    }

    /** Caller-owned request transported from worker to main. */
    public static final class MutableRequest {
        private int fastSlot;
        private int lifecycleGeneration;
        private int brickMinX;
        private int brickMinY;
        private int brickMinZ;
        private int profileId;
        private int candidateBit;
        private long requestSequence;

        public int lifecycleGeneration() {
            return lifecycleGeneration;
        }

        public int brickMinX() {
            return brickMinX;
        }

        public int brickMinY() {
            return brickMinY;
        }

        public int brickMinZ() {
            return brickMinZ;
        }

        public int profileId() {
            return profileId;
        }

        public int blockX() {
            return brickMinX + (candidateBit & 3);
        }

        public int blockY() {
            return brickMinY + (candidateBit >>> 4);
        }

        public int blockZ() {
            return brickMinZ + ((candidateBit >>> 2) & 3);
        }

        private void set(
                int nextFastSlot,
                int nextLifecycleGeneration,
                int nextBrickMinX,
                int nextBrickMinY,
                int nextBrickMinZ,
                int nextProfileId,
                int nextCandidateBit,
                long nextRequestSequence
        ) {
            fastSlot = nextFastSlot;
            lifecycleGeneration = nextLifecycleGeneration;
            brickMinX = nextBrickMinX;
            brickMinY = nextBrickMinY;
            brickMinZ = nextBrickMinZ;
            profileId = nextProfileId;
            candidateBit = nextCandidateBit;
            requestSequence = nextRequestSequence;
        }
    }

    private static final class MutableAck {
        private long watermark;
        private int fastSlot;
        private int lifecycleGeneration;
        private int brickMinX;
        private int brickMinY;
        private int brickMinZ;
        private int profileId;
        private long requestSequence;
        private AckOutcome outcome;
    }

    private static final class RequestRing {
        private final int capacity;
        private final int[] fastSlots;
        private final int[] lifecycleGenerations;
        private final int[] brickMinX;
        private final int[] brickMinY;
        private final int[] brickMinZ;
        private final int[] profileIds;
        private final byte[] candidateBits;
        private final long[] requestSequences;
        private final AtomicLong writeSequence = new AtomicLong();
        private final AtomicLong readSequence = new AtomicLong();

        private RequestRing(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("phase request capacity must be positive");
            }
            this.capacity = capacity;
            fastSlots = new int[capacity];
            lifecycleGenerations = new int[capacity];
            brickMinX = new int[capacity];
            brickMinY = new int[capacity];
            brickMinZ = new int[capacity];
            profileIds = new int[capacity];
            candidateBits = new byte[capacity];
            requestSequences = new long[capacity];
        }

        private boolean offer(ThermalCellArena arena, int phaseSlot) {
            long write = writeSequence.get();
            if (write - readSequence.get() >= capacity) {
                return false;
            }
            int index = (int) (write % capacity);
            fastSlots[index] = phaseSlot;
            lifecycleGenerations[index] = arena.lifecycleGeneration(phaseSlot);
            brickMinX[index] = arena.minimumX(phaseSlot);
            brickMinY[index] = arena.minimumY(phaseSlot);
            brickMinZ[index] = arena.minimumZ(phaseSlot);
            profileIds[index] = arena.phaseProfileId(phaseSlot);
            candidateBits[index] = (byte) arena.phaseRequestCandidateBit(phaseSlot);
            requestSequences[index] = arena.phaseRequestSequence(phaseSlot);
            writeSequence.lazySet(write + 1L);
            return true;
        }

        private boolean poll(MutableRequest target) {
            long read = readSequence.get();
            if (read >= writeSequence.get()) {
                return false;
            }
            int index = (int) (read % capacity);
            target.set(
                    fastSlots[index],
                    lifecycleGenerations[index],
                    brickMinX[index],
                    brickMinY[index],
                    brickMinZ[index],
                    profileIds[index],
                    Byte.toUnsignedInt(candidateBits[index]),
                    requestSequences[index]);
            readSequence.lazySet(read + 1L);
            return true;
        }
    }

    private static final class AckRing {
        private final int capacity;
        private final long[] watermarks;
        private final int[] fastSlots;
        private final int[] lifecycleGenerations;
        private final int[] brickMinX;
        private final int[] brickMinY;
        private final int[] brickMinZ;
        private final int[] profileIds;
        private final long[] requestSequences;
        private final byte[] outcomes;
        private final AtomicLong writeSequence = new AtomicLong();
        private final AtomicLong readSequence = new AtomicLong();
        private long nextWatermark;

        private AckRing(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("phase ACK capacity must be positive");
            }
            this.capacity = capacity;
            watermarks = new long[capacity];
            fastSlots = new int[capacity];
            lifecycleGenerations = new int[capacity];
            brickMinX = new int[capacity];
            brickMinY = new int[capacity];
            brickMinZ = new int[capacity];
            profileIds = new int[capacity];
            requestSequences = new long[capacity];
            outcomes = new byte[capacity];
        }

        private boolean offer(MutableRequest request, AckOutcome outcome) {
            return offer(
                    request.fastSlot,
                    request.lifecycleGeneration,
                    request.brickMinX,
                    request.brickMinY,
                    request.brickMinZ,
                    request.profileId,
                    request.requestSequence,
                    outcome);
        }

        private boolean offer(
                int fastSlot,
                int lifecycleGeneration,
                int nextBrickMinX,
                int nextBrickMinY,
                int nextBrickMinZ,
                int profileId,
                long requestSequence,
                AckOutcome outcome
        ) {
            long write = writeSequence.get();
            if (write - readSequence.get() >= capacity) {
                return false;
            }
            int index = (int) (write % capacity);
            long watermark = ++nextWatermark;
            watermarks[index] = watermark;
            fastSlots[index] = fastSlot;
            lifecycleGenerations[index] = lifecycleGeneration;
            brickMinX[index] = nextBrickMinX;
            brickMinY[index] = nextBrickMinY;
            brickMinZ[index] = nextBrickMinZ;
            profileIds[index] = profileId;
            requestSequences[index] = requestSequence;
            outcomes[index] = (byte) outcome.ordinal();
            writeSequence.lazySet(write + 1L);
            return true;
        }

        private boolean pollThrough(long maximumWatermark, MutableAck target) {
            long read = readSequence.get();
            if (read >= writeSequence.get()) {
                return false;
            }
            int index = (int) (read % capacity);
            if (watermarks[index] > maximumWatermark) {
                return false;
            }
            target.watermark = watermarks[index];
            target.fastSlot = fastSlots[index];
            target.lifecycleGeneration = lifecycleGenerations[index];
            target.brickMinX = brickMinX[index];
            target.brickMinY = brickMinY[index];
            target.brickMinZ = brickMinZ[index];
            target.profileId = profileIds[index];
            target.requestSequence = requestSequences[index];
            target.outcome = ACK_OUTCOMES[Byte.toUnsignedInt(outcomes[index])];
            readSequence.lazySet(read + 1L);
            return true;
        }

        private long latestOfferedWatermark() {
            return nextWatermark;
        }

        private boolean canOffer() {
            return writeSequence.get() - readSequence.get() < capacity;
        }
    }

    private static final class PendingAckTable {
        private final boolean[] occupied;
        private final int[] fastSlots;
        private final int[] lifecycleGenerations;
        private final int[] brickMinX;
        private final int[] brickMinY;
        private final int[] brickMinZ;
        private final int[] profileIds;
        private final long[] requestSequences;
        private final byte[] outcomes;

        private PendingAckTable(int capacity) {
            occupied = new boolean[capacity];
            fastSlots = new int[capacity];
            lifecycleGenerations = new int[capacity];
            brickMinX = new int[capacity];
            brickMinY = new int[capacity];
            brickMinZ = new int[capacity];
            profileIds = new int[capacity];
            requestSequences = new long[capacity];
            outcomes = new byte[capacity];
        }

        private boolean retain(MutableRequest request, AckOutcome outcome) {
            for (int index = 0; index < occupied.length; index++) {
                if (!occupied[index]) {
                    occupied[index] = true;
                    fastSlots[index] = request.fastSlot;
                    lifecycleGenerations[index] = request.lifecycleGeneration;
                    brickMinX[index] = request.brickMinX;
                    brickMinY[index] = request.brickMinY;
                    brickMinZ[index] = request.brickMinZ;
                    profileIds[index] = request.profileId;
                    requestSequences[index] = request.requestSequence;
                    outcomes[index] = (byte) outcome.ordinal();
                    return true;
                }
            }
            return false;
        }

        private boolean hasFreeSlot() {
            for (boolean value : occupied) {
                if (!value) {
                    return true;
                }
            }
            return false;
        }

        private int flushInto(AckRing ring) {
            int flushed = 0;
            for (int index = 0; index < occupied.length; index++) {
                if (!occupied[index]) {
                    continue;
                }
                AckOutcome outcome = ACK_OUTCOMES[
                        Byte.toUnsignedInt(outcomes[index])];
                if (!ring.offer(
                        fastSlots[index],
                        lifecycleGenerations[index],
                        brickMinX[index],
                        brickMinY[index],
                        brickMinZ[index],
                        profileIds[index],
                        requestSequences[index],
                        outcome)) {
                    break;
                }
                occupied[index] = false;
                flushed++;
            }
            return flushed;
        }
    }
}
