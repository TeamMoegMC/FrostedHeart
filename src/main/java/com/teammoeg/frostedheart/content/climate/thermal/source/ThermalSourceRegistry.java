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

import java.util.Arrays;
import java.util.Objects;

/** Single-owner packed source state and exact event-time energy ledger. */
public final class ThermalSourceRegistry {
    public static final int NO_SOURCE = -1;

    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double SHARE_TOLERANCE = 1.0e-12D;
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final ThermalSourceMode[] SOURCE_MODES = ThermalSourceMode.values();
    private static final SourceBinding.Kind[] BINDING_KINDS = SourceBinding.Kind.values();
    private static final int ROUTE_KIND_COUNT = BINDING_KINDS.length;

    private final int maxPortsPerSource;
    private final NodePowerAccumulatorArena accumulators;

    private long[] sourceIds;
    private int[] lifecycleGenerations;
    private byte[] sourceModes;
    private byte[] enabled;
    private byte[] unloaded;
    private int[] portCounts;
    private double[] declaredPowerW;
    private long[] lastLedgerTicks;
    private double[] routedEnergyJ;

    private int[] portIds;
    private double[] portPowerShares;
    private double[] portContributionW;
    private byte[] portBindingKinds;
    private long[] portTargetIds;
    private int[] portBindingGenerations;

    private long[] tableSourceIds;
    private int[] tableSlots;
    private byte[] tableStates;
    private int tableResizeThreshold;
    private int sourceCount;

    public ThermalSourceRegistry(
            int initialSourceCapacity,
            int maxPortsPerSource,
            NodePowerAccumulatorArena accumulators
    ) {
        if (initialSourceCapacity < 0) {
            throw new IllegalArgumentException("initialSourceCapacity must be non-negative");
        }
        if (maxPortsPerSource <= 0) {
            throw new IllegalArgumentException("maxPortsPerSource must be positive");
        }
        this.maxPortsPerSource = maxPortsPerSource;
        this.accumulators = Objects.requireNonNull(accumulators, "accumulators");
        allocateStorage(Math.max(1, initialSourceCapacity));
        allocateTable(tableCapacityFor(Math.max(1, initialSourceCapacity)));
    }

    public int sourceCount() {
        return sourceCount;
    }

    boolean referencesThermalNodeRange(long firstNodeId, long endNodeIdExclusive) {
        if (firstNodeId < 0L || endNodeIdExclusive < firstNodeId) {
            throw new IllegalArgumentException("thermal node range is invalid");
        }
        for (int sourceSlot = 0; sourceSlot < sourceCount; sourceSlot++) {
            int firstPort = portBase(sourceSlot);
            for (int index = 0; index < portCounts[sourceSlot]; index++) {
                SourceBinding binding = readBinding(firstPort + index);
                if (binding.isThermalNode()
                        && binding.targetId() >= firstNodeId
                        && binding.targetId() < endNodeIdExclusive) {
                    return true;
                }
            }
        }
        return false;
    }

    NodePowerAccumulatorArena accumulators() {
        return accumulators;
    }

    void registerSource(
            long sourceId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean isEnabled,
            long effectiveTick,
            EmissionPort[] ports
    ) {
        Objects.requireNonNull(mode, "mode");
        requireFinite("powerW", powerW);
        if (mode == ThermalSourceMode.IMPULSE && powerW != 0.0D) {
            throw new IllegalArgumentException("IMPULSE sources cannot hold continuous power");
        }
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("lifecycleGeneration must be non-negative");
        }
        requireTick("effectiveTick", effectiveTick);
        EmissionPort[] validatedPorts = validatePorts(ports, isEnabled, sourceId);
        int existing = findSource(sourceId);
        if (existing != NO_SOURCE) {
            if (unloaded[existing] == 0) {
                throw new IllegalArgumentException("sourceId is already registered: " + sourceId);
            }
            reviveSource(
                    existing,
                    lifecycleGeneration,
                    mode,
                    powerW,
                    isEnabled,
                    effectiveTick,
                    validatedPorts);
            return;
        }
        if (sourceCount >= tableResizeThreshold) {
            rehash(tableStates.length << 1);
        }
        ensureSourceStorage(sourceCount + 1);
        int slot = sourceCount++;
        sourceIds[slot] = sourceId;
        lifecycleGenerations[slot] = lifecycleGeneration;
        sourceModes[slot] = (byte) mode.ordinal();
        enabled[slot] = isEnabled ? (byte) 1 : 0;
        portCounts[slot] = validatedPorts.length;
        declaredPowerW[slot] = canonicalZero(powerW);
        lastLedgerTicks[slot] = effectiveTick;

        int firstPort = portBase(slot);
        for (int index = 0; index < validatedPorts.length; index++) {
            writePort(firstPort + index, validatedPorts[index]);
        }
        installEffectivePower(slot, effectiveTick, effectivePower(slot));
        insertTable(sourceId, slot);
    }

    private void reviveSource(
            int slot,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean isEnabled,
            long effectiveTick,
            EmissionPort[] ports
    ) {
        lifecycleGenerations[slot] = lifecycleGeneration;
        sourceModes[slot] = (byte) mode.ordinal();
        enabled[slot] = isEnabled ? (byte) 1 : 0;
        unloaded[slot] = 0;
        portCounts[slot] = ports.length;
        declaredPowerW[slot] = canonicalZero(powerW);
        lastLedgerTicks[slot] = effectiveTick;

        int firstPort = portBase(slot);
        int endPort = firstPort + maxPortsPerSource;
        Arrays.fill(portIds, firstPort, endPort, 0);
        Arrays.fill(portPowerShares, firstPort, endPort, 0.0D);
        Arrays.fill(portContributionW, firstPort, endPort, 0.0D);
        Arrays.fill(portBindingKinds, firstPort, endPort, (byte) 0);
        Arrays.fill(portTargetIds, firstPort, endPort, 0L);
        Arrays.fill(portBindingGenerations, firstPort, endPort, 0);
        for (int index = 0; index < ports.length; index++) {
            writePort(firstPort + index, ports[index]);
        }
        int firstRoute = routeOffset(slot, BINDING_KINDS[0]);
        Arrays.fill(routedEnergyJ, firstRoute, firstRoute + ROUTE_KIND_COUNT, 0.0D);
        installEffectivePower(slot, effectiveTick, effectivePower(slot));
    }

    public int findSource(long sourceId) {
        int mask = tableStates.length - 1;
        int index = hash(sourceId) & mask;
        while (tableStates[index] != EMPTY) {
            if (tableSourceIds[index] == sourceId) {
                return tableSlots[index];
            }
            index = (index + 1) & mask;
        }
        return NO_SOURCE;
    }

    void setPower(long sourceId, double newPowerW, long effectiveTick) {
        requireFinite("newPowerW", newPowerW);
        int slot = requireMutableSource(sourceId);
        if (mode(slot) != ThermalSourceMode.POWER_SOURCE) {
            throw new IllegalStateException("only POWER_SOURCE can hold continuous power");
        }
        beginEvent(slot, effectiveTick);
        declaredPowerW[slot] = canonicalZero(newPowerW);
        installEffectivePower(slot, effectiveTick, effectivePower(slot));
    }

    void setEnabled(long sourceId, boolean isEnabled, long effectiveTick) {
        int slot = requireMutableSource(sourceId);
        beginEvent(slot, effectiveTick);
        enabled[slot] = isEnabled ? (byte) 1 : 0;
        installEffectivePower(slot, effectiveTick, effectivePower(slot));
    }

    void rebindPort(
            long sourceId,
            int portId,
            SourceBinding requestedBinding,
            long effectiveTick
    ) {
        Objects.requireNonNull(requestedBinding, "requestedBinding");
        int slot = requireMutableSource(sourceId);
        int portOffset = requirePortOffset(slot, portId);
        beginEvent(slot, effectiveTick);
        SourceBinding newBinding = requestedBinding;
        if (enabled[slot] != 0 && requestedBinding.kind() == SourceBinding.Kind.UNBOUND) {
            newBinding = SourceBinding.degradedLoss(sourceId);
        }
        movePortContribution(portOffset, newBinding, effectiveTick);
        writeBinding(portOffset, newBinding);
    }

    void applyImpulse(
            long sourceId,
            int portId,
            double signedEnergyJ,
            long effectiveTick
    ) {
        requireFinite("signedEnergyJ", signedEnergyJ);
        int slot = requireMutableSource(sourceId);
        if (mode(slot) != ThermalSourceMode.IMPULSE) {
            throw new IllegalStateException("only IMPULSE sources accept impulse events");
        }
        int portOffset = requirePortOffset(slot, portId);
        beginEvent(slot, effectiveTick);
        SourceBinding binding = effectiveBinding(slot, portOffset);
        routeEnergy(slot, binding.kind(), signedEnergyJ);
        if (binding.isThermalNode()) {
            int accumulator = accumulators.ensureNode(
                    binding.targetId(), binding.lifecycleGeneration(), effectiveTick);
            accumulators.addImpulseAt(accumulator, effectiveTick, signedEnergyJ);
        }
    }

    UnloadStatus unloadSource(
            long sourceId,
            int expectedLifecycleGeneration,
            long effectiveTick
    ) {
        int slot = findSource(sourceId);
        if (slot == NO_SOURCE) {
            return UnloadStatus.NOT_FOUND;
        }
        if (lifecycleGenerations[slot] != expectedLifecycleGeneration) {
            return UnloadStatus.STALE_GENERATION;
        }
        if (unloaded[slot] != 0) {
            return UnloadStatus.ALREADY_UNLOADED;
        }
        beginEvent(slot, effectiveTick);
        int firstPort = portBase(slot);
        for (int index = 0; index < portCounts[slot]; index++) {
            int portOffset = firstPort + index;
            SourceBinding oldBinding = readBinding(portOffset);
            if (oldBinding.isThermalNode() && portContributionW[portOffset] != 0.0D) {
                changeNodePower(oldBinding, effectiveTick, -portContributionW[portOffset]);
            }
            portContributionW[portOffset] = 0.0D;
            writeBinding(portOffset, SourceBinding.unbound());
        }
        enabled[slot] = 0;
        unloaded[slot] = 1;
        return UnloadStatus.APPLIED;
    }

    public double routedEnergyJ(long sourceId, SourceBinding.Kind bindingKind) {
        Objects.requireNonNull(bindingKind, "bindingKind");
        return routedEnergyJ[routeOffset(requireSource(sourceId), bindingKind)];
    }

    double routedEnergyJAt(
            long sourceId,
            SourceBinding.Kind bindingKind,
            long targetTick
    ) {
        int slot = requireSource(sourceId);
        beginEvent(slot, targetTick);
        return routedEnergyJ[routeOffset(slot, Objects.requireNonNull(bindingKind, "bindingKind"))];
    }

    private void beginEvent(int sourceSlot, long effectiveTick) {
        requireTick("effectiveTick", effectiveTick);
        if (effectiveTick < lastLedgerTicks[sourceSlot]) {
            throw new IllegalArgumentException(
                    "effectiveTick precedes source ledger tick: "
                            + effectiveTick + " < " + lastLedgerTicks[sourceSlot]);
        }
        settleLedger(sourceSlot, effectiveTick);
    }

    private void settleLedger(int sourceSlot, long targetTick) {
        long previousTick = lastLedgerTicks[sourceSlot];
        if (targetTick == previousTick) {
            return;
        }
        double elapsedSeconds = (targetTick - previousTick) / TICKS_PER_SECOND;
        double totalEnergy = finiteProduct(
                "source interval energy", effectivePower(sourceSlot), elapsedSeconds);
        if (totalEnergy != 0.0D) {
            int firstPort = portBase(sourceSlot);
            double assigned = 0.0D;
            for (int index = 0; index < portCounts[sourceSlot]; index++) {
                int portOffset = firstPort + index;
                double portEnergy = index == portCounts[sourceSlot] - 1
                        ? totalEnergy - assigned
                        : finiteProduct(
                                "source port interval energy",
                                portContributionW[portOffset],
                                elapsedSeconds);
                if (index != portCounts[sourceSlot] - 1) {
                    assigned = finiteSum("assigned source energy", assigned, portEnergy);
                }
                if (portEnergy != 0.0D) {
                    SourceBinding binding = effectiveBinding(sourceSlot, portOffset);
                    routeEnergy(sourceSlot, binding.kind(), portEnergy);
                }
            }
        }
        lastLedgerTicks[sourceSlot] = targetTick;
    }

    private SourceBinding effectiveBinding(int sourceSlot, int portOffset) {
        SourceBinding binding = readBinding(portOffset);
        return binding.kind() == SourceBinding.Kind.UNBOUND
                ? SourceBinding.degradedLoss(sourceIds[sourceSlot])
                : binding;
    }

    private void installEffectivePower(int sourceSlot, long eventTick, double totalPowerW) {
        int firstPort = portBase(sourceSlot);
        double assigned = 0.0D;
        for (int index = 0; index < portCounts[sourceSlot]; index++) {
            int portOffset = firstPort + index;
            double contribution = index == portCounts[sourceSlot] - 1
                    ? totalPowerW - assigned
                    : finiteProduct(
                            "source port power", totalPowerW, portPowerShares[portOffset]);
            if (index != portCounts[sourceSlot] - 1) {
                assigned = finiteSum("assigned source power", assigned, contribution);
            }
            double delta = contribution - portContributionW[portOffset];
            SourceBinding binding = readBinding(portOffset);
            if (binding.isThermalNode() && delta != 0.0D) {
                changeNodePower(binding, eventTick, delta);
            }
            portContributionW[portOffset] = canonicalZero(contribution);
        }
    }

    private void movePortContribution(
            int portOffset,
            SourceBinding newBinding,
            long eventTick
    ) {
        double contribution = portContributionW[portOffset];
        SourceBinding oldBinding = readBinding(portOffset);
        if (contribution != 0.0D && oldBinding.isThermalNode()) {
            changeNodePower(oldBinding, eventTick, -contribution);
        }
        if (contribution != 0.0D && newBinding.isThermalNode()) {
            changeNodePower(newBinding, eventTick, contribution);
        }
    }

    private void changeNodePower(SourceBinding binding, long eventTick, double deltaPowerW) {
        int accumulator = accumulators.ensureNode(
                binding.targetId(), binding.lifecycleGeneration(), eventTick);
        accumulators.changePowerAt(accumulator, eventTick, deltaPowerW);
    }

    private void routeEnergy(
            int sourceSlot,
            SourceBinding.Kind bindingKind,
            double energyJ
    ) {
        int offset = routeOffset(sourceSlot, bindingKind);
        routedEnergyJ[offset] = finiteSum(
                "routed source energy", routedEnergyJ[offset], energyJ);
    }

    private double effectivePower(int sourceSlot) {
        return enabled[sourceSlot] != 0 && mode(sourceSlot) == ThermalSourceMode.POWER_SOURCE
                ? declaredPowerW[sourceSlot]
                : 0.0D;
    }

    private ThermalSourceMode mode(int sourceSlot) {
        return SOURCE_MODES[sourceModes[sourceSlot]];
    }

    private void writePort(int portOffset, EmissionPort port) {
        portIds[portOffset] = port.portId();
        portPowerShares[portOffset] = port.powerShare();
        writeBinding(portOffset, port.binding());
    }

    private void writeBinding(int portOffset, SourceBinding binding) {
        portBindingKinds[portOffset] = (byte) binding.kind().ordinal();
        portTargetIds[portOffset] = binding.targetId();
        portBindingGenerations[portOffset] = binding.lifecycleGeneration();
    }

    private SourceBinding readBinding(int portOffset) {
        return new SourceBinding(
                BINDING_KINDS[portBindingKinds[portOffset]],
                portTargetIds[portOffset],
                portBindingGenerations[portOffset]);
    }

    private int requireSource(long sourceId) {
        int slot = findSource(sourceId);
        if (slot == NO_SOURCE) {
            throw new IllegalArgumentException("unknown sourceId: " + sourceId);
        }
        return slot;
    }

    private int requireMutableSource(long sourceId) {
        int slot = requireSource(sourceId);
        if (unloaded[slot] != 0) {
            throw new IllegalStateException("source has been unloaded: " + sourceId);
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

    private EmissionPort[] validatePorts(
            EmissionPort[] ports,
            boolean isEnabled,
            long sourceId
    ) {
        Objects.requireNonNull(ports, "ports");
        if (ports.length == 0 || ports.length > maxPortsPerSource) {
            throw new IllegalArgumentException(
                    "ports must contain 1.." + maxPortsPerSource + " entries");
        }
        EmissionPort[] clone = ports.clone();
        double share = 0.0D;
        for (int index = 0; index < clone.length; index++) {
            EmissionPort port = Objects.requireNonNull(clone[index], "ports contains null");
            if (isEnabled && port.binding().kind() == SourceBinding.Kind.UNBOUND) {
                clone[index] = new EmissionPort(
                        port.portId(),
                        port.powerShare(),
                        SourceBinding.degradedLoss(sourceId));
            }
            share = finiteSum("source port shares", share, port.powerShare());
            for (int earlier = 0; earlier < index; earlier++) {
                if (clone[earlier].portId() == port.portId()) {
                    throw new IllegalArgumentException("duplicate source portId: " + port.portId());
                }
            }
        }
        if (Math.abs(share - 1.0D) > SHARE_TOLERANCE) {
            throw new IllegalArgumentException("source port power shares must sum to one");
        }
        return clone;
    }

    private void ensureSourceStorage(int requiredCapacity) {
        if (requiredCapacity <= sourceIds.length) {
            return;
        }
        resizeStorage(Math.max(requiredCapacity, sourceIds.length << 1));
    }

    private void allocateStorage(int capacity) {
        sourceIds = new long[capacity];
        lifecycleGenerations = new int[capacity];
        sourceModes = new byte[capacity];
        enabled = new byte[capacity];
        unloaded = new byte[capacity];
        portCounts = new int[capacity];
        declaredPowerW = new double[capacity];
        lastLedgerTicks = new long[capacity];
        routedEnergyJ = new double[Math.multiplyExact(capacity, ROUTE_KIND_COUNT)];

        int portCapacity = Math.multiplyExact(capacity, maxPortsPerSource);
        portIds = new int[portCapacity];
        portPowerShares = new double[portCapacity];
        portContributionW = new double[portCapacity];
        portBindingKinds = new byte[portCapacity];
        portTargetIds = new long[portCapacity];
        portBindingGenerations = new int[portCapacity];
    }

    private void resizeStorage(int newCapacity) {
        sourceIds = Arrays.copyOf(sourceIds, newCapacity);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, newCapacity);
        sourceModes = Arrays.copyOf(sourceModes, newCapacity);
        enabled = Arrays.copyOf(enabled, newCapacity);
        unloaded = Arrays.copyOf(unloaded, newCapacity);
        portCounts = Arrays.copyOf(portCounts, newCapacity);
        declaredPowerW = Arrays.copyOf(declaredPowerW, newCapacity);
        lastLedgerTicks = Arrays.copyOf(lastLedgerTicks, newCapacity);
        routedEnergyJ = Arrays.copyOf(
                routedEnergyJ, Math.multiplyExact(newCapacity, ROUTE_KIND_COUNT));

        int newPortCapacity = Math.multiplyExact(newCapacity, maxPortsPerSource);
        portIds = Arrays.copyOf(portIds, newPortCapacity);
        portPowerShares = Arrays.copyOf(portPowerShares, newPortCapacity);
        portContributionW = Arrays.copyOf(portContributionW, newPortCapacity);
        portBindingKinds = Arrays.copyOf(portBindingKinds, newPortCapacity);
        portTargetIds = Arrays.copyOf(portTargetIds, newPortCapacity);
        portBindingGenerations = Arrays.copyOf(portBindingGenerations, newPortCapacity);
    }

    private void allocateTable(int capacity) {
        tableSourceIds = new long[capacity];
        tableSlots = new int[capacity];
        tableStates = new byte[capacity];
        tableResizeThreshold = Math.max(1, (int) (capacity * 0.6D));
    }

    private void rehash(int requestedCapacity) {
        long[] oldIds = tableSourceIds;
        int[] oldSlots = tableSlots;
        byte[] oldStates = tableStates;
        allocateTable(nextPowerOfTwo(requestedCapacity));
        for (int index = 0; index < oldStates.length; index++) {
            if (oldStates[index] == OCCUPIED) {
                insertTable(oldIds[index], oldSlots[index]);
            }
        }
    }

    private void insertTable(long sourceId, int sourceSlot) {
        int mask = tableStates.length - 1;
        int index = hash(sourceId) & mask;
        while (tableStates[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }
        tableStates[index] = OCCUPIED;
        tableSourceIds[index] = sourceId;
        tableSlots[index] = sourceSlot;
    }

    private int portBase(int sourceSlot) {
        return sourceSlot * maxPortsPerSource;
    }

    private static int routeOffset(int sourceSlot, SourceBinding.Kind kind) {
        return sourceSlot * ROUTE_KIND_COUNT + kind.ordinal();
    }

    private static int tableCapacityFor(int expectedEntries) {
        return nextPowerOfTwo(Math.max(4, (int) Math.ceil(expectedEntries / 0.6D)));
    }

    private static int nextPowerOfTwo(int value) {
        int highest = Integer.highestOneBit(Math.max(2, value - 1));
        if (highest >= 1 << 29) {
            throw new IllegalArgumentException("source registry table is too large");
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

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireTick(String name, long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
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

    private static void requireFiniteResult(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " exceeded the finite source domain");
        }
    }

    private static double canonicalZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }

    enum UnloadStatus {
        APPLIED,
        NOT_FOUND,
        STALE_GENERATION,
        ALREADY_UNLOADED
    }
}
