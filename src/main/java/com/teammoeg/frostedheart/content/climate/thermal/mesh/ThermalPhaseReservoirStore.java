/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.Arrays;

/** Arena-slot-indexed primitive phase metadata and request state. */
final class ThermalPhaseReservoirStore {
    private static final byte REQUEST_IDLE = 0;
    private static final byte REQUEST_RETRY = 1;
    private static final byte REQUEST_ENQUEUED = 2;

    private int[] profileIds;
    private long[] candidateMasks;
    private double[] transitionTemperaturesC;
    private double[] transitionEnergyJPerUnit;
    private double[] reservedEnergyJ;
    private long[] requestSequences;
    private byte[] requestCandidateBits;
    private byte[] requestStates;

    ThermalPhaseReservoirStore(int capacity) {
        profileIds = new int[capacity];
        candidateMasks = new long[capacity];
        transitionTemperaturesC = new double[capacity];
        transitionEnergyJPerUnit = new double[capacity];
        reservedEnergyJ = new double[capacity];
        requestSequences = new long[capacity];
        requestCandidateBits = new byte[capacity];
        requestStates = new byte[capacity];
    }

    void ensureCapacity(int capacity) {
        if (capacity <= profileIds.length) {
            return;
        }
        profileIds = Arrays.copyOf(profileIds, capacity);
        candidateMasks = Arrays.copyOf(candidateMasks, capacity);
        transitionTemperaturesC = Arrays.copyOf(
                transitionTemperaturesC, capacity);
        transitionEnergyJPerUnit = Arrays.copyOf(
                transitionEnergyJPerUnit, capacity);
        reservedEnergyJ = Arrays.copyOf(reservedEnergyJ, capacity);
        requestSequences = Arrays.copyOf(requestSequences, capacity);
        requestCandidateBits = Arrays.copyOf(requestCandidateBits, capacity);
        requestStates = Arrays.copyOf(requestStates, capacity);
    }

    void write(
            int slot,
            int profileId,
            long candidateMask,
            double transitionTemperatureC,
            double transitionEnergyJ
    ) {
        profileIds[slot] = profileId;
        candidateMasks[slot] = candidateMask;
        transitionTemperaturesC[slot] = transitionTemperatureC;
        transitionEnergyJPerUnit[slot] = transitionEnergyJ;
        reservedEnergyJ[slot] = 0.0D;
        requestSequences[slot] = 0L;
        requestCandidateBits[slot] = 0;
        requestStates[slot] = REQUEST_IDLE;
    }

    void clear(int slot) {
        profileIds[slot] = 0;
        candidateMasks[slot] = 0L;
        transitionTemperaturesC[slot] = 0.0D;
        transitionEnergyJPerUnit[slot] = 0.0D;
        reservedEnergyJ[slot] = 0.0D;
        requestSequences[slot] = 0L;
        requestCandidateBits[slot] = 0;
        requestStates[slot] = REQUEST_IDLE;
    }

    int profileId(int slot) {
        return profileIds[slot];
    }

    long candidateMask(int slot) {
        return candidateMasks[slot];
    }

    double transitionTemperatureC(int slot) {
        return transitionTemperaturesC[slot];
    }

    double transitionEnergyJPerUnit(int slot) {
        return transitionEnergyJPerUnit[slot];
    }

    double availableEnergyJ(int slot, double enthalpyJ) {
        return enthalpyJ - reservedEnergyJ[slot];
    }

    double maximumEnergyJ(int slot) {
        return Long.bitCount(candidateMasks[slot])
                * transitionEnergyJPerUnit[slot];
    }

    boolean requestOutstanding(int slot) {
        return requestStates[slot] != REQUEST_IDLE;
    }

    boolean requestNeedsOffer(int slot) {
        return requestStates[slot] == REQUEST_RETRY;
    }

    long requestSequence(int slot) {
        return requestSequences[slot];
    }

    int requestCandidateBit(int slot) {
        return requestStates[slot] == REQUEST_IDLE
                ? -1 : Byte.toUnsignedInt(requestCandidateBits[slot]);
    }

    void beginRequest(
            int slot,
            long requestSequence,
            int candidateBit,
            double enthalpyJ
    ) {
        if (requestStates[slot] != REQUEST_IDLE
                || requestSequence <= requestSequences[slot]
                || candidateBit < 0 || candidateBit >= Long.SIZE
                || (candidateMasks[slot] & 1L << candidateBit) == 0L
                || availableEnergyJ(slot, enthalpyJ) + 1.0e-12D
                < transitionEnergyJPerUnit[slot]) {
            throw new IllegalStateException("phase request cannot be reserved");
        }
        reservedEnergyJ[slot] = transitionEnergyJPerUnit[slot];
        requestSequences[slot] = requestSequence;
        requestCandidateBits[slot] = (byte) candidateBit;
        requestStates[slot] = REQUEST_RETRY;
    }

    void markRequestEnqueued(int slot, long requestSequence) {
        requireCurrentRequest(slot, requestSequence);
        requestStates[slot] = REQUEST_ENQUEUED;
    }

    void retryRequest(int slot, long requestSequence) {
        requireCurrentRequest(slot, requestSequence);
        requestStates[slot] = REQUEST_RETRY;
    }

    double completeRequest(
            int slot,
            long requestSequence,
            boolean mutationApplied,
            double enthalpyJ
    ) {
        requireCurrentRequest(slot, requestSequence);
        double consumed = mutationApplied ? reservedEnergyJ[slot] : 0.0D;
        if (mutationApplied) {
            double next = enthalpyJ - consumed;
            if (!Double.isFinite(next) || next < -1.0e-9D) {
                throw new IllegalStateException(
                        "phase completion exceeds stored energy");
            }
        }
        reservedEnergyJ[slot] = 0.0D;
        requestCandidateBits[slot] = 0;
        requestStates[slot] = REQUEST_IDLE;
        return consumed;
    }

    void copyRequest(int oldSlot, int newSlot) {
        reservedEnergyJ[newSlot] = reservedEnergyJ[oldSlot];
        requestSequences[newSlot] = requestSequences[oldSlot];
        requestCandidateBits[newSlot] = requestCandidateBits[oldSlot];
        requestStates[newSlot] = requestStates[oldSlot];
    }

    private void requireCurrentRequest(int slot, long requestSequence) {
        if (requestStates[slot] == REQUEST_IDLE
                || requestSequences[slot] != requestSequence) {
            throw new IllegalStateException("phase request is not current");
        }
    }
}
