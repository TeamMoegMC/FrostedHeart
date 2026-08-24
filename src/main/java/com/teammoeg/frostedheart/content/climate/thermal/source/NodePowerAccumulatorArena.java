/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;

import java.util.Arrays;

/**
 * Primitive, node-keyed power accumulator.
 *
 * <p>Source events settle an affected node to their authoritative tick before
 * changing its aggregate power. A solve drains one exact {@code integral(Pdt)}
 * value regardless of worker cadence.</p>
 */
public final class NodePowerAccumulatorArena {
    public static final int NO_ACCUMULATOR = -1;

    private static final double TICKS_PER_SECOND = 20.0D;
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;

    private long[] nodeIds;
    private int[] lifecycleGenerations;
    private double[] currentPowerW;
    private double[] pendingEnergyJ;
    private double[] pendingCompensationJ;
    private long[] lastIntegralTicks;
    private int accumulatorCount;

    private long[] tableNodeIds;
    private int[] tableGenerations;
    private int[] tableSlots;
    private byte[] tableStates;
    private int resizeThreshold;

    public NodePowerAccumulatorArena(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        int storageCapacity = Math.max(1, initialCapacity);
        nodeIds = new long[storageCapacity];
        lifecycleGenerations = new int[storageCapacity];
        currentPowerW = new double[storageCapacity];
        pendingEnergyJ = new double[storageCapacity];
        pendingCompensationJ = new double[storageCapacity];
        lastIntegralTicks = new long[storageCapacity];
        allocateTable(tableCapacityFor(storageCapacity));
    }

    public int accumulatorCount() {
        return accumulatorCount;
    }

    /** Returns whether a solve would receive continuous or already-integrated energy. */
    public boolean hasActivePowerOrPendingEnergy() {
        for (int slot = 0; slot < accumulatorCount; slot++) {
            if (currentPowerW[slot] != 0.0D || pendingEnergyJ[slot] != 0.0D) {
                return true;
            }
        }
        return false;
    }

    public int ensureNode(long nodeId, int lifecycleGeneration, long initialTick) {
        requireGeneration(lifecycleGeneration);
        requireTick("initialTick", initialTick);
        int existing = findNode(nodeId, lifecycleGeneration);
        if (existing != NO_ACCUMULATOR) {
            return existing;
        }
        if (accumulatorCount >= resizeThreshold) {
            rehash(tableStates.length << 1);
        }
        ensureStorage(accumulatorCount + 1);
        int slot = accumulatorCount++;
        nodeIds[slot] = nodeId;
        lifecycleGenerations[slot] = lifecycleGeneration;
        lastIntegralTicks[slot] = initialTick;
        insertTable(nodeId, lifecycleGeneration, slot);
        return slot;
    }

    public int findNode(long nodeId, int lifecycleGeneration) {
        requireGeneration(lifecycleGeneration);
        int mask = tableStates.length - 1;
        int index = hash(nodeId, lifecycleGeneration) & mask;
        while (tableStates[index] != EMPTY) {
            if (tableNodeIds[index] == nodeId
                    && tableGenerations[index] == lifecycleGeneration) {
                return tableSlots[index];
            }
            index = (index + 1) & mask;
        }
        return NO_ACCUMULATOR;
    }

    /** Settles to {@code eventTick}, then changes the aggregate node power. */
    public void changePowerAt(int accumulatorSlot, long eventTick, double deltaPowerW) {
        requireFinite("deltaPowerW", deltaPowerW);
        settleTo(accumulatorSlot, eventTick);
        double updated = currentPowerW[accumulatorSlot] + deltaPowerW;
        requireFiniteResult("aggregate node power", updated);
        currentPowerW[accumulatorSlot] = canonicalZero(updated);
    }

    /** Adds a signed instantaneous energy event after settling prior power. */
    public void addImpulseAt(int accumulatorSlot, long eventTick, double energyJ) {
        requireFinite("energyJ", energyJ);
        settleTo(accumulatorSlot, eventTick);
        addPending(accumulatorSlot, energyJ);
    }

    /** Integrates current power to the requested tick without draining energy. */
    public double settleTo(int accumulatorSlot, long targetTick) {
        requireSlot(accumulatorSlot);
        requireTick("targetTick", targetTick);
        long previousTick = lastIntegralTicks[accumulatorSlot];
        if (targetTick < previousTick) {
            throw new IllegalArgumentException(
                    "targetTick precedes node integral tick: "
                            + targetTick + " < " + previousTick);
        }
        if (targetTick == previousTick) {
            return 0.0D;
        }
        double energyJ = finiteProduct(
                "integrated node energy",
                currentPowerW[accumulatorSlot],
                (targetTick - previousTick) / TICKS_PER_SECOND
        );
        addPending(accumulatorSlot, energyJ);
        lastIntegralTicks[accumulatorSlot] = targetTick;
        return energyJ;
    }

    /** Settles and transfers ownership of all pending energy to the caller. */
    public double drainPendingEnergyTo(int accumulatorSlot, long targetTick) {
        settleTo(accumulatorSlot, targetTick);
        double drained = pendingEnergyJ[accumulatorSlot];
        pendingEnergyJ[accumulatorSlot] = 0.0D;
        pendingCompensationJ[accumulatorSlot] = 0.0D;
        return canonicalZero(drained);
    }

    /** Drains each node once; this is the stable solver-facing source traversal. */
    public double drainAllPendingEnergyTo(long targetTick, NodeEnergyConsumer consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("consumer is required");
        }
        double total = 0.0D;
        double compensation = 0.0D;
        for (int slot = 0; slot < accumulatorCount; slot++) {
            settleTo(slot, targetTick);
            double energyJ = pendingEnergyJ[slot];
            if (energyJ == 0.0D) {
                continue;
            }
            double adjusted = energyJ - compensation;
            double updated = total + adjusted;
            requireFiniteResult("drained source energy", updated);
            consumer.accept(
                    slot,
                    nodeIds[slot],
                    lifecycleGenerations[slot],
                    energyJ
            );
            pendingEnergyJ[slot] = 0.0D;
            pendingCompensationJ[slot] = 0.0D;
            compensation = (updated - total) - adjusted;
            total = updated;
        }
        return canonicalZero(total);
    }

    /** Drains directly into the authoritative cell arena used by transport. */
    public double drainAllPendingEnergyTo(
            long targetTick,
            ThermalCellArena destination
    ) {
        if (destination == null) {
            throw new IllegalArgumentException("destination arena is required");
        }
        for (int slot = 0; slot < accumulatorCount; slot++) {
            settleTo(slot, targetTick);
            double energyJ = pendingEnergyJ[slot];
            if (energyJ == 0.0D) {
                continue;
            }
            destination.requireNodeEnthalpyWrite(
                    nodeIds[slot], lifecycleGenerations[slot], energyJ);
        }

        double total = 0.0D;
        double compensation = 0.0D;
        for (int slot = 0; slot < accumulatorCount; slot++) {
            double energyJ = pendingEnergyJ[slot];
            if (energyJ == 0.0D) {
                continue;
            }
            double adjusted = energyJ - compensation;
            double updated = total + adjusted;
            requireFiniteResult("drained source energy", updated);
            destination.addNodeEnthalpyJ(
                    nodeIds[slot], lifecycleGenerations[slot], energyJ);
            pendingEnergyJ[slot] = 0.0D;
            pendingCompensationJ[slot] = 0.0D;
            compensation = (updated - total) - adjusted;
            total = updated;
        }
        return canonicalZero(total);
    }

    public long nodeId(int accumulatorSlot) {
        requireSlot(accumulatorSlot);
        return nodeIds[accumulatorSlot];
    }

    public int lifecycleGeneration(int accumulatorSlot) {
        requireSlot(accumulatorSlot);
        return lifecycleGenerations[accumulatorSlot];
    }

    public double currentPowerW(int accumulatorSlot) {
        requireSlot(accumulatorSlot);
        return currentPowerW[accumulatorSlot];
    }

    public double pendingEnergyJ(int accumulatorSlot) {
        requireSlot(accumulatorSlot);
        return pendingEnergyJ[accumulatorSlot];
    }

    public long lastIntegralTick(int accumulatorSlot) {
        requireSlot(accumulatorSlot);
        return lastIntegralTicks[accumulatorSlot];
    }

    private void addPending(int slot, double energyJ) {
        double adjusted = energyJ - pendingCompensationJ[slot];
        double updated = pendingEnergyJ[slot] + adjusted;
        requireFiniteResult("pending node energy", updated);
        pendingCompensationJ[slot] = (updated - pendingEnergyJ[slot]) - adjusted;
        pendingEnergyJ[slot] = canonicalZero(updated);
    }

    private void ensureStorage(int requiredCapacity) {
        if (requiredCapacity <= nodeIds.length) {
            return;
        }
        int newCapacity = Math.max(requiredCapacity, nodeIds.length << 1);
        nodeIds = Arrays.copyOf(nodeIds, newCapacity);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, newCapacity);
        currentPowerW = Arrays.copyOf(currentPowerW, newCapacity);
        pendingEnergyJ = Arrays.copyOf(pendingEnergyJ, newCapacity);
        pendingCompensationJ = Arrays.copyOf(pendingCompensationJ, newCapacity);
        lastIntegralTicks = Arrays.copyOf(lastIntegralTicks, newCapacity);
    }

    private void allocateTable(int capacity) {
        tableNodeIds = new long[capacity];
        tableGenerations = new int[capacity];
        tableSlots = new int[capacity];
        tableStates = new byte[capacity];
        resizeThreshold = Math.max(1, (int) (capacity * 0.6D));
    }

    private void rehash(int requestedCapacity) {
        long[] oldNodeIds = tableNodeIds;
        int[] oldGenerations = tableGenerations;
        int[] oldSlots = tableSlots;
        byte[] oldStates = tableStates;
        allocateTable(nextPowerOfTwo(requestedCapacity));
        for (int index = 0; index < oldStates.length; index++) {
            if (oldStates[index] == OCCUPIED) {
                insertTable(oldNodeIds[index], oldGenerations[index], oldSlots[index]);
            }
        }
    }

    private void insertTable(long nodeId, int lifecycleGeneration, int slot) {
        int mask = tableStates.length - 1;
        int index = hash(nodeId, lifecycleGeneration) & mask;
        while (tableStates[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }
        tableStates[index] = OCCUPIED;
        tableNodeIds[index] = nodeId;
        tableGenerations[index] = lifecycleGeneration;
        tableSlots[index] = slot;
    }

    private void requireSlot(int slot) {
        if (slot < 0 || slot >= accumulatorCount) {
            throw new IllegalArgumentException("invalid accumulator slot: " + slot);
        }
    }

    private static int tableCapacityFor(int expectedEntries) {
        return nextPowerOfTwo(Math.max(4, (int) Math.ceil(expectedEntries / 0.6D)));
    }

    private static int nextPowerOfTwo(int value) {
        int highest = Integer.highestOneBit(Math.max(2, value - 1));
        if (highest >= 1 << 29) {
            throw new IllegalArgumentException("node accumulator table is too large");
        }
        return highest << 1;
    }

    private static int hash(long nodeId, int lifecycleGeneration) {
        long mixed = nodeId ^ (0x9E3779B97F4A7C15L * lifecycleGeneration);
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (int) mixed;
    }

    private static void requireGeneration(int lifecycleGeneration) {
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
    }

    private static void requireTick(String name, long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static double finiteProduct(String name, double left, double right) {
        double result = left * right;
        requireFiniteResult(name, result);
        return result;
    }

    private static void requireFiniteResult(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " exceeded the finite source domain");
        }
    }

    private static double canonicalZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }

    @FunctionalInterface
    public interface NodeEnergyConsumer {
        /** Must return only after the destination owns the complete energy value. */
        void accept(
                int accumulatorSlot,
                long nodeId,
                int lifecycleGeneration,
                double signedEnergyJ
        );
    }
}
