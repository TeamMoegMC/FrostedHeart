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
import java.util.Objects;

/**
 * Worker-owned source identity, exact event-time power, binding, and delivery
 * authority for one dimension.
 */
public final class ThermalSourceLedger implements AutoCloseable {
    private static final int NO_SOURCE = -1;

    private static final double SHARE_TOLERANCE = 1.0e-12D;
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte DELETED = 2;
    private static final ThermalSourceMode[] SOURCE_MODES =
            ThermalSourceMode.values();

    private final int maxPortsPerSource;
    private final int maximumSources;
    private final NodePowerAccumulatorArena accumulators;
    private final ThermalCellArena destination;

    private long[] sourceIds;
    private int[] lifecycleGenerations;
    private byte[] sourceModes;
    private byte[] enabled;
    private byte[] occupied;
    private int[] portCounts;
    private double[] declaredPowerW;
    private long[] lastEventTicks;
    private int[] nextFree;

    private int[] portIds;
    private double[] portPowerShares;
    private double[] portContributionW;
    private byte[] portBindingKinds;
    private long[] portTargetIds;
    private int[] portBindingGenerations;

    private long[] tableSourceIds;
    private int[] tableSlots;
    private byte[] tableStates;
    private int tableUsed;
    private int tableResizeThreshold;

    private int highWaterMark;
    private int liveSourceCount;
    private int freeHead = NO_SOURCE;
    private long cursorTick;
    private boolean closed;

    public ThermalSourceLedger(
            long initialTick,
            int initialSourceCapacity,
            int maxPortsPerSource,
            int maximumSources,
            NodePowerAccumulatorArena accumulators,
            ThermalCellArena destination
    ) {
        if (initialTick < 0L
                || initialSourceCapacity < 0 || maxPortsPerSource <= 0
                || maximumSources <= 0
                || initialSourceCapacity > maximumSources) {
            throw new IllegalArgumentException("source ledger configuration is invalid");
        }
        this.maxPortsPerSource = maxPortsPerSource;
        this.maximumSources = maximumSources;
        this.accumulators = Objects.requireNonNull(accumulators, "accumulators");
        this.destination = Objects.requireNonNull(destination, "destination");
        int capacity = Math.max(1, initialSourceCapacity);
        allocateStorage(capacity);
        allocateTable(tableCapacity(capacity));
        cursorTick = initialTick;
    }

    public boolean hasActivePowerOrPendingEnergy() {
        return accumulators.hasActivePowerOrPendingEnergy();
    }

    /**
     * Applies one immutable source cut in stable event order and delivers all
     * accumulated energy through {@code targetTick}.
     */
    public void acceptAndAdvance(
            ThermalSourceBatch batch,
            long targetTick,
            EventObserver observer
    ) {
        requireOpen();
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(observer, "observer");
        if (targetTick < cursorTick) {
            throw new IllegalArgumentException("source target tick precedes the cursor");
        }
        reserveBatch(batch);
        long previousTick = cursorTick;
        for (int index = 0; index < batch.size(); index++) {
            long eventTick = batch.effectiveTick(index);
            if (eventTick < previousTick || eventTick > targetTick) {
                throw new IllegalArgumentException(
                        "source events are outside their ordered batch interval");
            }
            cursorTick = eventTick;
            applyEvent(batch, index);
            observer.afterEvent(batch, index, this);
            previousTick = eventTick;
        }
        deliverThrough(targetTick);
    }

    /** Delivers continuous and pending energy without accepting source changes. */
    public void deliverThrough(long targetTick) {
        requireOpen();
        if (targetTick < cursorTick) {
            throw new IllegalArgumentException("source target tick precedes the cursor");
        }
        accumulators.drainAllPendingEnergyTo(targetTick, destination);
        cursorTick = targetTick;
    }

    /** Topology rebind at the already-settled source cursor. */
    public boolean rebindAtCursor(
            long sourceId,
            int expectedLifecycleGeneration,
            int portId,
            SourceBinding nextBinding
    ) {
        Objects.requireNonNull(nextBinding, "nextBinding");
        int slot = findSource(sourceId);
        if (slot == NO_SOURCE
                || lifecycleGenerations[slot] != expectedLifecycleGeneration) {
            return false;
        }
        int portOffset = requirePortOffset(slot, portId);
        if (!sameBinding(portOffset, nextBinding)) {
            movePortBinding(portOffset, nextBinding, cursorTick);
        }
        return true;
    }

    /** O(1) reference check for one exact arena node identity. */
    public boolean referencesThermalNode(long nodeId, int lifecycleGeneration) {
        return accumulators.referencesNode(nodeId, lifecycleGeneration);
    }

    private void reserveBatch(ThermalSourceBatch batch) {
        int registrations = 0;
        int thermalBindings = 0;
        for (int index = 0; index < batch.size(); index++) {
            if (batch.kind(index) == ThermalSourceBatch.Kind.REGISTER
                    && findSource(batch.sourceId(index)) == NO_SOURCE) {
                registrations++;
            }
            if (batch.kind(index) == ThermalSourceBatch.Kind.REGISTER) {
                for (EmissionPort port : batch.ports(index)) {
                    if (port.binding().isThermalNode()) {
                        thermalBindings++;
                    }
                }
            }
        }
        ensureStorageForAdditional(registrations);
        reserveTable(registrations);
        accumulators.reserveAdditional(thermalBindings);
    }

    private void applyEvent(ThermalSourceBatch batch, int index) {
        long sourceId = batch.sourceId(index);
        long eventTick = batch.effectiveTick(index);
        switch (batch.kind(index)) {
            case REGISTER -> registerSource(
                    sourceId,
                    batch.lifecycleGeneration(index),
                    batch.mode(index),
                    batch.value(index),
                    batch.enabled(index),
                    eventTick,
                    batch.ports(index));
            case POWER_CHANGE -> setPower(sourceId, batch.value(index), eventTick);
            case ENABLED_CHANGE -> setEnabled(
                    sourceId, batch.enabled(index), eventTick);
            case IMPULSE -> applyImpulse(
                    sourceId,
                    batch.portId(index),
                    batch.value(index),
                    eventTick);
            case UNLOAD -> unloadSource(
                    sourceId, batch.lifecycleGeneration(index), eventTick);
        }
    }

    private void registerSource(
            long sourceId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean sourceEnabled,
            long eventTick,
            EmissionPort[] ports
    ) {
        Objects.requireNonNull(mode, "mode");
        requireFinite("powerW", powerW);
        if (lifecycleGeneration < 0
                || mode == ThermalSourceMode.IMPULSE && powerW != 0.0D) {
            throw new IllegalArgumentException("source registration is invalid");
        }
        validatePorts(ports);
        if (findSource(sourceId) != NO_SOURCE) {
            throw new IllegalArgumentException("sourceId is already registered: " + sourceId);
        }
        int slot = allocateSlot();
        sourceIds[slot] = sourceId;
        lifecycleGenerations[slot] = lifecycleGeneration;
        sourceModes[slot] = (byte) mode.ordinal();
        enabled[slot] = sourceEnabled ? (byte) 1 : 0;
        occupied[slot] = 1;
        portCounts[slot] = ports.length;
        declaredPowerW[slot] = canonicalZero(powerW);
        lastEventTicks[slot] = eventTick;
        int firstPort = portBase(slot);
        for (int index = 0; index < ports.length; index++) {
            writePort(firstPort + index, ports[index], eventTick);
        }
        liveSourceCount++;
        insertTable(sourceId, slot);
        installEffectivePower(slot, eventTick, effectivePower(slot));
    }

    private void setPower(long sourceId, double powerW, long eventTick) {
        requireFinite("powerW", powerW);
        int slot = requireEventSource(sourceId, eventTick);
        if (mode(slot) != ThermalSourceMode.POWER_SOURCE) {
            throw new IllegalStateException("only POWER_SOURCE holds continuous power");
        }
        declaredPowerW[slot] = canonicalZero(powerW);
        installEffectivePower(slot, eventTick, effectivePower(slot));
    }

    private void setEnabled(long sourceId, boolean sourceEnabled, long eventTick) {
        int slot = requireEventSource(sourceId, eventTick);
        enabled[slot] = sourceEnabled ? (byte) 1 : 0;
        installEffectivePower(slot, eventTick, effectivePower(slot));
    }

    private void applyImpulse(
            long sourceId,
            int portId,
            double energyJ,
            long eventTick
    ) {
        requireFinite("energyJ", energyJ);
        int slot = requireEventSource(sourceId, eventTick);
        if (mode(slot) != ThermalSourceMode.IMPULSE) {
            throw new IllegalStateException("only IMPULSE accepts impulse events");
        }
        int portOffset = requirePortOffset(slot, portId);
        if (isThermalNode(portOffset) && energyJ != 0.0D) {
            int accumulator = accumulators.ensureNode(
                    portTargetIds[portOffset],
                    portBindingGenerations[portOffset],
                    eventTick);
            accumulators.addImpulseAt(accumulator, eventTick, energyJ);
        }
    }

    private void unloadSource(
            long sourceId,
            int expectedLifecycleGeneration,
            long eventTick
    ) {
        int slot = findSource(sourceId);
        if (slot == NO_SOURCE
                || lifecycleGenerations[slot] != expectedLifecycleGeneration) {
            return;
        }
        requireEventTick(slot, eventTick);
        int firstPort = portBase(slot);
        for (int index = 0; index < portCounts[slot]; index++) {
            int portOffset = firstPort + index;
            double contribution = portContributionW[portOffset];
            if (isThermalNode(portOffset)) {
                if (contribution != 0.0D) {
                    changeNodePower(
                            portTargetIds[portOffset],
                            portBindingGenerations[portOffset],
                            eventTick,
                            -contribution);
                }
                accumulators.releaseBinding(
                        portTargetIds[portOffset],
                        portBindingGenerations[portOffset],
                        eventTick);
            }
            clearPort(portOffset);
        }
        removeTable(sourceId);
        clearSource(slot);
    }

    private void installEffectivePower(
            int sourceSlot,
            long eventTick,
            double totalPowerW
    ) {
        int count = portCounts[sourceSlot];
        int firstPort = portBase(sourceSlot);
        double assigned = 0.0D;
        for (int index = 0; index < count; index++) {
            int portOffset = firstPort + index;
            double contribution = index == count - 1
                    ? totalPowerW - assigned
                    : finiteProduct(
                            "source port power",
                            totalPowerW,
                            portPowerShares[portOffset]);
            if (index != count - 1) {
                assigned = finiteSum(
                        "assigned source power", assigned, contribution);
            }
            double delta = contribution - portContributionW[portOffset];
            if (isThermalNode(portOffset) && delta != 0.0D) {
                changeNodePower(
                        portTargetIds[portOffset],
                        portBindingGenerations[portOffset],
                        eventTick,
                        delta);
            }
            portContributionW[portOffset] = canonicalZero(contribution);
        }
    }

    private void movePortBinding(
            int portOffset,
            SourceBinding next,
            long eventTick
    ) {
        double contribution = portContributionW[portOffset];
        if (isThermalNode(portOffset)) {
            if (contribution != 0.0D) {
                changeNodePower(
                        portTargetIds[portOffset],
                        portBindingGenerations[portOffset],
                        eventTick,
                        -contribution);
            }
            accumulators.releaseBinding(
                    portTargetIds[portOffset],
                    portBindingGenerations[portOffset],
                    eventTick);
        }
        if (next.isThermalNode()) {
            accumulators.retainBinding(
                    next.targetId(), next.lifecycleGeneration(), eventTick);
            if (contribution != 0.0D) {
                changeNodePower(
                        next.targetId(),
                        next.lifecycleGeneration(),
                        eventTick,
                        contribution);
            }
        }
        writeBinding(portOffset, next);
    }

    private void changeNodePower(
            long targetId,
            int generation,
            long eventTick,
            double deltaPowerW
    ) {
        int accumulator = accumulators.ensureNode(
                targetId, generation, eventTick);
        accumulators.changePowerAt(accumulator, eventTick, deltaPowerW);
    }

    private void writePort(int offset, EmissionPort port, long eventTick) {
        portIds[offset] = port.portId();
        portPowerShares[offset] = port.powerShare();
        writeBinding(offset, port.binding());
        if (port.binding().isThermalNode()) {
            accumulators.retainBinding(
                    port.binding().targetId(),
                    port.binding().lifecycleGeneration(),
                    eventTick);
        }
    }

    private void clearPort(int offset) {
        portIds[offset] = 0;
        portPowerShares[offset] = 0.0D;
        portContributionW[offset] = 0.0D;
        portBindingKinds[offset] = 0;
        portTargetIds[offset] = 0L;
        portBindingGenerations[offset] = 0;
    }

    private void writeBinding(int offset, SourceBinding binding) {
        portBindingKinds[offset] = (byte) binding.kind().ordinal();
        portTargetIds[offset] = binding.targetId();
        portBindingGenerations[offset] = binding.lifecycleGeneration();
    }

    private boolean isThermalNode(int offset) {
        return portBindingKinds[offset]
                == (byte) SourceBinding.Kind.THERMAL_NODE.ordinal();
    }

    private boolean sameBinding(int offset, SourceBinding binding) {
        return portBindingKinds[offset] == (byte) binding.kind().ordinal()
                && portTargetIds[offset] == binding.targetId()
                && portBindingGenerations[offset]
                        == binding.lifecycleGeneration();
    }

    private void validatePorts(EmissionPort[] ports) {
        Objects.requireNonNull(ports, "ports");
        if (ports.length == 0 || ports.length > maxPortsPerSource) {
            throw new IllegalArgumentException(
                    "ports must contain 1.." + maxPortsPerSource + " entries");
        }
        double share = 0.0D;
        for (int index = 0; index < ports.length; index++) {
            EmissionPort port = Objects.requireNonNull(
                    ports[index], "ports contains null");
            share = finiteSum("source port shares", share, port.powerShare());
            for (int earlier = 0; earlier < index; earlier++) {
                if (ports[earlier].portId() == port.portId()) {
                    throw new IllegalArgumentException(
                            "duplicate source portId: " + port.portId());
                }
            }
        }
        if (Math.abs(share - 1.0D) > SHARE_TOLERANCE) {
            throw new IllegalArgumentException(
                    "source port power shares must sum to one");
        }
    }

    private int requireEventSource(long sourceId, long eventTick) {
        int slot = requireSource(sourceId);
        requireEventTick(slot, eventTick);
        return slot;
    }

    private void requireEventTick(int sourceSlot, long eventTick) {
        if (eventTick < lastEventTicks[sourceSlot] || eventTick != cursorTick) {
            throw new IllegalArgumentException(
                    "source event is not contiguous with its ledger cursor");
        }
        lastEventTicks[sourceSlot] = eventTick;
    }

    private int requireSource(long sourceId) {
        int slot = findSource(sourceId);
        if (slot == NO_SOURCE) {
            throw new IllegalArgumentException("unknown sourceId: " + sourceId);
        }
        return slot;
    }

    private int requirePortOffset(int sourceSlot, int portId) {
        int firstPort = portBase(sourceSlot);
        for (int index = 0; index < portCounts[sourceSlot]; index++) {
            if (portIds[firstPort + index] == portId) {
                return firstPort + index;
            }
        }
        throw new IllegalArgumentException(
                "source " + sourceIds[sourceSlot] + " has no port " + portId);
    }

    private ThermalSourceMode mode(int sourceSlot) {
        return SOURCE_MODES[Byte.toUnsignedInt(sourceModes[sourceSlot])];
    }

    private double effectivePower(int sourceSlot) {
        return enabled[sourceSlot] != 0
                && mode(sourceSlot) == ThermalSourceMode.POWER_SOURCE
                ? declaredPowerW[sourceSlot]
                : 0.0D;
    }

    private int findSource(long sourceId) {
        int index = findTableIndex(sourceId);
        return index < 0 ? NO_SOURCE : tableSlots[index];
    }

    private int findTableIndex(long sourceId) {
        int mask = tableStates.length - 1;
        int index = hash(sourceId) & mask;
        while (tableStates[index] != EMPTY) {
            if (tableStates[index] == OCCUPIED
                    && tableSourceIds[index] == sourceId) {
                return index;
            }
            index = index + 1 & mask;
        }
        return -1;
    }

    private void insertTable(long sourceId, int slot) {
        int mask = tableStates.length - 1;
        int index = hash(sourceId) & mask;
        int deleted = -1;
        while (tableStates[index] != EMPTY) {
            if (tableStates[index] == DELETED && deleted < 0) {
                deleted = index;
            }
            index = index + 1 & mask;
        }
        int destination = deleted >= 0 ? deleted : index;
        if (tableStates[destination] == EMPTY) {
            if (tableUsed >= tableResizeThreshold) {
                throw new IllegalStateException(
                        "source index capacity was not reserved");
            }
            tableUsed++;
        }
        tableStates[destination] = OCCUPIED;
        tableSourceIds[destination] = sourceId;
        tableSlots[destination] = slot;
    }

    private void removeTable(long sourceId) {
        int index = findTableIndex(sourceId);
        if (index < 0) {
            throw new IllegalStateException("source index is missing");
        }
        tableStates[index] = DELETED;
        tableSlots[index] = NO_SOURCE;
    }

    private void reserveTable(int additional) {
        int required = Math.addExact(liveSourceCount, additional);
        if (required > tableResizeThreshold) {
            rehash(tableCapacity(required));
        } else if (tableUsed + additional > tableResizeThreshold) {
            rehash(tableStates.length);
        }
    }

    private int allocateSlot() {
        if (freeHead != NO_SOURCE) {
            int slot = freeHead;
            freeHead = nextFree[slot];
            nextFree[slot] = NO_SOURCE;
            return slot;
        }
        ensureStorageCapacity(highWaterMark + 1);
        return highWaterMark++;
    }

    private void clearSource(int slot) {
        occupied[slot] = 0;
        sourceIds[slot] = 0L;
        lifecycleGenerations[slot] = 0;
        sourceModes[slot] = 0;
        enabled[slot] = 0;
        portCounts[slot] = 0;
        declaredPowerW[slot] = 0.0D;
        lastEventTicks[slot] = 0L;
        nextFree[slot] = freeHead;
        freeHead = slot;
        liveSourceCount--;
    }

    private void ensureStorageForAdditional(int additional) {
        if (additional <= 0) {
            return;
        }
        int availableFree = highWaterMark - liveSourceCount;
        int extension = Math.max(0, additional - availableFree);
        int required = Math.addExact(highWaterMark, extension);
        if (Math.addExact(liveSourceCount, additional) > maximumSources) {
            throw new IllegalStateException("thermal source limit reached");
        }
        ensureStorageCapacity(required);
    }

    private void allocateStorage(int capacity) {
        sourceIds = new long[capacity];
        lifecycleGenerations = new int[capacity];
        sourceModes = new byte[capacity];
        enabled = new byte[capacity];
        occupied = new byte[capacity];
        portCounts = new int[capacity];
        declaredPowerW = new double[capacity];
        lastEventTicks = new long[capacity];
        nextFree = new int[capacity];
        Arrays.fill(nextFree, NO_SOURCE);

        int ports = Math.multiplyExact(capacity, maxPortsPerSource);
        portIds = new int[ports];
        portPowerShares = new double[ports];
        portContributionW = new double[ports];
        portBindingKinds = new byte[ports];
        portTargetIds = new long[ports];
        portBindingGenerations = new int[ports];
    }

    private void ensureStorageCapacity(int required) {
        if (required <= sourceIds.length) {
            return;
        }
        int old = sourceIds.length;
        if (required > maximumSources) {
            throw new IllegalStateException("thermal source limit reached");
        }
        int capacity = Math.min(
                maximumSources,
                Math.max(required, old + Math.max(8, old >>> 1)));
        sourceIds = Arrays.copyOf(sourceIds, capacity);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, capacity);
        sourceModes = Arrays.copyOf(sourceModes, capacity);
        enabled = Arrays.copyOf(enabled, capacity);
        occupied = Arrays.copyOf(occupied, capacity);
        portCounts = Arrays.copyOf(portCounts, capacity);
        declaredPowerW = Arrays.copyOf(declaredPowerW, capacity);
        lastEventTicks = Arrays.copyOf(lastEventTicks, capacity);
        nextFree = Arrays.copyOf(nextFree, capacity);
        Arrays.fill(nextFree, old, capacity, NO_SOURCE);

        int portCapacity = Math.multiplyExact(capacity, maxPortsPerSource);
        portIds = Arrays.copyOf(portIds, portCapacity);
        portPowerShares = Arrays.copyOf(portPowerShares, portCapacity);
        portContributionW = Arrays.copyOf(portContributionW, portCapacity);
        portBindingKinds = Arrays.copyOf(portBindingKinds, portCapacity);
        portTargetIds = Arrays.copyOf(portTargetIds, portCapacity);
        portBindingGenerations = Arrays.copyOf(
                portBindingGenerations, portCapacity);
    }

    private void allocateTable(int capacity) {
        tableSourceIds = new long[capacity];
        tableSlots = new int[capacity];
        tableStates = new byte[capacity];
        tableUsed = 0;
        tableResizeThreshold = Math.max(1, (int) (capacity * 0.6D));
    }

    private void rehash(int capacity) {
        long[] oldIds = tableSourceIds;
        int[] oldSlots = tableSlots;
        byte[] oldStates = tableStates;
        allocateTable(capacity);
        for (int index = 0; index < oldStates.length; index++) {
            if (oldStates[index] == OCCUPIED) {
                insertTable(oldIds[index], oldSlots[index]);
            }
        }
    }

    private int portBase(int sourceSlot) {
        return Math.multiplyExact(sourceSlot, maxPortsPerSource);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("source ledger is closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (int slot = 0; slot < highWaterMark; slot++) {
            if (occupied[slot] == 0) {
                continue;
            }
            int firstPort = portBase(slot);
            for (int index = 0; index < portCounts[slot]; index++) {
                int portOffset = firstPort + index;
                if (isThermalNode(portOffset)) {
                    double contribution = portContributionW[portOffset];
                    if (contribution != 0.0D) {
                        changeNodePower(
                                portTargetIds[portOffset],
                                portBindingGenerations[portOffset],
                                cursorTick,
                                -contribution);
                    }
                    accumulators.releaseBinding(
                            portTargetIds[portOffset],
                            portBindingGenerations[portOffset],
                            cursorTick);
                }
            }
        }
    }

    private static int tableCapacity(int expected) {
        int required = Math.max(4, (int) Math.ceil(expected / 0.6D));
        int highest = Integer.highestOneBit(required - 1);
        if (highest >= 1 << 29) {
            throw new IllegalArgumentException("source table is too large");
        }
        return highest << 1;
    }

    private static int hash(long sourceId) {
        long mixed = sourceId;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (int) mixed;
    }

    private static double finiteProduct(String name, double left, double right) {
        double result = left * right;
        requireFiniteResult(name, result);
        return result;
    }

    private static double finiteSum(String name, double left, double right) {
        double result = left + right;
        requireFiniteResult(name, result);
        return canonicalZero(result);
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFiniteResult(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " exceeded the finite source domain");
        }
    }

    private static double canonicalZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }

    /** Concrete worker binding boundary invoked after each exact source event. */
    public interface EventObserver {
        void afterEvent(
                ThermalSourceBatch batch,
                int eventIndex,
                ThermalSourceLedger ledger
        );
    }
}
