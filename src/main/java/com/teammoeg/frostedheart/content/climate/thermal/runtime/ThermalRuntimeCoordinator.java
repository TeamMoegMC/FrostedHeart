/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import java.util.Arrays;

/**
 * Fixed-capacity fair ready-dimension coordinator. It stores no Runnable or
 * unbounded executor task: shared workers repeatedly call {@link #runNext(long)}.
 */
public final class ThermalRuntimeCoordinator implements AutoCloseable {
    private static final byte IDLE = 0;
    private static final byte QUEUED = 1;
    private static final byte RUNNING = 2;

    private final int readyCapacity;
    private final int normalReadyCapacity;
    private final long oldestPromotionTicks;
    private final int normalRunsPerRecovery;
    private final ThermalMemoryBudget.Reservation reservation;

    private final DimensionThermalRuntime[] runtimes;
    private final byte[] mailboxStates;
    private final boolean[] rerunRequested;
    private final boolean[] recoveryRequested;
    private final boolean[] reofferRequired;
    private final long[] oldestRequestTicks;

    private final int[] readySlots;
    private final long[] readyGenerations;
    private final boolean[] readyRecovery;
    private int readySize;
    private int normalReadyCount;
    private int reofferCursor;
    private int normalRunsSinceRecovery;
    private boolean closed;

    private ThermalRuntimeCoordinator(
            int readyCapacity,
            int maxDimensions,
            int recoveryReserveSlots,
            long oldestPromotionTicks,
            int normalRunsPerRecovery,
            ThermalMemoryBudget.Reservation reservation
    ) {
        this.readyCapacity = readyCapacity;
        this.normalReadyCapacity = readyCapacity - recoveryReserveSlots;
        this.oldestPromotionTicks = oldestPromotionTicks;
        this.normalRunsPerRecovery = normalRunsPerRecovery;
        this.reservation = reservation;
        this.runtimes = new DimensionThermalRuntime[maxDimensions];
        this.mailboxStates = new byte[maxDimensions];
        this.rerunRequested = new boolean[maxDimensions];
        this.recoveryRequested = new boolean[maxDimensions];
        this.reofferRequired = new boolean[maxDimensions];
        this.oldestRequestTicks = new long[maxDimensions];
        Arrays.fill(oldestRequestTicks, Long.MAX_VALUE);
        this.readySlots = new int[readyCapacity];
        this.readyGenerations = new long[readyCapacity];
        this.readyRecovery = new boolean[readyCapacity];
    }

    /** Returns null when critical coordinator storage cannot be admitted. */
    public static ThermalRuntimeCoordinator tryCreate(
            ThermalMemoryBudget serverBudget,
            int readyCapacity,
            int maxDimensions,
            int recoveryReserveSlots,
            long oldestPromotionTicks,
            int normalRunsPerRecovery
    ) {
        if (serverBudget == null) {
            throw new IllegalArgumentException("serverBudget is required");
        }
        if (readyCapacity <= 0 || maxDimensions <= 0) {
            throw new IllegalArgumentException(
                    "readyCapacity and maxDimensions must be positive");
        }
        if (recoveryReserveSlots < 0 || recoveryReserveSlots >= readyCapacity) {
            throw new IllegalArgumentException(
                    "recoveryReserveSlots must leave at least one normal slot");
        }
        if (oldestPromotionTicks < 0L || normalRunsPerRecovery <= 0) {
            throw new IllegalArgumentException(
                    "fairness limits must be non-negative and positive");
        }
        long reservedBytes = estimatedPayloadBytes(readyCapacity, maxDimensions);
        ThermalMemoryBudget.Reservation reservation = serverBudget.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, reservedBytes);
        return reservation == null
                ? null
                : new ThermalRuntimeCoordinator(
                        readyCapacity,
                        maxDimensions,
                        recoveryReserveSlots,
                        oldestPromotionTicks,
                        normalRunsPerRecovery,
                        reservation);
    }

    public static long estimatedPayloadBytes(int readyCapacity, int maxDimensions) {
        if (readyCapacity <= 0 || maxDimensions <= 0) {
            throw new IllegalArgumentException("capacities must be positive");
        }
        long readyBytes = Math.multiplyExact(
                readyCapacity,
                (long) Integer.BYTES + Long.BYTES + 1L);
        long dimensionBytes = Math.multiplyExact(
                maxDimensions,
                8L + 1L + 1L + 1L + 1L + Long.BYTES);
        return Math.addExact(readyBytes, dimensionBytes);
    }

    public enum RequestResult {
        QUEUED,
        COALESCED,
        DISPATCH_REOFFER_REQUIRED,
        NOT_REGISTERED,
        GENERATION_MISMATCH,
        CLOSED
    }

    public enum DispatchStatus {
        EXECUTED,
        EMPTY
    }

    public record DispatchResult(
            DispatchStatus status,
            long runtimeId,
            long dimensionGeneration,
            DimensionThermalRuntime.RunReport runReport
    ) {
        private static DispatchResult empty() {
            return new DispatchResult(DispatchStatus.EMPTY, 0L, -1L, null);
        }
    }

    public synchronized boolean register(DimensionThermalRuntime runtime) {
        if (runtime == null) {
            throw new IllegalArgumentException("runtime is required");
        }
        if (closed || findRuntime(runtime.runtimeId()) >= 0) {
            return false;
        }
        for (int slot = 0; slot < runtimes.length; slot++) {
            if (runtimes[slot] == null) {
                runtimes[slot] = runtime;
                return true;
            }
        }
        return false;
    }

    public synchronized RequestResult request(
            long runtimeId,
            long dimensionGeneration,
            boolean recovery,
            long currentTick
    ) {
        if (currentTick < 0L) {
            throw new IllegalArgumentException("currentTick must be non-negative");
        }
        if (closed) {
            return RequestResult.CLOSED;
        }
        int slot = findRuntime(runtimeId);
        if (slot < 0) {
            return RequestResult.NOT_REGISTERED;
        }
        if (runtimes[slot].dimensionGeneration() != dimensionGeneration) {
            return RequestResult.GENERATION_MISMATCH;
        }
        oldestRequestTicks[slot] = Math.min(oldestRequestTicks[slot], currentTick);
        recoveryRequested[slot] |= recovery;
        if (mailboxStates[slot] == RUNNING) {
            rerunRequested[slot] = true;
            return RequestResult.COALESCED;
        }
        if (mailboxStates[slot] == QUEUED) {
            if (recovery) {
                upgradeQueuedToRecovery(slot, dimensionGeneration);
            }
            return RequestResult.COALESCED;
        }
        if (tryOfferSlot(slot, recoveryRequested[slot])) {
            reofferRequired[slot] = false;
            return RequestResult.QUEUED;
        }
        reofferRequired[slot] = true;
        return RequestResult.DISPATCH_REOFFER_REQUIRED;
    }

    /** Executes one selected dimension outside the coordinator monitor. */
    public DispatchResult runNext(long currentTick) {
        if (currentTick < 0L) {
            throw new IllegalArgumentException("currentTick must be non-negative");
        }
        DimensionThermalRuntime runtime;
        int runtimeSlot;
        long generation;
        synchronized (this) {
            while (true) {
                if (closed || readySize == 0) {
                    return DispatchResult.empty();
                }
                int queueIndex = selectQueueIndex(currentTick);
                runtimeSlot = readySlots[queueIndex];
                generation = readyGenerations[queueIndex];
                boolean recovery = readyRecovery[queueIndex];
                removeQueueIndex(queueIndex);
                if (runtimeSlot < 0 || runtimeSlot >= runtimes.length) {
                    continue;
                }
                runtime = runtimes[runtimeSlot];
                if (runtime == null
                        || runtime.dimensionGeneration() != generation
                        || mailboxStates[runtimeSlot] != QUEUED) {
                    continue;
                }
                mailboxStates[runtimeSlot] = RUNNING;
                rerunRequested[runtimeSlot] = false;
                recoveryRequested[runtimeSlot] = false;
                if (recovery) {
                    normalRunsSinceRecovery = 0;
                } else {
                    normalRunsSinceRecovery++;
                }
                reofferStickyLocked(currentTick);
                break;
            }
        }

        DimensionThermalRuntime.RunReport report = runtime.runOne();

        if (runtime.unloaded()) {
            runtime.close();
        }

        synchronized (this) {
            if (runtimeSlot < runtimes.length
                    && runtimes[runtimeSlot] == runtime
                    && runtime.dimensionGeneration() == generation
                    && mailboxStates[runtimeSlot] == RUNNING) {
                mailboxStates[runtimeSlot] = IDLE;
                boolean needsAnotherRun = rerunRequested[runtimeSlot]
                        || runtime.hasReadyWork();
                if (needsAnotherRun) {
                    if (oldestRequestTicks[runtimeSlot] == Long.MAX_VALUE) {
                        oldestRequestTicks[runtimeSlot] = currentTick;
                    }
                    if (!tryOfferSlot(
                            runtimeSlot, recoveryRequested[runtimeSlot])) {
                        reofferRequired[runtimeSlot] = true;
                    }
                } else {
                    oldestRequestTicks[runtimeSlot] = Long.MAX_VALUE;
                    reofferRequired[runtimeSlot] = false;
                    recoveryRequested[runtimeSlot] = false;
                }
                reofferStickyLocked(currentTick);
            }
        }
        return new DispatchResult(
                DispatchStatus.EXECUTED,
                runtime.runtimeId(),
                generation,
                report);
    }

    public synchronized boolean unload(long runtimeId, long dimensionGeneration) {
        int slot = findRuntime(runtimeId);
        if (slot < 0
                || runtimes[slot].dimensionGeneration() != dimensionGeneration) {
            return false;
        }
        DimensionThermalRuntime runtime = runtimes[slot];
        boolean wasRunning = mailboxStates[slot] == RUNNING;
        runtime.unload();
        removeQueuedRuntime(slot, dimensionGeneration);
        runtimes[slot] = null;
        mailboxStates[slot] = IDLE;
        rerunRequested[slot] = false;
        recoveryRequested[slot] = false;
        reofferRequired[slot] = false;
        oldestRequestTicks[slot] = Long.MAX_VALUE;
        reofferStickyLocked(0L);
        if (!wasRunning) {
            runtime.close();
        }
        return true;
    }

    public synchronized int readyCount() {
        return readySize;
    }

    public synchronized int registeredDimensionCount() {
        int count = 0;
        for (DimensionThermalRuntime runtime : runtimes) {
            if (runtime != null) {
                count++;
            }
        }
        return count;
    }

    public synchronized boolean dispatchReofferRequired(
            long runtimeId,
            long dimensionGeneration
    ) {
        int slot = findRuntime(runtimeId);
        return slot >= 0
                && runtimes[slot].dimensionGeneration() == dimensionGeneration
                && reofferRequired[slot];
    }

    public synchronized String mailboxState(
            long runtimeId,
            long dimensionGeneration
    ) {
        int slot = findRuntime(runtimeId);
        if (slot < 0
                || runtimes[slot].dimensionGeneration() != dimensionGeneration) {
            return "UNREGISTERED";
        }
        return switch (mailboxStates[slot]) {
            case IDLE -> "IDLE";
            case QUEUED -> "QUEUED";
            case RUNNING -> "RUNNING";
            default -> throw new IllegalStateException("unknown mailbox state");
        };
    }

    public long reservedBytes() {
        return reservation.bytes();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (int slot = 0; slot < runtimes.length; slot++) {
            if (runtimes[slot] != null) {
                runtimes[slot].close();
                runtimes[slot] = null;
            }
            mailboxStates[slot] = IDLE;
        }
        readySize = 0;
        normalReadyCount = 0;
        reservation.close();
    }

    private int selectQueueIndex(long currentTick) {
        int oldestIndex = -1;
        long oldestTick = Long.MAX_VALUE;
        for (int index = 0; index < readySize; index++) {
            int slot = readySlots[index];
            long requested = oldestRequestTicks[slot];
            long age = currentTick >= requested ? currentTick - requested : 0L;
            if (age >= oldestPromotionTicks && requested < oldestTick) {
                oldestTick = requested;
                oldestIndex = index;
            }
        }
        if (oldestIndex >= 0) {
            return oldestIndex;
        }
        if (normalRunsSinceRecovery >= normalRunsPerRecovery) {
            for (int index = 0; index < readySize; index++) {
                if (readyRecovery[index]) {
                    return index;
                }
            }
        }
        return 0;
    }

    private boolean tryOfferSlot(int runtimeSlot, boolean recovery) {
        if (mailboxStates[runtimeSlot] != IDLE || readySize >= readyCapacity) {
            return false;
        }
        if (!recovery && normalReadyCount >= normalReadyCapacity) {
            return false;
        }
        readySlots[readySize] = runtimeSlot;
        readyGenerations[readySize] = runtimes[runtimeSlot].dimensionGeneration();
        readyRecovery[readySize] = recovery;
        readySize++;
        if (!recovery) {
            normalReadyCount++;
        }
        mailboxStates[runtimeSlot] = QUEUED;
        reofferRequired[runtimeSlot] = false;
        return true;
    }

    private void reofferStickyLocked(long currentTick) {
        for (int pass = 0; pass < 2; pass++) {
            boolean recoveryPass = pass == 0;
            for (int checked = 0; checked < runtimes.length; checked++) {
                int slot = (reofferCursor + checked) % runtimes.length;
                if (runtimes[slot] == null
                        || mailboxStates[slot] != IDLE
                        || !reofferRequired[slot]
                        || recoveryRequested[slot] != recoveryPass) {
                    continue;
                }
                if (oldestRequestTicks[slot] == Long.MAX_VALUE) {
                    oldestRequestTicks[slot] = currentTick;
                }
                if (tryOfferSlot(slot, recoveryPass)) {
                    reofferCursor = (slot + 1) % runtimes.length;
                }
                if (readySize >= readyCapacity
                        || !recoveryPass && normalReadyCount >= normalReadyCapacity) {
                    break;
                }
            }
        }
    }

    private void upgradeQueuedToRecovery(int runtimeSlot, long generation) {
        for (int index = 0; index < readySize; index++) {
            if (readySlots[index] == runtimeSlot
                    && readyGenerations[index] == generation
                    && !readyRecovery[index]) {
                readyRecovery[index] = true;
                normalReadyCount--;
                return;
            }
        }
    }

    private void removeQueuedRuntime(int runtimeSlot, long generation) {
        for (int index = readySize - 1; index >= 0; index--) {
            if (readySlots[index] == runtimeSlot
                    && readyGenerations[index] == generation) {
                removeQueueIndex(index);
            }
        }
    }

    private void removeQueueIndex(int queueIndex) {
        if (!readyRecovery[queueIndex]) {
            normalReadyCount--;
        }
        int moved = readySize - queueIndex - 1;
        if (moved > 0) {
            System.arraycopy(
                    readySlots, queueIndex + 1,
                    readySlots, queueIndex, moved);
            System.arraycopy(
                    readyGenerations, queueIndex + 1,
                    readyGenerations, queueIndex, moved);
            System.arraycopy(
                    readyRecovery, queueIndex + 1,
                    readyRecovery, queueIndex, moved);
        }
        readySize--;
    }

    private int findRuntime(long runtimeId) {
        for (int slot = 0; slot < runtimes.length; slot++) {
            if (runtimes[slot] != null
                    && runtimes[slot].runtimeId() == runtimeId) {
                return slot;
            }
        }
        return -1;
    }
}
