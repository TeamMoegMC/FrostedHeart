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

import java.util.Arrays;
import java.util.Objects;

/** Worker-owned latent-energy reservoirs with batch-based main-thread handoff. */
public final class PhaseTransitionRuntime {
    private static final Request[] NO_REQUESTS = new Request[0];
    public enum AckOutcome {
        APPLIED,
        REJECTED,
        RETRY
    }

    /** Immutable worker-to-main request transported in a completion. */
    public record Request(
            int fastSlot,
            int lifecycleGeneration,
            int brickMinX,
            int brickMinY,
            int brickMinZ,
            int profileId,
            int candidateBit,
            long requestSequence
    ) {
        public Request {
            if (fastSlot < 0 || lifecycleGeneration < 0 || profileId < 0
                    || candidateBit < 0 || candidateBit >= 64
                    || requestSequence <= 0L) {
                throw new IllegalArgumentException("phase request identity is invalid");
            }
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
    }

    private final ThermalCellArena arena;
    private final RequestQueue requests;
    private final ReservoirIndex reservoirs;
    private final ThermalExchangeKernel.MutableBoundaryResult boundaryScratch =
            new ThermalExchangeKernel.MutableBoundaryResult();

    private long nextRequestSequence;

    public PhaseTransitionRuntime(ThermalCellArena arena, int requestCapacity) {
        this.arena = Objects.requireNonNull(arena, "arena");
        requests = new RequestQueue(requestCapacity);
        reservoirs = new ReservoirIndex(Math.max(4, requestCapacity));
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

    /** Applies one exactly-once request acknowledgement from an ordered batch. */
    public boolean applyAck(Request request, AckOutcome outcome) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(outcome, "outcome");
        int slot = findReservoir(request);
        if (slot < 0 || !arena.phaseRequestOutstanding(slot)
                || arena.phaseRequestSequence(slot) != request.requestSequence()) {
            return false;
        }
        if (outcome == AckOutcome.RETRY) {
            arena.retryPhaseRequest(slot, request.requestSequence());
        } else {
            arena.completePhaseRequest(
                    slot,
                    request.requestSequence(),
                    outcome == AckOutcome.APPLIED);
        }
        return true;
    }

    /** Drains at most {@code maximum} requests into an immutable completion. */
    public Request[] drainRequests(int maximum) {
        if (maximum < 0) {
            throw new IllegalArgumentException("phase request drain limit is negative");
        }
        int count = Math.min(maximum, requests.size());
        if (count == 0) {
            return NO_REQUESTS;
        }
        Request[] result = new Request[count];
        for (int index = 0; index < result.length; index++) {
            result[index] = requests.poll();
        }
        return result;
    }

    public void reserveReservoirChanges(int additionalReservoirs) {
        reservoirs.reserve(additionalReservoirs);
    }

    public void registerReservoir(int slot) {
        if (!arena.isLive(slot) || !arena.isPhaseReservoir(slot)) {
            throw new IllegalArgumentException("phase reservoir slot is invalid");
        }
        reservoirs.put(
                arena.lifecycleGeneration(slot),
                arena.minimum(slot, 0),
                arena.minimum(slot, 1),
                arena.minimum(slot, 2),
                arena.phaseProfileId(slot),
                slot);
    }

    public void unregisterReservoir(int slot) {
        if (arena.isLive(slot) && arena.isPhaseReservoir(slot)) {
            reservoirs.remove(
                    arena.lifecycleGeneration(slot),
                    arena.minimum(slot, 0),
                    arena.minimum(slot, 1),
                    arena.minimum(slot, 2),
                    arena.phaseProfileId(slot));
        }
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

    private int findReservoir(Request request) {
        if (matches(
                request.fastSlot(),
                request.lifecycleGeneration(),
                request.brickMinX(),
                request.brickMinY(),
                request.brickMinZ(),
                request.profileId())) {
            return request.fastSlot();
        }
        return reservoirs.find(
                request.lifecycleGeneration(),
                request.brickMinX(),
                request.brickMinY(),
                request.brickMinZ(),
                request.profileId());
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
                && arena.minimum(slot, 0) == brickMinX
                && arena.minimum(slot, 1) == brickMinY
                && arena.minimum(slot, 2) == brickMinZ
                && arena.phaseProfileId(slot) == profileId;
    }

    private static final class ReservoirIndex {
        private static final byte EMPTY = 0;
        private static final byte OCCUPIED = 1;
        private static final byte DELETED = 2;

        private int[] generations;
        private int[] minX;
        private int[] minY;
        private int[] minZ;
        private int[] profileIds;
        private int[] slots;
        private byte[] states;
        private int size;
        private int used;
        private int resizeThreshold;

        private ReservoirIndex(int expected) {
            allocate(tableCapacity(expected));
        }

        private void reserve(int additional) {
            if (additional < 0) {
                throw new IllegalArgumentException("reservoir reserve count is negative");
            }
            int required = Math.addExact(size, additional);
            if (required > resizeThreshold) {
                rehash(tableCapacity(required));
            } else if (used + additional > resizeThreshold) {
                rehash(states.length);
            }
        }

        private int find(int generation, int x, int y, int z, int profileId) {
            int mask = states.length - 1;
            int index = hash(generation, x, y, z, profileId) & mask;
            while (states[index] != EMPTY) {
                if (states[index] == OCCUPIED
                        && matches(index, generation, x, y, z, profileId)) {
                    return slots[index];
                }
                index = index + 1 & mask;
            }
            return -1;
        }

        private void put(
                int generation,
                int x,
                int y,
                int z,
                int profileId,
                int slot
        ) {
            int mask = states.length - 1;
            int index = hash(generation, x, y, z, profileId) & mask;
            int deleted = -1;
            while (states[index] != EMPTY) {
                if (states[index] == OCCUPIED
                        && matches(index, generation, x, y, z, profileId)) {
                    slots[index] = slot;
                    return;
                }
                if (states[index] == DELETED && deleted < 0) {
                    deleted = index;
                }
                index = index + 1 & mask;
            }
            int destination = deleted >= 0 ? deleted : index;
            if (states[destination] == EMPTY) {
                if (used >= resizeThreshold) {
                    throw new IllegalStateException(
                            "phase reservoir index capacity was not reserved");
                }
                used++;
            }
            states[destination] = OCCUPIED;
            generations[destination] = generation;
            minX[destination] = x;
            minY[destination] = y;
            minZ[destination] = z;
            profileIds[destination] = profileId;
            slots[destination] = slot;
            size++;
        }

        private void remove(int generation, int x, int y, int z, int profileId) {
            int mask = states.length - 1;
            int index = hash(generation, x, y, z, profileId) & mask;
            while (states[index] != EMPTY) {
                if (states[index] == OCCUPIED
                        && matches(index, generation, x, y, z, profileId)) {
                    states[index] = DELETED;
                    slots[index] = -1;
                    size--;
                    return;
                }
                index = index + 1 & mask;
            }
        }

        private boolean matches(
                int index,
                int generation,
                int x,
                int y,
                int z,
                int profileId
        ) {
            return generations[index] == generation
                    && minX[index] == x
                    && minY[index] == y
                    && minZ[index] == z
                    && profileIds[index] == profileId;
        }

        private void allocate(int capacity) {
            generations = new int[capacity];
            minX = new int[capacity];
            minY = new int[capacity];
            minZ = new int[capacity];
            profileIds = new int[capacity];
            slots = new int[capacity];
            states = new byte[capacity];
            resizeThreshold = Math.max(1, (int) (capacity * 0.6D));
            size = 0;
            used = 0;
        }

        private void rehash(int capacity) {
            int[] oldGenerations = generations;
            int[] oldX = minX;
            int[] oldY = minY;
            int[] oldZ = minZ;
            int[] oldProfiles = profileIds;
            int[] oldSlots = slots;
            byte[] oldStates = states;
            allocate(capacity);
            for (int index = 0; index < oldStates.length; index++) {
                if (oldStates[index] == OCCUPIED) {
                    put(
                            oldGenerations[index], oldX[index], oldY[index],
                            oldZ[index], oldProfiles[index], oldSlots[index]);
                }
            }
        }

        private static int tableCapacity(int expected) {
            int required = Math.max(4, (int) Math.ceil(expected / 0.6D));
            int highest = Integer.highestOneBit(required - 1);
            if (highest >= 1 << 29) {
                throw new IllegalArgumentException("phase reservoir index is too large");
            }
            return highest << 1;
        }

        private static int hash(int generation, int x, int y, int z, int profileId) {
            int hash = generation * 0x9E3779B9;
            hash = Integer.rotateLeft(hash ^ x, 7) * 0x85EBCA6B;
            hash = Integer.rotateLeft(hash ^ y, 11) * 0xC2B2AE35;
            hash = Integer.rotateLeft(hash ^ z, 13) * 0x27D4EB2D;
            return hash ^ profileId * 0x165667B1;
        }
    }

    /** Worker-only bounded FIFO. Completion draining never overlaps a substep. */
    private static final class RequestQueue {
        private final int capacity;
        private final int[] fastSlots;
        private final int[] lifecycleGenerations;
        private final int[] brickMinX;
        private final int[] brickMinY;
        private final int[] brickMinZ;
        private final int[] profileIds;
        private final byte[] candidateBits;
        private final long[] requestSequences;
        private long writeSequence;
        private long readSequence;

        private RequestQueue(int capacity) {
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
            if (size() >= capacity) {
                return false;
            }
            int index = (int) (writeSequence % capacity);
            fastSlots[index] = phaseSlot;
            lifecycleGenerations[index] = arena.lifecycleGeneration(phaseSlot);
            brickMinX[index] = arena.minimum(phaseSlot, 0);
            brickMinY[index] = arena.minimum(phaseSlot, 1);
            brickMinZ[index] = arena.minimum(phaseSlot, 2);
            profileIds[index] = arena.phaseProfileId(phaseSlot);
            candidateBits[index] = (byte) arena.phaseRequestCandidateBit(phaseSlot);
            requestSequences[index] = arena.phaseRequestSequence(phaseSlot);
            writeSequence++;
            return true;
        }

        private Request poll() {
            if (size() == 0) {
                return null;
            }
            int index = (int) (readSequence % capacity);
            Request result = new Request(
                    fastSlots[index],
                    lifecycleGenerations[index],
                    brickMinX[index],
                    brickMinY[index],
                    brickMinZ[index],
                    profileIds[index],
                    Byte.toUnsignedInt(candidateBits[index]),
                    requestSequences[index]);
            readSequence++;
            return result;
        }

        private int size() {
            return Math.toIntExact(writeSequence - readSequence);
        }

    }
}
