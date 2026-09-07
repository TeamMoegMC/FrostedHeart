/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.Arrays;

/** Recyclable primitive node-power ledger with O(active nodes) delivery. */
public final class NodePowerAccumulatorArena {
    private static final int NO_ACCUMULATOR = -1;

    private static final double TICKS_PER_SECOND = 20.0D;
    private long[] nodeIds;
    private int[] lifecycleGenerations;
    private double[] currentPowerW;
    private double[] pendingEnergyJ;
    private double[] pendingCompensationJ;
    private long[] lastIntegralTicks;
    private int[] bindingReferences;
    private int[] nextFree;
    private int[] activePrevious;
    private int[] activeNext;
    private byte[] occupied;
    private byte[] active;

    private final Long2IntOpenHashMap slotsByNode;
    private final int maximumCapacity;

    private int highWaterMark;
    private int liveCount;
    private int freeHead = NO_ACCUMULATOR;
    private int activeHead = NO_ACCUMULATOR;
    private int activeTail = NO_ACCUMULATOR;

    public NodePowerAccumulatorArena(int initialCapacity, int maximumCapacity) {
        if (initialCapacity < 0 || maximumCapacity <= 0
                || initialCapacity > maximumCapacity) {
            throw new IllegalArgumentException(
                    "accumulator capacity limits are invalid");
        }
        this.maximumCapacity = maximumCapacity;
        allocateStorage(Math.max(1, initialCapacity));
        slotsByNode = new Long2IntOpenHashMap(Math.max(1, initialCapacity));
        slotsByNode.defaultReturnValue(NO_ACCUMULATOR);
    }

    public boolean hasActivePowerOrPendingEnergy() {
        return activeHead != NO_ACCUMULATOR;
    }

    public void reserveAdditional(int additional) {
        if (additional < 0) {
            throw new IllegalArgumentException("additional accumulator count is negative");
        }
        int required = Math.addExact(liveCount, additional);
        if (required > maximumCapacity) {
            throw new IllegalStateException(
                    "thermal source node limit reached");
        }
        ensureStorage(required);
    }

    public int ensureNode(long nodeId, int lifecycleGeneration, long initialTick) {
        requireGeneration(lifecycleGeneration);
        requireTick("initialTick", initialTick);
        int existing = findNode(nodeId, lifecycleGeneration);
        if (existing != NO_ACCUMULATOR) {
            return existing;
        }
        int slot = allocateSlot();
        nodeIds[slot] = nodeId;
        lifecycleGenerations[slot] = lifecycleGeneration;
        lastIntegralTicks[slot] = initialTick;
        occupied[slot] = 1;
        liveCount++;
        slotsByNode.put(key(nodeId, lifecycleGeneration), slot);
        return slot;
    }

    public int findNode(long nodeId, int lifecycleGeneration) {
        requireGeneration(lifecycleGeneration);
        return slotsByNode.get(key(nodeId, lifecycleGeneration));
    }

    public void retainBinding(
            long nodeId,
            int lifecycleGeneration,
            long eventTick
    ) {
        int slot = ensureNode(nodeId, lifecycleGeneration, eventTick);
        settleTo(slot, eventTick);
        bindingReferences[slot] = Math.incrementExact(bindingReferences[slot]);
    }

    public void releaseBinding(
            long nodeId,
            int lifecycleGeneration,
            long eventTick
    ) {
        int slot = findNode(nodeId, lifecycleGeneration);
        if (slot == NO_ACCUMULATOR || bindingReferences[slot] <= 0) {
            throw new IllegalStateException("thermal node binding reference underflow");
        }
        settleTo(slot, eventTick);
        bindingReferences[slot]--;
        reclaimIfIdle(slot);
    }

    public boolean referencesNode(long nodeId, int lifecycleGeneration) {
        int slot = findNode(nodeId, lifecycleGeneration);
        return slot != NO_ACCUMULATOR
                && (bindingReferences[slot] != 0
                || currentPowerW[slot] != 0.0D
                || pendingEnergyJ[slot] != 0.0D);
    }

    public void changePowerAt(int slot, long eventTick, double deltaPowerW) {
        requireFinite("deltaPowerW", deltaPowerW);
        settleTo(slot, eventTick);
        double updated = currentPowerW[slot] + deltaPowerW;
        requireFiniteResult("aggregate node power", updated);
        currentPowerW[slot] = canonicalZero(updated);
        refreshActive(slot);
        reclaimIfIdle(slot);
    }

    public void addImpulseAt(int slot, long eventTick, double energyJ) {
        requireFinite("energyJ", energyJ);
        settleTo(slot, eventTick);
        addPending(slot, energyJ);
        refreshActive(slot);
    }

    public double settleTo(int slot, long targetTick) {
        requireSlot(slot);
        requireTick("targetTick", targetTick);
        long previousTick = lastIntegralTicks[slot];
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
                currentPowerW[slot],
                (targetTick - previousTick) / TICKS_PER_SECOND);
        addPending(slot, energyJ);
        lastIntegralTicks[slot] = targetTick;
        refreshActive(slot);
        return energyJ;
    }

    public void drainAllPendingEnergyTo(
            long targetTick,
            ThermalCellArena destination
    ) {
        if (destination == null) {
            throw new IllegalArgumentException("destination arena is required");
        }
        int slot = activeHead;
        while (slot != NO_ACCUMULATOR) {
            int next = activeNext[slot];
            settleTo(slot, targetTick);
            double energyJ = pendingEnergyJ[slot];
            if (energyJ != 0.0D) {
                destination.addNodeEnthalpyJ(
                        nodeIds[slot], lifecycleGenerations[slot], energyJ);
                pendingEnergyJ[slot] = 0.0D;
                pendingCompensationJ[slot] = 0.0D;
            }
            refreshActive(slot);
            reclaimIfIdle(slot);
            slot = next;
        }
    }

    private int allocateSlot() {
        if (freeHead != NO_ACCUMULATOR) {
            int slot = freeHead;
            freeHead = nextFree[slot];
            nextFree[slot] = NO_ACCUMULATOR;
            return slot;
        }
        ensureStorage(highWaterMark + 1);
        return highWaterMark++;
    }

    private void reclaimIfIdle(int slot) {
        if (occupied[slot] == 0
                || bindingReferences[slot] != 0
                || currentPowerW[slot] != 0.0D
                || pendingEnergyJ[slot] != 0.0D) {
            return;
        }
        deactivate(slot);
        slotsByNode.remove(key(nodeIds[slot], lifecycleGenerations[slot]));
        occupied[slot] = 0;
        nodeIds[slot] = 0L;
        lifecycleGenerations[slot] = 0;
        lastIntegralTicks[slot] = 0L;
        nextFree[slot] = freeHead;
        freeHead = slot;
        liveCount--;
    }

    private void addPending(int slot, double energyJ) {
        double adjusted = energyJ - pendingCompensationJ[slot];
        double updated = pendingEnergyJ[slot] + adjusted;
        requireFiniteResult("pending node energy", updated);
        pendingCompensationJ[slot] = (updated - pendingEnergyJ[slot]) - adjusted;
        pendingEnergyJ[slot] = canonicalZero(updated);
    }

    private void refreshActive(int slot) {
        boolean shouldBeActive = currentPowerW[slot] != 0.0D
                || pendingEnergyJ[slot] != 0.0D;
        if (shouldBeActive && active[slot] == 0) {
            active[slot] = 1;
            activePrevious[slot] = activeTail;
            activeNext[slot] = NO_ACCUMULATOR;
            if (activeTail == NO_ACCUMULATOR) {
                activeHead = slot;
            } else {
                activeNext[activeTail] = slot;
            }
            activeTail = slot;
        } else if (!shouldBeActive) {
            deactivate(slot);
        }
    }

    private void deactivate(int slot) {
        if (active[slot] == 0) {
            return;
        }
        int previous = activePrevious[slot];
        int next = activeNext[slot];
        if (previous == NO_ACCUMULATOR) {
            activeHead = next;
        } else {
            activeNext[previous] = next;
        }
        if (next == NO_ACCUMULATOR) {
            activeTail = previous;
        } else {
            activePrevious[next] = previous;
        }
        active[slot] = 0;
        activePrevious[slot] = NO_ACCUMULATOR;
        activeNext[slot] = NO_ACCUMULATOR;
    }

    private void allocateStorage(int capacity) {
        nodeIds = new long[capacity];
        lifecycleGenerations = new int[capacity];
        currentPowerW = new double[capacity];
        pendingEnergyJ = new double[capacity];
        pendingCompensationJ = new double[capacity];
        lastIntegralTicks = new long[capacity];
        bindingReferences = new int[capacity];
        nextFree = new int[capacity];
        activePrevious = new int[capacity];
        activeNext = new int[capacity];
        occupied = new byte[capacity];
        active = new byte[capacity];
        Arrays.fill(nextFree, NO_ACCUMULATOR);
        Arrays.fill(activePrevious, NO_ACCUMULATOR);
        Arrays.fill(activeNext, NO_ACCUMULATOR);
    }

    private void ensureStorage(int required) {
        if (required <= nodeIds.length) {
            return;
        }
        int old = nodeIds.length;
        if (required > maximumCapacity) {
            throw new IllegalStateException(
                    "thermal source node limit reached");
        }
        int capacity = Math.min(
                maximumCapacity,
                Math.max(required, old + Math.max(8, old >>> 1)));
        nodeIds = Arrays.copyOf(nodeIds, capacity);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, capacity);
        currentPowerW = Arrays.copyOf(currentPowerW, capacity);
        pendingEnergyJ = Arrays.copyOf(pendingEnergyJ, capacity);
        pendingCompensationJ = Arrays.copyOf(pendingCompensationJ, capacity);
        lastIntegralTicks = Arrays.copyOf(lastIntegralTicks, capacity);
        bindingReferences = Arrays.copyOf(bindingReferences, capacity);
        nextFree = Arrays.copyOf(nextFree, capacity);
        activePrevious = Arrays.copyOf(activePrevious, capacity);
        activeNext = Arrays.copyOf(activeNext, capacity);
        occupied = Arrays.copyOf(occupied, capacity);
        active = Arrays.copyOf(active, capacity);
        Arrays.fill(nextFree, old, capacity, NO_ACCUMULATOR);
        Arrays.fill(activePrevious, old, capacity, NO_ACCUMULATOR);
        Arrays.fill(activeNext, old, capacity, NO_ACCUMULATOR);
    }

    private void requireSlot(int slot) {
        if (slot < 0 || slot >= highWaterMark || occupied[slot] == 0) {
            throw new IllegalArgumentException("invalid accumulator slot: " + slot);
        }
    }

    private static long key(long nodeId, int generation) {
        if (nodeId < 0L || nodeId > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "thermal node ID must be an arena slot");
        }
        return (long) generation << 32 | nodeId;
    }

    private static void requireGeneration(int generation) {
        if (generation < 0) {
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
}
