/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;

import java.util.Arrays;
import java.util.Objects;

/**
 * Worker-owned source identity, port binding and committed energy accounting.
 * SpatialHeatStore owns all accepted, unconsumed heat.
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

    private long[] sourceIds;
    private int[] lifecycleGenerations;
    private byte[] sourceModes;
    private byte[] enabled;
    private byte[] occupied;
    private int[] portCounts;
    private double[] declaredPowerW;
    private long[] lastEventTicks;
    private long[] boundThroughTicks;
    private double spatialDeliveredJ, spatialUnacceptedJ;
    private double exportedEnergyJ;
    private int[] nextFree;

    private int[] portIds;
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
    private double outputEnergyJ, declaredLossEnergyJ, degradedLossEnergyJ;
    private com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation storage;

    public void storageOwner(com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation owner) {
        storage = owner;
    }

    private long columnBytes(int capacity) {
        int ports = Math.multiplyExact(capacity, maxPortsPerSource);
        return 4 * AirBackingMeter.arrayPayloadBytes(capacity, 8)
                + 3 * AirBackingMeter.arrayPayloadBytes(capacity, 4)
                + 3 * AirBackingMeter.arrayPayloadBytes(capacity, 1)
                + AirBackingMeter.arrayPayloadBytes(ports, 8)
                + 2 * AirBackingMeter.arrayPayloadBytes(ports, 4) + AirBackingMeter.arrayPayloadBytes(ports, 1);
    }
    private static long indexBytes(int capacity) {
        return AirBackingMeter.arrayPayloadBytes(capacity, 8) + AirBackingMeter.arrayPayloadBytes(capacity, 4)
                + AirBackingMeter.arrayPayloadBytes(capacity, 1);
    }

    /** Producer ownership is accepted independently of source binding and physical completion. */
    public void acceptSpatialEnergy(double joules) { outputEnergyJ += joules; }

    public void prepareSourcePrefix(SourceInputWindow window) {
        if (!window.first() || !window.registration) return;
        ensureStorageForAdditional(1); reserveTable(1);
    }

    /** Bind an already accepted prefix. This operation never records its energy a second time. */
    public void bindSpatialPrefix(SourceInputWindow window, long through,
            BindingUpdates bindings, CutLoadBuffer output) {
        requireOpen();
        if (!window.canBind(through) || through > window.toTick)
            throw new IllegalArgumentException("Invalid source prefix");
        var descriptor = window.descriptor;
        int source = findSource(descriptor.sourceId());
        if (window.first() && window.registration) {
            if (source >= 0) throw new IllegalStateException("Source replacement overtook its old terminal window");
            cursorTick = window.fromTick;
            registerSource(descriptor.sourceId(), descriptor.generation(), descriptor.mode(),
                    window.declaredPowerAt(window.fromTick), window.enabledAt(window.fromTick), window.fromTick, descriptor.ports());
            source = requireSource(descriptor.sourceId());
            boundThroughTicks[source] = window.fromTick;
            bindings.registered(descriptor, window.fromTick, this);
        } else if (source < 0 || lifecycleGenerations[source] != descriptor.generation()
                || window.through() != boundThroughTicks[source]) {
            throw new IllegalStateException("Source prefix skipped or repeated its predecessor");
        }
        output.sourceContext(descriptor.sourceId(), descriptor.generation());
        for (int index = 0; index < window.size; index++) {
            var span = window.span(index);
            long from = Math.max(window.through(), span.fromTick()), to = Math.min(through, span.toTick());
            boolean impulse = span.fromTick() == span.toTick();
            if (impulse ? span.fromTick() > through || span.fromTick() < window.through()
                    || span.fromTick() == window.through() && !window.first() : to <= from) continue;
            if (span.port() >= 0) {
                int port = requirePortOffset(source, span.port());
                bindSpatialEnergy(port, from, to, window.take(index, port - portBase(source), from, to), output);
            } else {
                int base = portBase(source), count = portCounts[source];
                for (int port = 0; port < count; port++)
                    bindSpatialEnergy(base + port, from, to, window.take(index, port, from, to), output);
            }
        }
        // Membership at a fence describes that fence, never the future end of
        // the transport envelope. Energy above was settled using the old binding.
        double power = window.declaredPowerAt(through);
        boolean active = window.enabledAt(through);
        boundThroughTicks[source] = through;
        if (declaredPowerW[source] != power) {
            cursorTick = through; setPower(descriptor.sourceId(), power, through);
            bindings.emissionChanged(descriptor, this);
        }
        if ((enabled[source] != 0) != active) {
            cursorTick = through; setEnabled(descriptor.sourceId(), active, through);
            bindings.emissionChanged(descriptor, this);
        }
        if (through == window.toTick && window.unload) {
            cursorTick = through; unloadSource(descriptor.sourceId(), descriptor.generation(), through);
            bindings.unloaded(descriptor);
        }
        window.boundThrough(through);
    }

    private void bindSpatialEnergy(int port, long from, long to, double joules, CutLoadBuffer output) {
        if (joules == 0) return;
        if (isThermalTarget(port)) {
            if (from == to) output.impulse(accumulatorTargetId(port), portBindingGenerations[port], to, joules);
            else output.span(accumulatorTargetId(port), portBindingGenerations[port], from, to, joules * 20.0 / (to - from));
        } else if (portBindingKinds[port] == SourceBinding.Kind.DECLARED_LOSS.ordinal()) declaredLossEnergyJ += joules;
        else degradedLossEnergyJ += joules;
    }

    public long spatialBoundThrough(long sourceId, int generation) {
        int slot = findSource(sourceId);
        if (slot < 0 || lifecycleGenerations[slot] != generation) return Long.MAX_VALUE;
        if (SOURCE_MODES[Byte.toUnsignedInt(sourceModes[slot])] == ThermalSourceMode.POWER_SOURCE
                && (enabled[slot] == 0 || declaredPowerW[slot] == 0)) return Long.MAX_VALUE;
        return boundThroughTicks[slot];
    }

    public static double integralJ(double watts, long fromTick, long toTick) {
        if (toTick < fromTick) throw new IllegalArgumentException("Source interval moved backwards");
        return finiteProduct("source energy", watts, (toTick - fromTick) / 20.0);
    }

    public ThermalSourceLedger(
            long initialTick,
            int initialSourceCapacity,
            int maxPortsPerSource,
            int maximumSources
    ) {
        if (initialTick < 0L
                || initialSourceCapacity < 0 || maxPortsPerSource <= 0
                || maximumSources <= 0
                || initialSourceCapacity > maximumSources) {
            throw new IllegalArgumentException("source ledger configuration is invalid");
        }
        this.maxPortsPerSource = maxPortsPerSource;
        this.maximumSources = maximumSources;
        int capacity = Math.max(1, initialSourceCapacity);
        allocateStorage(capacity);
        allocateTable(tableCapacity(capacity));
        cursorTick = initialTick;
    }

    public void recordBacking(AirBackingMeter meter) {
        meter.array(sourceIds); meter.array(lifecycleGenerations); meter.array(sourceModes);
        meter.array(enabled); meter.array(occupied); meter.array(portCounts); meter.array(declaredPowerW);
        meter.array(lastEventTicks); meter.array(nextFree); meter.array(portIds);
        meter.array(boundThroughTicks);
        meter.array(portBindingKinds); meter.array(portTargetIds);
        meter.array(portBindingGenerations); meter.array(tableSourceIds); meter.array(tableSlots); meter.array(tableStates);
    }

    /** Called once with the committed numerical result, never for a discarded trial. */
    public void confirmRecordedDelivery(double deliveredJ, double unacceptedJ) {
        spatialDeliveredJ += deliveredJ; spatialUnacceptedJ += unacceptedJ;
    }

    /** A deferred port was resolved to an existing modeled sink, not discarded for capacity. */
    public void confirmRecordedLoss(double joules, boolean unavailableGeometry) {
        if (unavailableGeometry) degradedLossEnergyJ += joules;
        else declaredLossEnergyJ += joules;
    }
    public void exportPendingEnergy(double recordedJ, double unrecordedJ) {
        outputEnergyJ += unrecordedJ;
        exportedEnergyJ += recordedJ + unrecordedJ;
    }
    public void restorePendingEnergy(double joules) {
        outputEnergyJ += joules;
    }
    public void settleUnrecordedLoss(double declaredJ, double degradedJ) {
        outputEnergyJ += declaredJ + degradedJ; declaredLossEnergyJ += declaredJ; degradedLossEnergyJ += degradedJ;
    }

    public boolean suppliesPower(long sourceId) {
        int slot = findSource(sourceId);
        return slot != NO_SOURCE && effectivePower(slot) > 0;
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
            writeBinding(portOffset, nextBinding);
        }
        return true;
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
            writePort(firstPort + index, ports[index]);
        }
        liveSourceCount++;
        insertTable(sourceId, slot);
    }

    private void setPower(long sourceId, double powerW, long eventTick) {
        requireFinite("powerW", powerW);
        int slot = requireEventSource(sourceId, eventTick);
        if (mode(slot) != ThermalSourceMode.POWER_SOURCE) {
            throw new IllegalStateException("only POWER_SOURCE holds continuous power");
        }
        declaredPowerW[slot] = canonicalZero(powerW);
    }

    private void setEnabled(long sourceId, boolean sourceEnabled, long eventTick) {
        int slot = requireEventSource(sourceId, eventTick);
        enabled[slot] = sourceEnabled ? (byte) 1 : 0;
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
            clearPort(portOffset);
        }
        removeTable(sourceId);
        clearSource(slot);
    }

    // Declared loss is a modeled sink, degraded loss is unavailable topology, and unaccepted
    // energy reached a node that could not absorb it (for example a completed latent plateau).
    public record EnergyBalance(double outputJ, double deliveredJ, double declaredLossJ,
            double degradedLossJ, double unacceptedJ, double exportedJ) { }

    public EnergyBalance energyBalance() {
        return new EnergyBalance(outputEnergyJ, spatialDeliveredJ, declaredLossEnergyJ,
                degradedLossEnergyJ, spatialUnacceptedJ, exportedEnergyJ);
    }

    private void writePort(int offset, EmissionPort port) {
        portIds[offset] = port.portId();
        writeBinding(offset, port.binding());
    }

    private void clearPort(int offset) {
        portIds[offset] = 0;
        portBindingKinds[offset] = 0;
        portTargetIds[offset] = 0L;
        portBindingGenerations[offset] = 0;
    }

    private void writeBinding(int offset, SourceBinding binding) {
        portBindingKinds[offset] = (byte) binding.kind().ordinal();
        portTargetIds[offset] = binding.targetId();
        portBindingGenerations[offset] = binding.lifecycleGeneration();
    }

    private boolean isThermalTarget(int offset) {
        return portBindingKinds[offset]
                == (byte) SourceBinding.Kind.THERMAL_NODE.ordinal()
                || portBindingKinds[offset] == (byte) SourceBinding.Kind.AIR_STENCIL.ordinal();
    }

    private long accumulatorTargetId(int offset) {
        return portBindingKinds[offset] == (byte) SourceBinding.Kind.AIR_STENCIL.ordinal()
                ? portTargetIds[offset] | 1L << 31 : portTargetIds[offset];
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
        boundThroughTicks = new long[capacity];
        nextFree = new int[capacity];
        Arrays.fill(nextFree, NO_SOURCE);

        int ports = Math.multiplyExact(capacity, maxPortsPerSource);
        portIds = new int[ports];
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
        if (storage != null) storage.beforeAllocation(columnBytes(capacity));
        sourceIds = Arrays.copyOf(sourceIds, capacity);
        lifecycleGenerations = Arrays.copyOf(lifecycleGenerations, capacity);
        sourceModes = Arrays.copyOf(sourceModes, capacity);
        enabled = Arrays.copyOf(enabled, capacity);
        occupied = Arrays.copyOf(occupied, capacity);
        portCounts = Arrays.copyOf(portCounts, capacity);
        declaredPowerW = Arrays.copyOf(declaredPowerW, capacity);
        lastEventTicks = Arrays.copyOf(lastEventTicks, capacity);
        boundThroughTicks = Arrays.copyOf(boundThroughTicks, capacity);
        nextFree = Arrays.copyOf(nextFree, capacity);
        Arrays.fill(nextFree, old, capacity, NO_SOURCE);

        int portCapacity = Math.multiplyExact(capacity, maxPortsPerSource);
        portIds = Arrays.copyOf(portIds, portCapacity);
        portBindingKinds = Arrays.copyOf(portBindingKinds, portCapacity);
        portTargetIds = Arrays.copyOf(portTargetIds, portCapacity);
        portBindingGenerations = Arrays.copyOf(
                portBindingGenerations, portCapacity);
        if (storage != null) storage.afterReplacement(columnBytes(old));
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
        if (storage != null) storage.beforeAllocation(indexBytes(capacity));
        allocateTable(capacity);
        for (int index = 0; index < oldStates.length; index++) {
            if (oldStates[index] == OCCUPIED) {
                insertTable(oldIds[index], oldSlots[index]);
            }
        }
        if (storage != null) storage.afterReplacement(indexBytes(oldStates.length));
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

    /** Binding changes follow the accepted prefix's exact control order. */
    public interface BindingUpdates {
        void registered(SpatialSourceTimeline.Descriptor descriptor, long tick, ThermalSourceLedger ledger);
        void emissionChanged(SpatialSourceTimeline.Descriptor descriptor, ThermalSourceLedger ledger);
        void unloaded(SpatialSourceTimeline.Descriptor descriptor);
    }
}
