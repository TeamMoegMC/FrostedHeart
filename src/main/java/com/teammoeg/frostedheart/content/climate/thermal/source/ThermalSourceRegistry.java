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

/**
 * Single-owner packed physical-source identity and event-time energy ledgers.
 *
 * <p>Sources and ports are primitive structure-of-arrays storage. Per-source
 * history is a fixed-size primitive ring and is only materialized as objects
 * when a resync snapshot is explicitly requested. Stable solve work reads the
 * separate {@link NodePowerAccumulatorArena}, never this registry. Runtime
 * mutation is owned by {@link ThermalSourceTimeline}; direct calls remain for
 * focused ledger tests and cold recovery construction.</p>
 */
public final class ThermalSourceRegistry {
    public static final int NO_SOURCE = -1;

    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double SHARE_TOLERANCE = 1.0e-12D;
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;

    private final int maxPortsPerSource;
    private final int historyCapacityPerSource;
    private final NodePowerAccumulatorArena accumulators;

    private long[] sourceIds;
    private long[] packedPositions;
    private int[] profileIds;
    private int[] lifecycleGenerations;
    private int[] sourceRevisions;
    private byte[] sourceModes;
    private byte[] enabled;
    private byte[] unloaded;
    private int[] portCounts;
    private double[] declaredPowerW;
    private long[] lastLedgerTicks;
    private long[] eventWatermarks;
    private double[] cumulativeEnergyJ;
    private double[] cumulativeCompensationJ;
    private long[] ackWatermarks;
    private long[] ackTicks;
    private double[] ackCumulativeEnergyJ;
    private int[] historyHeads;
    private int[] historySizes;
    private long[] droppedFirstWatermarks;
    private long[] droppedLastWatermarks;
    private long[] droppedStartTicks;
    private long[] droppedEndTicks;
    private int[] droppedSegmentCounts;
    private double[] routedEnergyJ;

    private int[] portIds;
    private int[] portRevisions;
    private byte[] portChannels;
    private double[] portPowerShares;
    private double[] portContributionW;
    private byte[] portBindingKinds;
    private long[] portTargetIds;
    private int[] portBindingGenerations;

    private long[] historyWatermarks;
    private long[] historyStartTicks;
    private long[] historyEndTicks;
    private int[] historyPortIds;
    private int[] historyPortRevisions;
    private byte[] historyChannels;
    private byte[] historyBindingKinds;
    private long[] historyTargetIds;
    private int[] historyBindingGenerations;
    private double[] historyEnergyJ;

    private long[] tableSourceIds;
    private int[] tableSlots;
    private byte[] tableStates;
    private int tableResizeThreshold;

    private int sourceCount;
    private long nextEventWatermark;

    public ThermalSourceRegistry(
            int initialSourceCapacity,
            int maxPortsPerSource,
            int historyCapacityPerSource,
            NodePowerAccumulatorArena accumulators
    ) {
        if (initialSourceCapacity < 0) {
            throw new IllegalArgumentException("initialSourceCapacity must be non-negative");
        }
        if (maxPortsPerSource <= 0) {
            throw new IllegalArgumentException("maxPortsPerSource must be positive");
        }
        if (historyCapacityPerSource <= 0) {
            throw new IllegalArgumentException("historyCapacityPerSource must be positive");
        }
        this.maxPortsPerSource = maxPortsPerSource;
        this.historyCapacityPerSource = historyCapacityPerSource;
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

    public long latestEventWatermark() {
        return nextEventWatermark;
    }

    public int maxPortsPerSource() {
        return maxPortsPerSource;
    }

    public int historyCapacityPerSource() {
        return historyCapacityPerSource;
    }

    NodePowerAccumulatorArena accumulators() {
        return accumulators;
    }

    void registerSource(
            long sourceId,
            long packedPosition,
            int profileId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean isEnabled,
            long effectiveTick,
            EmissionPort[] ports
    ) {
        Objects.requireNonNull(mode, "mode");
        if (!mode.usesPhysicalEnergyLedger()) {
            throw new IllegalArgumentException(
                    "non-physical mode does not belong in ThermalSourceRegistry: " + mode);
        }
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
                    packedPosition,
                    profileId,
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
        packedPositions[slot] = packedPosition;
        profileIds[slot] = profileId;
        lifecycleGenerations[slot] = lifecycleGeneration;
        sourceRevisions[slot] = 1;
        sourceModes[slot] = (byte) mode.ordinal();
        enabled[slot] = isEnabled ? (byte) 1 : 0;
        portCounts[slot] = validatedPorts.length;
        declaredPowerW[slot] = canonicalZero(powerW);
        lastLedgerTicks[slot] = effectiveTick;
        ackTicks[slot] = effectiveTick;
        droppedFirstWatermarks[slot] = Long.MAX_VALUE;
        droppedStartTicks[slot] = Long.MAX_VALUE;

        int firstPort = portBase(slot);
        for (int index = 0; index < validatedPorts.length; index++) {
            writePort(firstPort + index, validatedPorts[index]);
        }
        long watermark = nextWatermark();
        eventWatermarks[slot] = watermark;
        installEffectivePower(slot, effectiveTick, effectivePower(slot));
        insertTable(sourceId, slot);
    }

    private void reviveSource(
            int slot,
            long packedPosition,
            int profileId,
            int lifecycleGeneration,
            ThermalSourceMode mode,
            double powerW,
            boolean isEnabled,
            long effectiveTick,
            EmissionPort[] ports
    ) {
        packedPositions[slot] = packedPosition;
        profileIds[slot] = profileId;
        lifecycleGenerations[slot] = lifecycleGeneration;
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
        sourceModes[slot] = (byte) mode.ordinal();
        enabled[slot] = isEnabled ? (byte) 1 : 0;
        unloaded[slot] = 0;
        portCounts[slot] = ports.length;
        declaredPowerW[slot] = canonicalZero(powerW);
        lastLedgerTicks[slot] = effectiveTick;
        eventWatermarks[slot] = nextWatermark();
        cumulativeEnergyJ[slot] = 0.0D;
        cumulativeCompensationJ[slot] = 0.0D;
        ackWatermarks[slot] = 0L;
        ackTicks[slot] = effectiveTick;
        ackCumulativeEnergyJ[slot] = 0.0D;
        historyHeads[slot] = 0;
        historySizes[slot] = 0;
        clearDroppedHistory(slot);

        int firstPort = portBase(slot);
        Arrays.fill(portIds, firstPort, firstPort + maxPortsPerSource, 0);
        Arrays.fill(portRevisions, firstPort, firstPort + maxPortsPerSource, 0);
        Arrays.fill(portChannels, firstPort, firstPort + maxPortsPerSource, (byte) 0);
        Arrays.fill(portPowerShares, firstPort, firstPort + maxPortsPerSource, 0.0D);
        Arrays.fill(portContributionW, firstPort, firstPort + maxPortsPerSource, 0.0D);
        Arrays.fill(portBindingKinds, firstPort, firstPort + maxPortsPerSource, (byte) 0);
        Arrays.fill(portTargetIds, firstPort, firstPort + maxPortsPerSource, 0L);
        Arrays.fill(portBindingGenerations, firstPort, firstPort + maxPortsPerSource, 0);
        for (int index = 0; index < ports.length; index++) {
            writePort(firstPort + index, ports[index]);
        }

        int firstHistory = historyOffset(slot, 0);
        int endHistory = firstHistory + historyCapacityPerSource;
        Arrays.fill(historyWatermarks, firstHistory, endHistory, 0L);
        Arrays.fill(historyStartTicks, firstHistory, endHistory, 0L);
        Arrays.fill(historyEndTicks, firstHistory, endHistory, 0L);
        Arrays.fill(historyPortIds, firstHistory, endHistory, 0);
        Arrays.fill(historyPortRevisions, firstHistory, endHistory, 0);
        Arrays.fill(historyChannels, firstHistory, endHistory, (byte) 0);
        Arrays.fill(historyBindingKinds, firstHistory, endHistory, (byte) 0);
        Arrays.fill(historyTargetIds, firstHistory, endHistory, 0L);
        Arrays.fill(historyBindingGenerations, firstHistory, endHistory, 0);
        Arrays.fill(historyEnergyJ, firstHistory, endHistory, 0.0D);
        int firstRoute = routeOffset(slot, SourceBinding.Kind.values()[0]);
        Arrays.fill(
                routedEnergyJ,
                firstRoute,
                firstRoute + SourceBinding.Kind.values().length,
                0.0D);
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

    public ThermalSourceEntry entry(long sourceId) {
        int slot = requireSource(sourceId);
        return new ThermalSourceEntry(
                sourceIds[slot],
                packedPositions[slot],
                profileIds[slot],
                lifecycleGenerations[slot],
                sourceRevisions[slot],
                eventWatermarks[slot],
                mode(slot),
                declaredPowerW[slot],
                enabled[slot] != 0,
                unloaded[slot] != 0,
                lastLedgerTicks[slot],
                cumulativeEnergyJ[slot],
                portCounts[slot]
        );
    }

    public EmissionPort port(long sourceId, int portId) {
        int sourceSlot = requireSource(sourceId);
        return readPort(requirePortOffset(sourceSlot, portId));
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
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
    }

    void setEnabled(long sourceId, boolean isEnabled, long effectiveTick) {
        int slot = requireMutableSource(sourceId);
        beginEvent(slot, effectiveTick);
        enabled[slot] = isEnabled ? (byte) 1 : 0;
        installEffectivePower(slot, effectiveTick, effectivePower(slot));
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
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
        portRevisions[portOffset] = Math.incrementExact(portRevisions[portOffset]);
        writeBinding(portOffset, newBinding);
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
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
        SourceBinding binding = readBinding(portOffset);
        if (binding.kind() == SourceBinding.Kind.UNBOUND) {
            binding = SourceBinding.degradedLoss(sourceId);
        }
        appendSegment(
                slot,
                eventWatermarks[slot],
                effectiveTick,
                effectiveTick,
                portOffset,
                binding,
                signedEnergyJ
        );
        addCumulative(slot, signedEnergyJ);
        routeEnergy(slot, binding.kind(), signedEnergyJ);
        if (binding.isThermalNode()) {
            int accumulator = accumulators.ensureNode(
                    binding.targetId(), binding.lifecycleGeneration(), effectiveTick);
            accumulators.addImpulseAt(accumulator, effectiveTick, signedEnergyJ);
        }
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
    }

    /**
     * Settles all old node bindings, then routes future cold-source power to an
     * explicit finite sink instead of accumulating debt for later admission.
     */
    void routeColdSourceTo(
            long sourceId,
            SourceBinding explicitSink,
            long effectiveTick
    ) {
        Objects.requireNonNull(explicitSink, "explicitSink");
        if (!explicitSink.isExplicitSink()) {
            throw new IllegalArgumentException("cold source requires an explicit sink binding");
        }
        int slot = requireMutableSource(sourceId);
        beginEvent(slot, effectiveTick);
        int firstPort = portBase(slot);
        for (int index = 0; index < portCounts[slot]; index++) {
            int portOffset = firstPort + index;
            movePortContribution(portOffset, explicitSink, effectiveTick);
            portRevisions[portOffset] = Math.incrementExact(portRevisions[portOffset]);
            writeBinding(portOffset, explicitSink);
        }
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
    }

    /** Settles to the unload tick before removing every live contribution. */
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
            portRevisions[portOffset] = Math.incrementExact(portRevisions[portOffset]);
            writeBinding(portOffset, SourceBinding.unbound());
        }
        enabled[slot] = 0;
        unloaded[slot] = 1;
        sourceRevisions[slot] = Math.incrementExact(sourceRevisions[slot]);
        return UnloadStatus.APPLIED;
    }

    /** Materializes the bounded replay history from the last acknowledged point. */
    public SourceResyncSnapshot snapshotAt(long sourceId, long snapshotTick) {
        int slot = requireSource(sourceId);
        if (snapshotTick > lastLedgerTicks[slot]) {
            beginEvent(slot, snapshotTick);
        } else if (snapshotTick < lastLedgerTicks[slot]) {
            throw new IllegalArgumentException("snapshotTick precedes source ledger tick");
        }

        SourceResyncSnapshot.BindingEnergySegment[] retained = retainedSegments(slot);
        double retainedEnergy = compensatedSegmentSum(retained);
        double missingEnergy = finiteDifference(
                "source recovery checksum",
                cumulativeEnergyJ[slot] - ackCumulativeEnergyJ[slot],
                retainedEnergy
        );
        SourceResyncSnapshot.SourceResyncLoss[] losses;
        if (droppedLastWatermarks[slot] > ackWatermarks[slot]) {
            long lossStart = droppedStartTicks[slot] == Long.MAX_VALUE
                    ? ackTicks[slot]
                    : Math.max(ackTicks[slot], droppedStartTicks[slot]);
            long lossEnd = Math.max(lossStart, droppedEndTicks[slot]);
            losses = new SourceResyncSnapshot.SourceResyncLoss[]{
                    new SourceResyncSnapshot.SourceResyncLoss(
                            SourceResyncSnapshot.SourceResyncLoss.Reason.HISTORY_EXHAUSTED,
                            lossStart,
                            lossEnd,
                            Math.max(ackWatermarks[slot] + 1L,
                                    droppedFirstWatermarks[slot]),
                            droppedLastWatermarks[slot],
                            Math.max(1, droppedSegmentCounts[slot]),
                            canonicalZero(missingEnergy),
                            null
                    )
            };
        } else {
            losses = new SourceResyncSnapshot.SourceResyncLoss[0];
        }
        return new SourceResyncSnapshot(
                sourceIds[slot],
                sourceRevisions[slot],
                eventWatermarks[slot],
                snapshotTick,
                ackWatermarks[slot],
                ackTicks[slot],
                ackCumulativeEnergyJ[slot],
                cumulativeEnergyJ[slot],
                effectivePower(slot),
                enabled[slot] != 0,
                mode(slot),
                ports(slot),
                retained,
                losses
        );
    }

    /** Advances retention only after the worker has accepted this exact snapshot. */
    public boolean acknowledge(SourceResyncSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        int slot = findSource(snapshot.sourceId());
        if (slot == NO_SOURCE
                || snapshot.baseAckWatermark() != ackWatermarks[slot]
                || snapshot.baseAckTick() != ackTicks[slot]
                || !close(snapshot.baseAckCumulativeEnergyJ(),
                        ackCumulativeEnergyJ[slot])
                || snapshot.eventWatermark() < ackWatermarks[slot]
                || snapshot.eventWatermark() > eventWatermarks[slot]
                || snapshot.snapshotTick() < ackTicks[slot]
                || snapshot.snapshotTick() > lastLedgerTicks[slot]
                || !withinChecksumTolerance(
                        snapshot.checksumResidualJ(),
                        snapshot.cumulativeEmittedEnergyJ())) {
            return false;
        }
        ackWatermarks[slot] = snapshot.eventWatermark();
        ackTicks[slot] = snapshot.snapshotTick();
        ackCumulativeEnergyJ[slot] = snapshot.cumulativeEmittedEnergyJ();
        pruneAcknowledgedHistory(slot);
        if (droppedLastWatermarks[slot] <= ackWatermarks[slot]) {
            clearDroppedHistory(slot);
        }
        return true;
    }

    public double routedEnergyJ(long sourceId, SourceBinding.Kind bindingKind) {
        Objects.requireNonNull(bindingKind, "bindingKind");
        int slot = requireSource(sourceId);
        return routedEnergyJ[routeOffset(slot, bindingKind)];
    }

    private void beginEvent(int sourceSlot, long effectiveTick) {
        requireTick("effectiveTick", effectiveTick);
        if (effectiveTick < lastLedgerTicks[sourceSlot]) {
            throw new IllegalArgumentException(
                    "effectiveTick precedes source ledger tick: "
                            + effectiveTick + " < " + lastLedgerTicks[sourceSlot]);
        }
        long watermark = nextWatermark();
        settleLedger(sourceSlot, effectiveTick, watermark);
        eventWatermarks[sourceSlot] = watermark;
    }

    private void settleLedger(int sourceSlot, long targetTick, long watermark) {
        long previousTick = lastLedgerTicks[sourceSlot];
        if (targetTick == previousTick) {
            return;
        }
        double totalEnergy = finiteProduct(
                "source interval energy",
                effectivePower(sourceSlot),
                (targetTick - previousTick) / TICKS_PER_SECOND
        );
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
                                (targetTick - previousTick) / TICKS_PER_SECOND
                        );
                if (index != portCounts[sourceSlot] - 1) {
                    assigned = finiteSum("assigned source energy", assigned, portEnergy);
                }
                if (portEnergy != 0.0D) {
                    SourceBinding binding = readBinding(portOffset);
                    if (binding.kind() == SourceBinding.Kind.UNBOUND) {
                        binding = SourceBinding.degradedLoss(sourceIds[sourceSlot]);
                    }
                    appendSegment(
                            sourceSlot,
                            watermark,
                            previousTick,
                            targetTick,
                            portOffset,
                            binding,
                            portEnergy
                    );
                    routeEnergy(sourceSlot, binding.kind(), portEnergy);
                }
            }
            addCumulative(sourceSlot, totalEnergy);
        }
        lastLedgerTicks[sourceSlot] = targetTick;
    }

    private void installEffectivePower(int sourceSlot, long eventTick, double totalPowerW) {
        int firstPort = portBase(sourceSlot);
        double assigned = 0.0D;
        for (int index = 0; index < portCounts[sourceSlot]; index++) {
            int portOffset = firstPort + index;
            double contribution = index == portCounts[sourceSlot] - 1
                    ? totalPowerW - assigned
                    : finiteProduct(
                            "source port power",
                            totalPowerW,
                            portPowerShares[portOffset]
                    );
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

    private void appendSegment(
            int sourceSlot,
            long watermark,
            long startTick,
            long endTick,
            int portOffset,
            SourceBinding binding,
            double energyJ
    ) {
        int head = historyHeads[sourceSlot];
        int size = historySizes[sourceSlot];
        int ringIndex;
        if (size < historyCapacityPerSource) {
            ringIndex = (head + size) % historyCapacityPerSource;
            historySizes[sourceSlot] = size + 1;
        } else {
            ringIndex = head;
            int overwrittenOffset = historyOffset(sourceSlot, ringIndex);
            rememberDroppedHistory(sourceSlot, overwrittenOffset);
            historyHeads[sourceSlot] = (head + 1) % historyCapacityPerSource;
        }
        int historyOffset = historyOffset(sourceSlot, ringIndex);
        historyWatermarks[historyOffset] = watermark;
        historyStartTicks[historyOffset] = startTick;
        historyEndTicks[historyOffset] = endTick;
        historyPortIds[historyOffset] = portIds[portOffset];
        historyPortRevisions[historyOffset] = portRevisions[portOffset];
        historyChannels[historyOffset] = portChannels[portOffset];
        historyBindingKinds[historyOffset] = (byte) binding.kind().ordinal();
        historyTargetIds[historyOffset] = binding.targetId();
        historyBindingGenerations[historyOffset] = binding.lifecycleGeneration();
        historyEnergyJ[historyOffset] = canonicalZero(energyJ);
    }

    private void rememberDroppedHistory(int sourceSlot, int historyOffset) {
        long watermark = historyWatermarks[historyOffset];
        if (watermark <= ackWatermarks[sourceSlot]) {
            return;
        }
        droppedFirstWatermarks[sourceSlot] = Math.min(
                droppedFirstWatermarks[sourceSlot], watermark);
        droppedLastWatermarks[sourceSlot] = Math.max(
                droppedLastWatermarks[sourceSlot], watermark);
        droppedStartTicks[sourceSlot] = Math.min(
                droppedStartTicks[sourceSlot], historyStartTicks[historyOffset]);
        droppedEndTicks[sourceSlot] = Math.max(
                droppedEndTicks[sourceSlot], historyEndTicks[historyOffset]);
        droppedSegmentCounts[sourceSlot] = Math.addExact(
                droppedSegmentCounts[sourceSlot], 1);
    }

    private SourceResyncSnapshot.BindingEnergySegment[] retainedSegments(int sourceSlot) {
        int count = 0;
        int head = historyHeads[sourceSlot];
        for (int index = 0; index < historySizes[sourceSlot]; index++) {
            int offset = historyOffset(
                    sourceSlot, (head + index) % historyCapacityPerSource);
            if (historyWatermarks[offset] > ackWatermarks[sourceSlot]) {
                count++;
            }
        }
        SourceResyncSnapshot.BindingEnergySegment[] result =
                new SourceResyncSnapshot.BindingEnergySegment[count];
        int write = 0;
        for (int index = 0; index < historySizes[sourceSlot]; index++) {
            int offset = historyOffset(
                    sourceSlot, (head + index) % historyCapacityPerSource);
            if (historyWatermarks[offset] <= ackWatermarks[sourceSlot]) {
                continue;
            }
            result[write++] = new SourceResyncSnapshot.BindingEnergySegment(
                    historyWatermarks[offset],
                    historyStartTicks[offset],
                    historyEndTicks[offset],
                    historyPortIds[offset],
                    historyPortRevisions[offset],
                    SourceChannel.values()[historyChannels[offset]],
                    new SourceBinding(
                            SourceBinding.Kind.values()[historyBindingKinds[offset]],
                            historyTargetIds[offset],
                            historyBindingGenerations[offset]
                    ),
                    historyEnergyJ[offset]
            );
        }
        return result;
    }

    private void pruneAcknowledgedHistory(int sourceSlot) {
        int head = historyHeads[sourceSlot];
        int size = historySizes[sourceSlot];
        while (size > 0) {
            int offset = historyOffset(sourceSlot, head);
            if (historyWatermarks[offset] > ackWatermarks[sourceSlot]) {
                break;
            }
            head = (head + 1) % historyCapacityPerSource;
            size--;
        }
        historyHeads[sourceSlot] = head;
        historySizes[sourceSlot] = size;
    }

    private void clearDroppedHistory(int sourceSlot) {
        droppedFirstWatermarks[sourceSlot] = Long.MAX_VALUE;
        droppedLastWatermarks[sourceSlot] = 0L;
        droppedStartTicks[sourceSlot] = Long.MAX_VALUE;
        droppedEndTicks[sourceSlot] = 0L;
        droppedSegmentCounts[sourceSlot] = 0;
    }

    private void addCumulative(int sourceSlot, double energyJ) {
        double adjusted = energyJ - cumulativeCompensationJ[sourceSlot];
        double updated = cumulativeEnergyJ[sourceSlot] + adjusted;
        requireFiniteResult("cumulative source energy", updated);
        cumulativeCompensationJ[sourceSlot] =
                (updated - cumulativeEnergyJ[sourceSlot]) - adjusted;
        cumulativeEnergyJ[sourceSlot] = canonicalZero(updated);
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
        return ThermalSourceMode.values()[sourceModes[sourceSlot]];
    }

    private EmissionPort[] ports(int sourceSlot) {
        EmissionPort[] result = new EmissionPort[portCounts[sourceSlot]];
        int firstPort = portBase(sourceSlot);
        for (int index = 0; index < result.length; index++) {
            result[index] = readPort(firstPort + index);
        }
        return result;
    }

    private void writePort(int portOffset, EmissionPort port) {
        portIds[portOffset] = port.portId();
        portRevisions[portOffset] = port.portRevision();
        portChannels[portOffset] = (byte) port.channel().ordinal();
        portPowerShares[portOffset] = port.powerShare();
        writeBinding(portOffset, port.binding());
    }

    private EmissionPort readPort(int portOffset) {
        return new EmissionPort(
                portIds[portOffset],
                portRevisions[portOffset],
                SourceChannel.values()[portChannels[portOffset]],
                portPowerShares[portOffset],
                readBinding(portOffset)
        );
    }

    private void writeBinding(int portOffset, SourceBinding binding) {
        portBindingKinds[portOffset] = (byte) binding.kind().ordinal();
        portTargetIds[portOffset] = binding.targetId();
        portBindingGenerations[portOffset] = binding.lifecycleGeneration();
    }

    private SourceBinding readBinding(int portOffset) {
        return new SourceBinding(
                SourceBinding.Kind.values()[portBindingKinds[portOffset]],
                portTargetIds[portOffset],
                portBindingGenerations[portOffset]
        );
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
                        port.portRevision(),
                        port.channel(),
                        port.powerShare(),
                        SourceBinding.degradedLoss(sourceId)
                );
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

    private long nextWatermark() {
        nextEventWatermark = Math.incrementExact(nextEventWatermark);
        return nextEventWatermark;
    }

    private void ensureSourceStorage(int requiredCapacity) {
        if (requiredCapacity <= sourceIds.length) {
            return;
        }
        int newCapacity = Math.max(requiredCapacity, sourceIds.length << 1);
        resizeStorage(newCapacity);
    }

    private void allocateStorage(int capacity) {
        sourceIds = new long[capacity];
        packedPositions = new long[capacity];
        profileIds = new int[capacity];
        lifecycleGenerations = new int[capacity];
        sourceRevisions = new int[capacity];
        sourceModes = new byte[capacity];
        enabled = new byte[capacity];
        unloaded = new byte[capacity];
        portCounts = new int[capacity];
        declaredPowerW = new double[capacity];
        lastLedgerTicks = new long[capacity];
        eventWatermarks = new long[capacity];
        cumulativeEnergyJ = new double[capacity];
        cumulativeCompensationJ = new double[capacity];
        ackWatermarks = new long[capacity];
        ackTicks = new long[capacity];
        ackCumulativeEnergyJ = new double[capacity];
        historyHeads = new int[capacity];
        historySizes = new int[capacity];
        droppedFirstWatermarks = new long[capacity];
        droppedLastWatermarks = new long[capacity];
        droppedStartTicks = new long[capacity];
        droppedEndTicks = new long[capacity];
        droppedSegmentCounts = new int[capacity];
        routedEnergyJ = new double[Math.multiplyExact(
                capacity, SourceBinding.Kind.values().length)];
        Arrays.fill(droppedFirstWatermarks, Long.MAX_VALUE);
        Arrays.fill(droppedStartTicks, Long.MAX_VALUE);

        int portCapacity = Math.multiplyExact(capacity, maxPortsPerSource);
        portIds = new int[portCapacity];
        portRevisions = new int[portCapacity];
        portChannels = new byte[portCapacity];
        portPowerShares = new double[portCapacity];
        portContributionW = new double[portCapacity];
        portBindingKinds = new byte[portCapacity];
        portTargetIds = new long[portCapacity];
        portBindingGenerations = new int[portCapacity];

        int historyCapacity = Math.multiplyExact(capacity, historyCapacityPerSource);
        historyWatermarks = new long[historyCapacity];
        historyStartTicks = new long[historyCapacity];
        historyEndTicks = new long[historyCapacity];
        historyPortIds = new int[historyCapacity];
        historyPortRevisions = new int[historyCapacity];
        historyChannels = new byte[historyCapacity];
        historyBindingKinds = new byte[historyCapacity];
        historyTargetIds = new long[historyCapacity];
        historyBindingGenerations = new int[historyCapacity];
        historyEnergyJ = new double[historyCapacity];
    }

    private void resizeStorage(int newCapacity) {
        int oldCapacity = sourceIds.length;
        sourceIds = Arrays.copyOf(sourceIds, newCapacity);
        packedPositions = Arrays.copyOf(packedPositions, newCapacity);
        profileIds = Arrays.copyOf(profileIds, newCapacity);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, newCapacity);
        sourceRevisions = Arrays.copyOf(sourceRevisions, newCapacity);
        sourceModes = Arrays.copyOf(sourceModes, newCapacity);
        enabled = Arrays.copyOf(enabled, newCapacity);
        unloaded = Arrays.copyOf(unloaded, newCapacity);
        portCounts = Arrays.copyOf(portCounts, newCapacity);
        declaredPowerW = Arrays.copyOf(declaredPowerW, newCapacity);
        lastLedgerTicks = Arrays.copyOf(lastLedgerTicks, newCapacity);
        eventWatermarks = Arrays.copyOf(eventWatermarks, newCapacity);
        cumulativeEnergyJ = Arrays.copyOf(cumulativeEnergyJ, newCapacity);
        cumulativeCompensationJ = Arrays.copyOf(cumulativeCompensationJ, newCapacity);
        ackWatermarks = Arrays.copyOf(ackWatermarks, newCapacity);
        ackTicks = Arrays.copyOf(ackTicks, newCapacity);
        ackCumulativeEnergyJ = Arrays.copyOf(ackCumulativeEnergyJ, newCapacity);
        historyHeads = Arrays.copyOf(historyHeads, newCapacity);
        historySizes = Arrays.copyOf(historySizes, newCapacity);
        droppedFirstWatermarks = Arrays.copyOf(droppedFirstWatermarks, newCapacity);
        droppedLastWatermarks = Arrays.copyOf(droppedLastWatermarks, newCapacity);
        droppedStartTicks = Arrays.copyOf(droppedStartTicks, newCapacity);
        droppedEndTicks = Arrays.copyOf(droppedEndTicks, newCapacity);
        droppedSegmentCounts = Arrays.copyOf(droppedSegmentCounts, newCapacity);
        Arrays.fill(droppedFirstWatermarks, oldCapacity, newCapacity, Long.MAX_VALUE);
        Arrays.fill(droppedStartTicks, oldCapacity, newCapacity, Long.MAX_VALUE);
        routedEnergyJ = Arrays.copyOf(
                routedEnergyJ,
                Math.multiplyExact(newCapacity, SourceBinding.Kind.values().length)
        );

        int newPortCapacity = Math.multiplyExact(newCapacity, maxPortsPerSource);
        portIds = Arrays.copyOf(portIds, newPortCapacity);
        portRevisions = Arrays.copyOf(portRevisions, newPortCapacity);
        portChannels = Arrays.copyOf(portChannels, newPortCapacity);
        portPowerShares = Arrays.copyOf(portPowerShares, newPortCapacity);
        portContributionW = Arrays.copyOf(portContributionW, newPortCapacity);
        portBindingKinds = Arrays.copyOf(portBindingKinds, newPortCapacity);
        portTargetIds = Arrays.copyOf(portTargetIds, newPortCapacity);
        portBindingGenerations = Arrays.copyOf(portBindingGenerations, newPortCapacity);

        int newHistoryCapacity = Math.multiplyExact(newCapacity, historyCapacityPerSource);
        historyWatermarks = Arrays.copyOf(historyWatermarks, newHistoryCapacity);
        historyStartTicks = Arrays.copyOf(historyStartTicks, newHistoryCapacity);
        historyEndTicks = Arrays.copyOf(historyEndTicks, newHistoryCapacity);
        historyPortIds = Arrays.copyOf(historyPortIds, newHistoryCapacity);
        historyPortRevisions = Arrays.copyOf(historyPortRevisions, newHistoryCapacity);
        historyChannels = Arrays.copyOf(historyChannels, newHistoryCapacity);
        historyBindingKinds = Arrays.copyOf(historyBindingKinds, newHistoryCapacity);
        historyTargetIds = Arrays.copyOf(historyTargetIds, newHistoryCapacity);
        historyBindingGenerations = Arrays.copyOf(historyBindingGenerations, newHistoryCapacity);
        historyEnergyJ = Arrays.copyOf(historyEnergyJ, newHistoryCapacity);
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

    private int historyOffset(int sourceSlot, int ringIndex) {
        return sourceSlot * historyCapacityPerSource + ringIndex;
    }

    private int routeOffset(int sourceSlot, SourceBinding.Kind kind) {
        return sourceSlot * SourceBinding.Kind.values().length + kind.ordinal();
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

    private static double compensatedSegmentSum(
            SourceResyncSnapshot.BindingEnergySegment[] segments
    ) {
        double sum = 0.0D;
        double compensation = 0.0D;
        for (SourceResyncSnapshot.BindingEnergySegment segment : segments) {
            double adjusted = segment.signedEnergyJ() - compensation;
            double next = sum + adjusted;
            requireFiniteResult("retained source energy", next);
            compensation = (next - sum) - adjusted;
            sum = next;
        }
        return canonicalZero(sum);
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

    private static boolean withinChecksumTolerance(double residual, double cumulative) {
        return Math.abs(residual) <= 1.0e-10D * Math.max(1.0D, Math.abs(cumulative));
    }

    private static boolean close(double left, double right) {
        return withinChecksumTolerance(
                left - right,
                Math.max(Math.abs(left), Math.abs(right))
        );
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

    private static double finiteDifference(String name, double left, double right) {
        double result = left - right;
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

    public record ThermalSourceEntry(
            long sourceId,
            long packedPosition,
            int profileId,
            int lifecycleGeneration,
            int sourceRevision,
            long eventWatermark,
            ThermalSourceMode mode,
            double declaredPowerW,
            boolean enabled,
            boolean unloaded,
            long lastLedgerTick,
            double cumulativeEmittedEnergyJ,
            int portCount
    ) {
    }

    enum UnloadStatus {
        APPLIED,
        NOT_FOUND,
        STALE_GENERATION,
        ALREADY_UNLOADED
    }
}
