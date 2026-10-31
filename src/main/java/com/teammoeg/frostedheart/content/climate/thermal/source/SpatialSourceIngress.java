/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayDeque;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Source transport and binding have independent progress. This owner holds only
 * producer windows and scheduling references; all accepted J belongs to the store.
 * A Page's geometry, receipt or retirement never owns one of these queues.
 */
public final class SpatialSourceIngress implements AutoCloseable {
    public record Progress(int generation, long tick, long sequence, int ordinal) { }
    private record Offered(SpatialSourceTimeline.Window window, long sequence, int ordinal) { }
    @FunctionalInterface public interface Controls {
        void changed(SourceInputWindow window, LongSet positions);
    }
    @FunctionalInterface public interface PendingControl {
        void visit(SpatialSourceTimeline.Descriptor descriptor, long tick, long sequence);
    }
    public static long metadataBytes(int ports) {
        // Identity/queues/index entries, plus bounded scratch for 27 mixing bricks per physical port.
        return 1024L + 64L * 27 * ports;
    }
    private static final class Source {
        final long id;
        final ThermalMemoryBudget.Reservation metadata;
        final ArrayDeque<Offered> offered = new ArrayDeque<>();
        final ArrayDeque<SourceInputWindow> accepted = new ArrayDeque<>();
        Progress acceptedThrough, boundThrough;
        long bindingWait = -1;
        boolean terminal, queued;
        long boundaryVisit;
        int indexedGeneration = -1;
        SpatialSourceTimeline.Descriptor producer;
        SpatialSourceTimeline.ControlTail controlTail;
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet indexedSections = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        Source(long id, ThermalMemoryBudget.Reservation metadata) { this.id = id; this.metadata = metadata; }
    }

    private final Object transport = new Object();
    private final Long2ObjectLinkedOpenHashMap<Source> sources = new Long2ObjectLinkedOpenHashMap<>();
    private final ArrayDeque<Source> scheduled = new ArrayDeque<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<java.util.Set<Source>> bySection =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private final ThermalSourceLedger ledger;
    private final SpatialHeatStore store;
    private final WorkerPhysicalSourceBindings bindings;
    private final RegionalAirRuntime regions;
    private final ThermalCellArena arena;
    private final AirLoadTable airLoads;
    private final CutLoadBuffer output = new CutLoadBuffer();
    private final ToLongFunction<SpatialSourceTimeline.Descriptor> bindingLimit;
    private final Predicate<SpatialSourceTimeline.Descriptor> frozen;
    private final Controls controls;
    private long revision, controlRevision, dependencyRevision, boundaryVisit;

    public SpatialSourceIngress(ThermalSourceLedger ledger,
            WorkerPhysicalSourceBindings bindings, RegionalAirRuntime regions, ThermalCellArena arena,
            AirLoadTable airLoads, ToLongFunction<SpatialSourceTimeline.Descriptor> bindingLimit,
            Predicate<SpatialSourceTimeline.Descriptor> frozen, Controls controls) {
        this.ledger = ledger; this.bindings = bindings; this.regions = regions;
        this.store = regions.inputStore(); this.arena = arena; this.airLoads = airLoads;
        this.bindingLimit = bindingLimit; this.frozen = frozen; this.controls = controls;
    }

    /** Main transfers a prepaid reference. No binding or numerical work runs under this lock. */
    public void enqueue(SpatialSourceTimeline.Window window, long sequence, int ordinal) {
        synchronized (transport) {
            var source = sources.get(window.descriptor.sourceId());
            if (source == null) {
                var metadata = window.takeIngressCredit();
                if (metadata == null) throw new IllegalStateException("Source ingress credit already transferred");
                source = new Source(window.descriptor.sourceId(), metadata); sources.put(source.id, source);
            }
            source.offered.addLast(new Offered(window, sequence, ordinal));
            source.producer = window.descriptor; source.controlTail = window.controlTail;
            window.announced();
            if (source.indexedGeneration != window.descriptor.generation()) {
                source.indexedGeneration = window.descriptor.generation();
                index(source, window.descriptor.section());
                for (int port = 0; port < window.descriptor.ports().length; port++) {
                    var origin = bindings.originalPort(window.descriptor, port);
                    if (origin != null) {
                        index(source, section(origin.position()));
                        var face = com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace.fromOrdinal(origin.face() - 6);
                        index(source, section(BlockPos.asLong(BlockPos.getX(origin.position()) + face.stepX(),
                                BlockPos.getY(origin.position()) + face.stepY(), BlockPos.getZ(origin.position()) + face.stepZ())));
                    }
                }
            }
            if (!source.queued) { source.queued = true; scheduled.addLast(source); }
        }
    }
    private static long section(long position) {
        return SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
    }
    private void index(Source source, long section) {
        if (source.indexedSections.add(section)) bySection.computeIfAbsent(section,
                ignored -> new java.util.LinkedHashSet<>()).add(source);
    }
    private void retire(Source source) {
        sources.remove(source.id); source.metadata.close();
        for (long section : source.indexedSections) {
            var indexed = bySection.get(section); indexed.remove(source);
            if (indexed.isEmpty()) bySection.remove(section);
        }
    }

    public void dependenciesChanged() { revision++; dependencyRevision++; }
    public long revision() { return revision; }
    public long controlRevision() { return controlRevision; }
    /** Control visibility does not depend on acceptance capacity, binding, or old-generation retirement. */
    public void forEachPendingControl(PendingControl visitor) {
        synchronized (transport) {
            for (var source : sources.values()) {
                long tail = source.controlTail == null ? Long.MAX_VALUE : source.controlTail.firstTick();
                if (tail != Long.MAX_VALUE) visitor.visit(source.producer, tail, Long.MAX_VALUE);
                for (var window : source.accepted) {
                    long tick = window.pendingControlTick();
                    if (tick != Long.MAX_VALUE) visitor.visit(window.descriptor, tick, window.sequence);
                }
                for (var offered : source.offered) {
                    var window = offered.window;
                    if (window.controlsChanged) visitor.visit(window.descriptor,
                            window.registration ? window.fromTick : window.boundaryTick, offered.sequence);
                }
            }
        }
    }
    private boolean acceptanceReady(Source source) {
        var next = source.offered.peekFirst();
        return next != null && source.accepted.size() < SourceInputWindow.MAXIMUM_OWNED_WINDOWS
                && !frozen.test(next.window.descriptor);
    }
    private boolean bindingReady(Source source) {
        var next = source.accepted.peekFirst();
        return next != null && source.bindingWait != dependencyRevision && !frozen.test(next.descriptor);
    }
    private boolean ready(Source source) {
        return acceptanceReady(source) || bindingReady(source);
    }
    public boolean hasReadyWork() {
        synchronized (transport) { for (var source : scheduled) if (ready(source)) return true; }
        return false;
    }
    public boolean hasWork() { synchronized (transport) { return !scheduled.isEmpty(); } }

    /** Rotate identities after every bounded operation, including an identity that cannot bind. */
    public void advance(long deadline) {
        int remaining = 32;
        do {
            Source source = null;
            synchronized (transport) {
                int count = scheduled.size();
                while (count-- > 0) {
                    var next = scheduled.removeFirst();
                    if (ready(next)) { source = next; break; }
                    scheduled.addLast(next);
                }
            }
            if (source == null) return;
            // Bind the oldest accepted window first, so a busy source returns its own slot.
            if (bindingReady(source)) bind(source);
            else accept(source);
            synchronized (transport) {
                if (source.offered.isEmpty() && source.accepted.isEmpty()) {
                    source.queued = false;
                    if (source.terminal) retire(source);
                } else scheduled.addLast(source);
            }
        } while (--remaining > 0 && System.nanoTime() < deadline);
    }

    private void accept(Source source) {
        Offered offered;
        synchronized (transport) { offered = source.offered.peekFirst(); }
        if (offered == null) return;
        var producer = offered.window;
        var origins = new SpatialHeatInput.Target[producer.descriptor.ports().length];
        for (int port = 0; port < origins.length; port++) origins[port] = bindings.originalPort(producer.descriptor, port);
        regions.prepareSourceWindow(producer, origins);
        var owned = store.acceptSourceWindow(producer, offered.sequence, offered.ordinal, origins, ledger);
        synchronized (transport) { source.offered.removeFirst(); source.accepted.addLast(owned); }
        source.acceptedThrough = new Progress(owned.descriptor.generation(), owned.toTick, owned.sequence, owned.ordinal);
        source.bindingWait = -1; source.terminal = false;
        revision++;
        observe(source, true);
    }

    private void bind(Source source) {
        var window = source.accepted.peekFirst();
        long previousControl = window.pendingControlTick();
        long through = Math.min(window.toTick, bindingLimit.applyAsLong(window.descriptor));
        // A membership fence publishes its mixing control before binding the later suffix.
        through = Math.min(through, window.nextControlTick());
        if (!window.canBind(through)) { source.bindingWait = dependencyRevision; observe(source, false); return; }
        int records = Math.multiplyExact(window.size + 1, window.portCount());
        regions.prepareSpatialInput(records);
        output.clear(); output.reserve(records); ledger.prepareSourcePrefix(window);
        long mixing = bindings.mixingRevision();
        store.bindSourcePrefix(window, through, ledger, bindings, output);
        if (previousControl != window.pendingControlTick()) controlRevision++;
        var heat = SpatialHeatInput.takeWorldIdentity(output.freeze(airLoads, arena));
        synchronized (transport) { for (int index = 0; index < heat.size(); index++) index(source, section(heat.target(index).position())); }
        regions.receiveSpatialHeat(heat);
        synchronized (transport) { regions.sourceBound(source.id, source.indexedSections); }
        source.boundThrough = new Progress(window.descriptor.generation(), through, window.sequence, window.ordinal);
        var changes = bindings.mixingChangesAfter(mixing);
        if (!changes.isEmpty()) controls.changed(window, changes);
        if (window.complete()) {
            source.accepted.removeFirst();
            source.terminal = window.unload;
            bindings.reclaimInputIdentities();
        }
        source.bindingWait = -1;
        revision++;
        observe(source, false);
    }

    private void observe(Source source, boolean accepted) {
        var observer = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (observer == null) return;
        int queued; synchronized (transport) { queued = source.offered.size(); }
        double unboundJ = 0;
        for (var window : source.accepted) unboundJ += window.pendingEnergyJ();
        observer.sourceProgress(source.id, source.acceptedThrough, source.boundThrough, accepted, source.accepted.size(), queued, unboundJ,
                source.bindingWait == dependencyRevision);
    }

    /** Inactive sources may heartbeat only after all their prior J and due controls are bound. */
    public long pendingThrough(long sourceId) {
        synchronized (transport) {
            var source = sources.get(sourceId); if (source == null) return Long.MAX_VALUE;
            long tick = source.accepted.isEmpty() ? Long.MAX_VALUE : source.accepted.peekFirst().through();
            if (!source.offered.isEmpty()) tick = Math.min(tick, source.offered.peekFirst().window.fromTick);
            return tick;
        }
    }
    /** New identities can feed an existing region before their first binding creates its source index. */
    public long pendingFor(LongSet sections, java.util.function.LongPredicate knownSource,
            java.util.function.LongPredicate ownsTarget) {
        synchronized (transport) {
            long visit = ++boundaryVisit, through = Long.MAX_VALUE;
            for (long section : sections) {
                var indexed = bySection.get(section); if (indexed == null) continue;
                for (var source : indexed) {
                    if (source.boundaryVisit == visit || knownSource.test(source.id)) continue;
                    source.boundaryVisit = visit;
                    var owned = source.accepted.peekFirst(); var offered = source.offered.peekFirst();
                    var descriptor = owned != null ? owned.descriptor : offered == null ? null : offered.window.descriptor;
                    if (descriptor == null) continue;
                    for (int port = 0; port < descriptor.ports().length; port++) {
                        var destination = bindings.checkpointTarget(descriptor, port).target();
                        if (destination != null && ownsTarget.test(destination.position())) {
                            through = Math.min(through, pendingThrough(source.id)); break;
                        }
                    }
                }
            }
            return through;
        }
    }

    private static boolean due(SpatialSourceTimeline.Window window, long tick, long sequence) {
        if (window.fromTick < tick) return true;
        if (window.fromTick > tick || sequence < 0) return false;
        if (window.registration || window.controlsChanged && window.boundaryTick <= tick) return true;
        for (int i = 0; i < window.size; i++) {
            var span = window.span(i); if (span.fromTick() == span.toTick() && span.toTick() <= tick) return true;
        }
        return false;
    }
    public String firstBlocker(RegionalContinuationBoundary boundary, long tick, long sequence) {
        synchronized (transport) {
            var source = blockingSource(boundary, tick, sequence);
            return source == null ? null : blocker(source, tick, sequence);
        }
    }
    private Source blockingSource(RegionalContinuationBoundary boundary, long tick, long sequence) {
        long visit = ++boundaryVisit;
        for (long section : boundary.sections()) {
            var indexed = bySection.get(section); if (indexed == null) continue;
            for (var source : indexed) {
                if (source.boundaryVisit == visit) continue;
                source.boundaryVisit = visit;
                if (blocked(source, tick, sequence)) return source;
            }
        }
        return null;
    }
    private static boolean pending(SourceInputWindow window, long tick, long sequence) {
        boolean due = window.fromTick < tick || window.fromTick == tick && window.sequence <= sequence && window.hasEventThrough(tick);
        return due && (window.first() || window.through() < Math.min(tick, window.toTick));
    }
    private static boolean blocked(Source source, long tick, long sequence) {
        if (!source.accepted.isEmpty())
            for (var window : source.accepted) if (pending(window, tick, sequence)) return true;
        if (!source.offered.isEmpty())
            for (var offered : source.offered) if (due(offered.window, tick, offered.sequence <= sequence ? sequence : -1)) return true;
        return false;
    }
    private String blocker(Source source, long tick, long sequence) {
        for (var window : source.accepted) {
            if (pending(window, tick, sequence))
                return "source=" + source.id + ":generation=" + window.descriptor.generation()
                        + ":sequence=" + window.sequence + ":ordinal=" + window.ordinal
                        + ":accepted=" + source.acceptedThrough + ":bound=" + source.boundThrough
                        + ":needed=" + tick + ":limit=" + bindingLimit.applyAsLong(window.descriptor)
                        + ":frozen=" + frozen.test(window.descriptor);
        }
        for (var offered : source.offered) if (due(offered.window, tick, offered.sequence <= sequence ? sequence : -1))
            return "source=" + source.id + ":generation=" + offered.window.descriptor.generation()
                    + ":sequence=" + offered.sequence + ":ordinal=" + offered.ordinal + ":wait=acceptance:needed=" + tick;
        return null;
    }
    public boolean boundThrough(RegionalContinuationBoundary boundary, long tick, long sequence) {
        synchronized (transport) { return blockingSource(boundary, tick, sequence) == null; }
    }
    public boolean hasOfferedInChunk(long chunk) {
        synchronized (transport) {
            for (var entry : bySection.long2ObjectEntrySet()) {
                long section = entry.getLongKey();
                if (net.minecraft.world.level.ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section)) != chunk) continue;
                for (var source : entry.getValue()) if (!source.offered.isEmpty()) return true;
            }
            return false;
        }
    }
    public void recordBacking(AirBackingMeter meter) {
        output.recordBacking(meter);
        synchronized (transport) { meter.container(sources, sources.size(), 32); meter.container(scheduled, scheduled.size(), 8); }
    }
    @Override public void close() {
        synchronized (transport) {
            for (var source : sources.values()) {
                for (var offered : source.offered) offered.window.close();
                source.metadata.close(); source.offered.clear(); source.accepted.clear();
            }
            sources.clear(); scheduled.clear(); bySection.clear();
        }
    }
}
