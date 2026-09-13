/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import java.util.Arrays;

/** Arena-slot-indexed primitive phase metadata and request state. */
final class ThermalPhaseRequestStore {
    private static final byte REQUEST_IDLE = 0;
    private static final byte REQUEST_RETRY = 1;
    private static final byte REQUEST_ENQUEUED = 2;
    private static final byte ACKED_WAITING_LAYOUT = 3;

    private long[] requestSequences;
    private byte[] requestStates;

    ThermalPhaseRequestStore(int capacity) {
        requestSequences = new long[capacity];
        requestStates = new byte[capacity];
    }

    void ensureCapacity(int capacity) {
        if (capacity <= requestSequences.length) {
            return;
        }
        requestSequences = Arrays.copyOf(requestSequences, capacity);
        requestStates = Arrays.copyOf(requestStates, capacity);
    }

    void clear(int slot) {
        requestSequences[slot] = 0L;
        requestStates[slot] = REQUEST_IDLE;
    }

    boolean requestOutstanding(int slot) {
        return requestStates[slot] != REQUEST_IDLE;
    }

    boolean requestNeedsOffer(int slot) {
        return requestStates[slot] == REQUEST_RETRY;
    }

    boolean acknowledged(int slot) {
        return requestStates[slot] == ACKED_WAITING_LAYOUT;
    }

    void beginMaterialRequest(int slot, long sequence) {
        requestSequences[slot] = sequence;
        requestStates[slot] = REQUEST_RETRY;
    }

    void completeMaterialRequest(int slot, long sequence, boolean applied) {
        requireCurrentRequest(slot, sequence);
        // Latent energy is already in the body's H. Never consume it on ACK.
        requestStates[slot] = applied ? ACKED_WAITING_LAYOUT : REQUEST_IDLE;
    }

    long requestSequence(int slot) {
        return requestSequences[slot];
    }

    void markRequestEnqueued(int slot, long requestSequence) {
        requireCurrentRequest(slot, requestSequence);
        requestStates[slot] = REQUEST_ENQUEUED;
    }

    void retryRequest(int slot, long requestSequence) {
        requireCurrentRequest(slot, requestSequence);
        requestStates[slot] = REQUEST_RETRY;
    }

    void copyRequest(int oldSlot, int newSlot) {
        requestSequences[newSlot] = requestSequences[oldSlot];
        requestStates[newSlot] = requestStates[oldSlot];
    }

    private void requireCurrentRequest(int slot, long requestSequence) {
        if (requestStates[slot] == REQUEST_IDLE
                || requestSequences[slot] != requestSequence) {
            throw new IllegalStateException("phase request is not current");
        }
    }
}
