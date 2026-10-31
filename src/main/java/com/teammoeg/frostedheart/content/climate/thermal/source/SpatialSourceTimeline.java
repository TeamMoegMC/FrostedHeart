/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongArray;

/** Main-thread source capture. It integrates declared input, never the temperature field. */
public final class SpatialSourceTimeline implements AutoCloseable {
    private static final int EXACT_RECORDS = 64, CHANNELS = 4, WINDOW_RECORDS = EXACT_RECORDS + 2 * CHANNELS;
    private static final long ENTRY_BYTES = 1536L + 3 * AirBackingMeter.arrayPayloadBytes(WINDOW_RECORDS, 8)
            + 3L * WINDOW_RECORDS * 72 + 2L * CHANNELS * 48
            + 2 * AirBackingMeter.arrayPayloadBytes(WINDOW_RECORDS, Double.BYTES) + 1024
            + AirBackingMeter.arrayPayloadBytes(WINDOW_RECORDS, 8) + 176;

    /** Main publishes the first control still outside transport before advancing the envelope clock. */
    public static final class ControlTail {
        private volatile long unsealedTick = Long.MAX_VALUE;
        private final AtomicLongArray sealed = new AtomicLongArray(new long[] {
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE});
        public long firstTick() {
            long tick = unsealedTick;
            for (int index = 0; index < sealed.length(); index++) tick = Math.min(tick, sealed.get(index));
            return tick;
        }
    }

    /** Produced on main; accepted/exported only on the sole dimension coordinator. */
    private static final class PortTotals {
        final double[] produced = new double[CHANNELS * 2];
        final double[] accepted = new double[CHANNELS * 2];
        final double[] exported = new double[CHANNELS * 2];
        long acceptedThrough;
        PortTotals(long tick) { acceptedThrough = tick; }
        void produce(int port, double joules) { produced[port * 2 + (joules < 0 ? 1 : 0)] += joules; }
        double accept(int port, double joules) {
            int channel = port * 2 + (joules < 0 ? 1 : 0);
            accepted[channel] += joules;
            double skipped = Math.copySign(Math.min(Math.abs(joules), Math.abs(exported[channel])), joules);
            exported[channel] -= skipped;
            return joules - skipped;
        }
    }

    public record Descriptor(long sourceId, int generation, ThermalSourceMode mode,
            int x, int y, int z, int profile, EmissionPort[] ports) {
        public long section() {
            return SectionPos.asLong(x >> 4, y >> 4, z >> 4);
        }
    }
    public record Span(long fromTick, long toTick, double joules, int port, boolean aggregated,
            double declaredPowerW, boolean enabled) {
        Span(long fromTick, long toTick, double joules, int port, boolean aggregated) {
            this(fromTick, toTick, joules, port, aggregated, Double.NaN, false);
        }
    }

    /** A prepaid transport slot, released when the store accepts ownership. */
    public static final class Window implements AutoCloseable {
        private final Slot slot;
        private final ThermalMemoryBudget.BackingLease backing;
        private final AtomicBoolean ingressCredit;
        private final PortTotals totals;
        public final ControlTail controlTail;
        public final Descriptor descriptor;
        public final long fromTick, toTick;
        public final double finalPowerW;
        public final boolean enabled, registration, unload;
        public final boolean membershipChanged, controlsChanged;
        public final long boundaryTick;
        public final int size;
        private boolean released;

        private Window(Entry entry, Slot slot, long through, int count) {
            this.slot = slot; backing = entry.backing.retain(); descriptor = entry.descriptor;
            ingressCredit = entry.ingressCredit;
            totals = entry.totals;
            controlTail = entry.controlTail;
            fromTick = entry.sealedThrough; toTick = through; size = count;
            finalPowerW = entry.powerW; enabled = entry.enabled;
            registration = !entry.registered; unload = entry.unloaded;
            membershipChanged = registration || unload || entry.membershipChanged;
            controlsChanged = entry.dirty || membershipChanged;
            boundaryTick = entry.controlTick == Long.MAX_VALUE ? through : entry.controlTick;
        }
        public Span span(int index) { return slot.values[index]; }
        /** Called under the ingress transport lock, after this window's control is visible there. */
        public void announced() { controlTail.sealed.set(slot.index, Long.MAX_VALUE); }
        public double acceptPort(int port, double joules) { return totals.accept(port, joules); }
        public void acceptedThrough(long tick) { totals.acceptedThrough = tick; }
        public ThermalMemoryBudget.Reservation takeIngressCredit() {
            return ingressCredit.compareAndSet(false, true) ? backing.split(SpatialSourceIngress.metadataBytes(descriptor.ports.length)) : null;
        }
        @Override public synchronized void close() {
            if (released) return;
            released = true;
            announced();
            java.util.Arrays.fill(slot.values, 0, size, null);
            slot.busy.set(false);
            backing.close();
        }
    }
    private static final class Slot {
        final int index;
        final Span[] values = new Span[WINDOW_RECORDS];
        final AtomicBoolean busy = new AtomicBoolean();
        Slot(int index) { this.index = index; }
    }
    private static final class Total {
        long from = Long.MAX_VALUE, to = Long.MIN_VALUE;
        double joules;
        void add(Span span) { from = Math.min(from, span.fromTick); to = Math.max(to, span.toTick); joules += span.joules; }
        boolean empty() { return from == Long.MAX_VALUE; }
        void clear() { from = Long.MAX_VALUE; to = Long.MIN_VALUE; joules = 0; }
    }
    private static final class Entry {
        final Descriptor descriptor;
        final ThermalMemoryBudget.BackingLease backing;
        final ArrayDeque<Span> exact = new ArrayDeque<>(EXACT_RECORDS);
        final Total[] older = new Total[CHANNELS * 2];
        final Slot[] slots = {new Slot(0), new Slot(1)};
        // Retirement moves the existing exact/summary tail here; it creates no third record pool.
        final Slot terminalSlot = new Slot(2);
        final ControlTail controlTail = new ControlTail();
        final AtomicBoolean ingressCredit = new AtomicBoolean();
        final PortTotals totals;
        long tailTick, sealedThrough;
        double powerW;
        boolean enabled, registered, unloaded;
        boolean dirty = true;
        boolean terminalSealed;
        boolean membershipChanged;
        long controlTick;

        Entry(Descriptor descriptor, double watts, boolean enabled, long tick, ThermalMemoryBudget.Reservation charge) {
            this.descriptor = descriptor; powerW = watts; this.enabled = enabled;
            tailTick = sealedThrough = tick;
            totals = new PortTotals(tick);
            controlTick = tick;
            backing = charge.shareBacking(charge.bytes()); charge.close();
            for (int i = 0; i < older.length; i++) older[i] = new Total();
        }
        void closeTail(long tick) {
            if (tick < tailTick) throw new IllegalArgumentException("Source event time moved backwards");
            if (descriptor.mode == ThermalSourceMode.POWER_SOURCE && tick > tailTick)
                append(new Span(tailTick, tick, enabled ? ThermalSourceLedger.integralJ(powerW, tailTick, tick) : 0,
                        -1, false, powerW, enabled));
            tailTick = tick;
        }
        boolean emitting() { return enabled && descriptor.mode == ThermalSourceMode.POWER_SOURCE && powerW > 0; }
        void append(Span span) {
            if (span.port >= 0) totals.produce(portIndex(span.port), span.joules);
            else {
                double assigned = 0;
                for (int port = 0; port < descriptor.ports.length; port++) {
                    double share = port == descriptor.ports.length - 1 ? span.joules - assigned
                            : span.joules * descriptor.ports[port].powerShare();
                    assigned += share; totals.produce(port, share);
                }
            }
            // Closing a producer for saving does not introduce a new numerical event.
            var previousTail = exact.peekLast();
            if (previousTail != null && previousTail.toTick == span.fromTick && previousTail.fromTick < previousTail.toTick
                    && span.fromTick < span.toTick && previousTail.port == span.port && previousTail.enabled == span.enabled
                    && Double.doubleToLongBits(previousTail.declaredPowerW) == Double.doubleToLongBits(span.declaredPowerW)
                    && !previousTail.aggregated && !span.aggregated) {
                exact.removeLast();
                span = new Span(previousTail.fromTick, span.toTick, previousTail.joules + span.joules, span.port, false, span.declaredPowerW, span.enabled);
            }
            while (!exact.isEmpty() && (exact.size() == EXACT_RECORDS || exact.peekFirst().toTick < span.toTick - 40)) {
                Span previous = exact.removeFirst();
                int channel = previous.port < 0 ? 0 : portIndex(previous.port) + 1;
                older[2 * channel + (previous.joules < 0 ? 1 : 0)].add(previous);
            }
            exact.addLast(span);
        }
        int portIndex(int port) {
            for (int i = 0; i < descriptor.ports.length; i++) if (descriptor.ports[i].portId() == port) return i;
            throw new IllegalArgumentException("Unknown source port " + port);
        }
        Window seal(long tick) {
            closeTail(tick);
            Slot selected = null;
            if (unloaded) {
                if (terminalSlot.busy.compareAndSet(false, true)) selected = terminalSlot;
            } else for (var slot : slots) if (slot.busy.compareAndSet(false, true)) { selected = slot; break; }
            if (selected == null) { controlTail.unsealedTick = controlTick; return null; }
            // Keep the announcement visible across seal -> offer, even when an earlier
            // envelope already carried a later heartbeat while this tail had no slot.
            controlTail.sealed.set(selected.index, controlTick);
            controlTail.unsealedTick = Long.MAX_VALUE;
            int count = 0;
            for (int i = 0; i < older.length; i++) {
                Total total = older[i];
                if (total.empty()) continue;
                int port = i / 2 == 0 ? -1 : descriptor.ports[i / 2 - 1].portId();
                selected.values[count++] = new Span(total.from, total.to, total.joules, port, true);
                total.clear();
            }
            while (!exact.isEmpty()) selected.values[count++] = exact.removeFirst();
            Window window = new Window(this, selected, tick, count);
            sealedThrough = tick; registered = true; dirty = false;
            controlTick = Long.MAX_VALUE; membershipChanged = false;
            return window;
        }
    }

    private final ThermalMemoryBudget budget;
    private final int maximumEntries;
    private final Long2ObjectLinkedOpenHashMap<Entry> active = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<Entry> scheduled = new Long2ObjectLinkedOpenHashMap<>();
    private final ArrayDeque<Entry> retired = new ArrayDeque<>();
    private final ThermalMemoryBudget.Reservation indexCharge;
    private int indexCapacity;
    private boolean captureOpen;
    private long latestTick;
    public long latestTick() { return latestTick; }

    public SpatialSourceTimeline(ThermalMemoryBudget budget, int maximumSources) {
        this.budget = budget; maximumEntries = Math.multiplyExact(2, maximumSources);
        indexCharge = budget.reserveCompletion(1024);
    }

    public boolean register(Descriptor descriptor, double watts, boolean enabled, long tick) {
        if (descriptor.ports.length >= CHANNELS) throw new IllegalArgumentException("Too many source ports");
        if (active.containsKey(descriptor.sourceId)) throw new IllegalArgumentException("Source already registered");
        int size = active.size() + retired.size() + 1;
        if (size > maximumEntries) return false;
        if (size > indexCapacity) {
            int capacity = Math.max(16, Math.max(size, indexCapacity * 2));
            indexCharge.completeResize(1024L + 128L * capacity);
            indexCapacity = capacity;
        }
        // A real producer must record its bounded tail even when detailed
        // numerical work has exhausted its optional memory target.
        var reservation = budget.reserveCompletion(ENTRY_BYTES + SpatialSourceIngress.metadataBytes(descriptor.ports.length)
                + 96L * descriptor.ports.length);
        var entry = new Entry(descriptor, watts, enabled, tick, reservation);
        active.put(descriptor.sourceId, entry); scheduled.put(descriptor.sourceId, entry);
        latestTick = Math.max(latestTick, tick);
        return true;
    }
    public void power(long sourceId, double watts, long tick) {
        if (!Double.isFinite(watts)) throw new IllegalArgumentException("Non-finite source power");
        Entry entry = required(sourceId); entry.closeTail(tick); latestTick = Math.max(latestTick, tick);
        boolean wasEmitting = entry.emitting(); entry.powerW = watts; entry.dirty = true;
        scheduled.put(sourceId, entry);
        entry.controlTick = Math.min(entry.controlTick, tick);
        if (wasEmitting != entry.emitting()) {
            entry.controlTick = Math.min(entry.controlTick, tick); entry.membershipChanged = true;
        }
    }
    public void enabled(long sourceId, boolean enabled, long tick) {
        Entry entry = required(sourceId); entry.closeTail(tick); latestTick = Math.max(latestTick, tick);
        boolean wasEmitting = entry.emitting(); entry.enabled = enabled; entry.dirty = true;
        scheduled.put(sourceId, entry);
        entry.controlTick = Math.min(entry.controlTick, tick);
        if (wasEmitting != entry.emitting()) {
            entry.controlTick = Math.min(entry.controlTick, tick); entry.membershipChanged = true;
        }
    }
    public void impulse(long sourceId, int port, double joules, long tick) {
        Entry entry = required(sourceId);
        if (entry.descriptor.mode != ThermalSourceMode.IMPULSE || !Double.isFinite(joules))
            throw new IllegalArgumentException("Invalid source impulse");
        entry.closeTail(tick); latestTick = Math.max(latestTick, tick); entry.portIndex(port); entry.append(new Span(tick, tick, joules, port, false));
        entry.dirty = true; scheduled.put(sourceId, entry);
    }
    public void unload(long sourceId, int generation, long tick) {
        Entry entry = active.get(sourceId);
        if (entry == null || entry.descriptor.generation != generation) return;
        entry.closeTail(tick); latestTick = Math.max(latestTick, tick); entry.unloaded = true;
        entry.controlTick = Math.min(entry.controlTick, tick);
        active.remove(sourceId); scheduled.remove(sourceId); retired.addLast(entry);
    }
    private Entry required(long sourceId) {
        Entry entry = active.get(sourceId);
        if (entry == null) throw new IllegalArgumentException("Unknown source " + sourceId);
        return entry;
    }

    /** Retired generations precede replacement registrations at the same world position. */
    public Window[] seal(long tick) {
        var windows = new ArrayList<Window>(scheduled.size() + retired.size());
        var retiredIterator = retired.iterator();
        while (retiredIterator.hasNext()) {
            Entry entry = retiredIterator.next();
            if (entry.terminalSealed) {
                if (entry.slots[0].busy.get() || entry.slots[1].busy.get() || entry.terminalSlot.busy.get()) continue;
                retiredIterator.remove(); entry.backing.close();
                continue;
            }
            Window window = entry.seal(entry.tailTick);
            if (window == null) continue;
            windows.add(window); entry.terminalSealed = true;
        }
        var selected = scheduled.values().iterator();
        while (selected.hasNext()) {
            Entry entry = selected.next();
            Window window = entry.seal(tick);
            if (window != null) {
                windows.add(window);
                // Clean inactive power sources need no zero-energy transport. Impulses
                // retain heartbeat ordering until their next explicitly captured event.
                if (entry.descriptor.mode == ThermalSourceMode.POWER_SOURCE && (!entry.enabled || entry.powerW == 0)) selected.remove();
            }
        }
        return windows.toArray(Window[]::new);
    }
    public boolean capturedThrough(Long chunk, long tick) {
        for (Entry entry : scheduled.values()) if ((chunk == null || sourceChunk(entry) == chunk) && entry.sealedThrough < tick) return false;
        for (Entry entry : retired) if ((chunk == null || sourceChunk(entry) == chunk) && !entry.terminalSealed) return false;
        return true;
    }

    /**
     * Main freezes production once. The coordinator later subtracts the recording
     * totals at the same point as its accepted fields; no physical fence is involved.
     */
    public Capture capture(long tick) {
        if (captureOpen) return null;
        captureOpen = true;
        var values = new ArrayList<Produced>(active.size() + retired.size());
        try {
        for (var entry : active.values()) {
            entry.closeTail(tick);
            values.add(new Produced(entry.descriptor, entry.totals, entry.totals.produced.clone(), entry.tailTick, entry.backing.retain()));
        }
        for (var entry : retired)
            values.add(new Produced(entry.descriptor, entry.totals, entry.totals.produced.clone(), entry.tailTick, entry.backing.retain()));
        } catch (RuntimeException | Error failure) {
            for (var value : values) value.backing.close();
            captureOpen = false;
            throw failure;
        }
        latestTick = Math.max(latestTick, tick);
        return new Capture(this, tick, values);
    }

    private record Produced(Descriptor descriptor, PortTotals totals, double[] joules, long through, ThermalMemoryBudget.BackingLease backing) { }
    private static final class Export {
        final PortTotals totals;
        final int channel;
        final byte kind;
        final SpatialHeatInput.Target target;
        final double acceptedJ, exportedJ, baselinePendingJ;
        final long acceptedThrough;
        double producedJ;
        Export(PortTotals totals, int channel, double producedJ, byte kind, SpatialHeatInput.Target target) {
            this.totals = totals; this.channel = channel; this.producedJ = producedJ;
            acceptedJ = totals.accepted[channel]; exportedJ = totals.exported[channel];
            baselinePendingJ = remaining(producedJ); acceptedThrough = totals.acceptedThrough;
            this.kind = kind; this.target = target;
        }
        double remaining(double produced) {
            double value = produced - acceptedJ - exportedJ;
            return (channel & 1) == 0 ? Math.max(0, value) : Math.min(0, value);
        }
    }
    public record PortDestination(SpatialHeatInput.Target target, boolean declaredLoss) {
        public static PortDestination thermal(SpatialHeatInput.Target target) { return new PortDestination(target, false); }
        public static PortDestination loss(boolean declared) { return new PortDestination(null, declared); }
    }
    @FunctionalInterface
    public interface PortTarget { PortDestination resolve(Descriptor descriptor, int port); }

    public static final class Capture implements AutoCloseable {
        public final long tick;
        private java.util.List<Produced> values;
        private final SpatialSourceTimeline owner;
        private boolean closed;
        private Capture(SpatialSourceTimeline owner, long tick, java.util.List<Produced> values) {
            this.owner = owner; this.tick = tick; this.values = values;
        }
        public int maximumPendingRecords(long chunk, PortTarget resolver) {
            int count = 0;
            for (var value : values) for (int port = 0; port < value.descriptor.ports.length; port++) {
                var target = resolver.resolve(value.descriptor, port).target();
                if (target != null
                        && net.minecraft.world.level.ChunkPos.asLong(BlockPos.getX(target.position()) >> 4,
                        BlockPos.getZ(target.position()) >> 4) == chunk) count += 2;
            }
            return count;
        }
        public Pending pending(long chunk, PortTarget resolver, SpatialHeatStore.RecordConsumer consumer) {
            var exports = new ArrayList<Export>();
            for (var value : values) for (int port = 0; port < value.descriptor.ports.length; port++) {
                var destination = resolver.resolve(value.descriptor, port);
                var target = destination.target();
                long ownerChunk = target == null ? net.minecraft.world.level.ChunkPos.asLong(value.descriptor.x >> 4, value.descriptor.z >> 4)
                        : net.minecraft.world.level.ChunkPos.asLong(BlockPos.getX(target.position()) >> 4, BlockPos.getZ(target.position()) >> 4);
                if (ownerChunk != chunk) continue;
                for (int sign = 0; sign < 2; sign++) {
                    int channel = port * 2 + sign;
                    double pending = value.joules[channel] - value.totals.accepted[channel] - value.totals.exported[channel];
                    exports.add(new Export(value.totals, channel, value.joules[channel],
                            (byte) (target != null ? 0 : destination.declaredLoss ? 1 : 2), target));
                    if (sign == 0 ? pending <= 0 : pending >= 0) continue;
                    long from = Math.min(value.through, value.totals.acceptedThrough);
                    if (target != null) consumer.accept(target, from, value.through, pending);
                }
            }
            return new Pending(exports);
        }
        /** Main owns produced totals. Target/source changes invalidate the native capture before this call. */
        public void collectAdditionalThrough(long through, Pending pending, SpatialHeatStore.RecordConsumer consumer) {
            through = Math.max(tick, through);
            if (through == tick) return;
            for (var entry : owner.active.values()) entry.closeTail(through);
            owner.latestTick = Math.max(owner.latestTick, through);
            for (var port : pending.exports) {
                double current = port.totals.produced[port.channel];
                double delta = port.remaining(current) - port.baselinePendingJ;
                port.producedJ = current;
                if (port.target != null && delta != 0) consumer.accept(port.target, Math.min(through, Math.max(tick, port.acceptedThrough)), through, delta);
            }
        }
        @Override public void close() {
            if (closed) return;
            closed = true;
            for (var value : values) value.backing.close();
            values = java.util.List.of(); owner.captureOpen = false;
        }
    }

    /** Source tails are exported only on actual unload. A normal save only reads them. */
    public static final class Pending {
        private final java.util.List<Export> exports;
        private boolean exported;
        private double thermalJ, declaredJ, degradedJ;
        private Pending(java.util.List<Export> exports) { this.exports = exports; }
        public double export() {
            settle(); return thermalJ;
        }
        public double declaredLossJ() { settle(); return declaredJ; }
        public double degradedLossJ() { settle(); return degradedJ; }
        private void settle() {
            if (exported) return;
            exported = true;
            for (var value : exports) {
                double remaining = value.producedJ - value.totals.accepted[value.channel] - value.totals.exported[value.channel];
                if ((value.channel & 1) == 0 ? remaining <= 0 : remaining >= 0) continue;
                value.totals.exported[value.channel] += remaining;
                if (value.kind == 0) thermalJ += remaining;
                else if (value.kind == 1) declaredJ += remaining;
                else degradedJ += remaining;
            }
        }
    }
    private static long sourceChunk(Entry entry) {
        return net.minecraft.world.level.ChunkPos.asLong(entry.descriptor.x >> 4, entry.descriptor.z >> 4);
    }
    @Override public void close() {
        for (Entry entry : active.values()) entry.backing.close();
        for (Entry entry : retired) entry.backing.close();
        active.clear(); scheduled.clear(); retired.clear(); indexCharge.close();
    }
}
