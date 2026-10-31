/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.LongConsumer;
import java.util.function.ObjDoubleConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * Accepted heat is owned by a world target. Region ownership is only a routing
 * index; rebuilding a numerical region never copies or retires this payload.
 * All mutation is on the dimension coordinator.
 */
public final class SpatialHeatStore {
    private static final int EXACT_RECORDS = 64;
    private record Span(long from, long to, double joules) { }
    private static final class Summary {
        long from = Long.MAX_VALUE, to = Long.MIN_VALUE;
        double joules;
        void add(Span value) { from = Math.min(from, value.from); to = Math.max(to, value.to); joules += value.joules; }
        boolean empty() { return from == Long.MAX_VALUE; }
        Span take() { Span result = new Span(from, to, joules); from = Long.MAX_VALUE; to = Long.MIN_VALUE; joules = 0; return result; }
    }
    private static final class Stream {
        SpatialHeatInput.Target target;
        final ArrayDeque<Span> exact = new ArrayDeque<>();
        final Summary positive = new Summary(), negative = new Summary();
        final ArrayDeque<Span> prefix = new ArrayDeque<>();
        Owner owner;
        Stream previous, next;
        Claim claim;
        ArrayList<Span> claimed;
        double pendingJ;
        double claimedJ;
        int claimedRecords;
        boolean routingQueued;
        long latestTick;
        Stream(SpatialHeatInput.Target target) { this.target = target; }
        void append(Span span) {
            latestTick = Math.max(latestTick, span.to);
            while (exact.size() >= EXACT_RECORDS || !exact.isEmpty() && exact.peekFirst().to < latestTick - 40) {
                Span old = exact.removeFirst(); (old.joules >= 0 ? positive : negative).add(old);
            }
            insert(exact, span); pendingJ += span.joules;
        }
        private static void insert(ArrayDeque<Span> values, Span next) {
            if (values.isEmpty() || compare(values.peekLast(), next) <= 0) { values.addLast(next); return; }
            int remaining = values.size(); boolean inserted = false;
            while (remaining-- > 0) {
                var old = values.removeFirst();
                if (!inserted && compare(next, old) < 0) { values.addLast(next); inserted = true; }
                values.addLast(old);
            }
            if (!inserted) values.addLast(next);
        }
        private static int compare(Span a, Span b) {
            int first = Long.compare(a.from, b.from);
            return first == 0 ? Long.compare(a.to, b.to) : first;
        }
        boolean empty() { return prefix.isEmpty() && exact.isEmpty() && positive.empty() && negative.empty() && claim == null; }
        int recordCount() { return prefix.size() + exact.size() + claimedRecords
                + (positive.empty() ? 0 : 1) + (negative.empty() ? 0 : 1); }
        long firstTick() {
            long first = Long.MAX_VALUE;
            for (var span : prefix) first = Math.min(first, span.from);
            for (var span : exact) first = Math.min(first, span.from);
            if (!positive.empty()) first = Math.min(first, positive.from);
            if (!negative.empty()) first = Math.min(first, negative.from);
            return first;
        }
    }
    private static final class Owner {
        final long id;
        Stream first, last;
        Claim preparing;
        int targets, records;
        Owner(long id) { this.id = id; }
    }

    private final LinkedHashMap<SpatialHeatInput.Target, Stream> streams = new LinkedHashMap<>();
    private final Long2ObjectOpenHashMap<Owner> owners = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<java.util.LinkedHashSet<Stream>> chunks = new Long2ObjectOpenHashMap<>();
    private final ArrayDeque<Claim> retiring = new ArrayDeque<>();
    private final ArrayDeque<Stream> routingReady = new ArrayDeque<>();
    private final java.util.Set<SourceInputWindow> sourceWindows = new java.util.LinkedHashSet<>();
    private final Long2ObjectOpenHashMap<java.util.Set<SourceInputWindow>> sourceChunks = new Long2ObjectOpenHashMap<>();
    private long sourceBytes;
    private int routingHighWater;
    private LongConsumer changed = ignored -> { };
    private LongConsumer invalidatedClaim = ignored -> { };
    private double pendingJ;
    private int streamHighWater, recordHighWater, liveRecords;

    public void onPendingChanged(LongConsumer listener) { changed = listener; }
    public void onClaimInvalidated(LongConsumer listener) { invalidatedClaim = listener; }
    private static long chunk(SpatialHeatInput.Target target) {
        return ChunkPos.asLong(BlockPos.getX(target.position()) >> 4, BlockPos.getZ(target.position()) >> 4);
    }

    /** The caller has reserved the complete bounded window before this ownership transfer. */
    public SourceInputWindow acceptSourceWindow(SpatialSourceTimeline.Window producer, long sequence, int ordinal,
            SpatialHeatInput.Target[] origins, ThermalSourceLedger ledger) {
        var owned = new SourceInputWindow(producer, sequence, ordinal, origins);
        sourceWindows.add(owned);
        for (var target : origins) if (target != null)
            sourceChunks.computeIfAbsent(chunk(target), ignored -> new java.util.LinkedHashSet<>()).add(owned);
        sourceBytes += owned.retainedBytes();
        owned.accept(producer, ledger);
        pendingJ += owned.pendingEnergyJ();
        for (var target : origins) if (target != null) changed.accept(chunk(target));
        producer.close();
        return owned;
    }

    /** A binding prefix moves J out of its original source window, without producing it again. */
    public void bindSourcePrefix(SourceInputWindow window, long through, ThermalSourceLedger ledger,
            ThermalSourceLedger.BindingUpdates bindings, CutLoadBuffer output) {
        double previous = window.pendingEnergyJ();
        ledger.bindSpatialPrefix(window, through, bindings, output);
        pendingJ -= previous - window.pendingEnergyJ();
        for (int port = 0; port < window.portCount(); port++) {
            var target = window.origin(port);
            if (target != null) changed.accept(chunk(target));
        }
        if (window.complete()) {
            sourceWindows.remove(window); sourceBytes -= window.retainedBytes();
            for (int port = 0; port < window.portCount(); port++) {
                var target = window.origin(port); if (target == null) continue;
                var indexed = sourceChunks.get(chunk(target));
                if (indexed != null) { indexed.remove(window); if (indexed.isEmpty()) sourceChunks.remove(chunk(target)); }
            }
        }
    }

    public void receive(long ownerId, SpatialHeatInput.Target target, long from, long to, double joules) {
        if (joules == 0) return;
        var stream = streams.get(target);
        if (stream == null) {
            stream = new Stream(target); streams.put(target, stream);
            chunks.computeIfAbsent(chunk(target), ignored -> new java.util.LinkedHashSet<>()).add(stream);
        }
        if (stream.claim != null && !stream.claim.closed && (from < stream.claim.through || from == to && from == stream.claim.through))
            invalidatedClaim.accept(stream.owner.id);
        assign(stream, ownerId);
        int previous = stream.recordCount();
        stream.append(new Span(from, to, joules)); pendingJ += joules;
        recordsChanged(stream, previous);
        streamHighWater = Math.max(streamHighWater, streams.size());
        if (ownerId == 0) enqueueRouting(stream);
        changed.accept(chunk(target));
    }

    public void receiveUnrouted(SpatialHeatInput input) {
        for (int i = 0; i < input.size(); i++) {
            var target = input.target(i);
            var existing = streams.get(target);
            // A resolved world identity does not become unresolved when a late prefix arrives.
            // Its numerical owner cancels an overlapping private claim before accepting that prefix.
            long owner = existing == null || existing.owner == null ? 0 : existing.owner.id;
            receive(owner, target, input.fromTick(i), input.toTick(i), input.energyJ(i));
        }
    }

    private void recordsChanged(Stream stream, int previous) {
        int change = stream.recordCount() - previous;
        if (stream.owner != null) stream.owner.records += change;
        liveRecords += change; recordHighWater = Math.max(recordHighWater, liveRecords);
    }

    public void remapOwner(long ownerId, java.util.function.ToLongFunction<SpatialHeatInput.Target> ownerAt,
            java.util.function.UnaryOperator<SpatialHeatInput.Target> unresolvedIdentity) {
        var owner = owners.get(ownerId);
        for (var stream = owner == null ? null : owner.first; stream != null;) {
            var next = stream.next;
            long nextOwner = ownerAt.applyAsLong(stream.target);
            assign(stream, nextOwner);
            if (nextOwner == 0) changeTarget(stream, unresolvedIdentity.apply(stream.target));
            stream = next;
        }
    }

    private void changeTarget(Stream stream, SpatialHeatInput.Target target) {
        var key = stream.target;
        if (key.equals(target)) return;
        var existing = streams.get(target);
        if (existing == null) {
            streams.remove(key); stream.target = target; streams.put(target, stream);
            if (chunk(key) != chunk(target)) {
                var previous = chunks.get(chunk(key)); previous.remove(stream);
                if (previous.isEmpty()) chunks.remove(chunk(key));
                chunks.computeIfAbsent(chunk(target), ignored -> new java.util.LinkedHashSet<>()).add(stream);
            }
            enqueueRouting(stream);
        } else if (existing != stream) {
            if (existing.claim != null && !existing.claim.closed && stream.firstTick() <= existing.claim.through)
                invalidatedClaim.accept(existing.owner.id);
            int before = existing.recordCount();
            if (!stream.positive.empty()) existing.append(new Span(stream.positive.from, stream.positive.to, stream.positive.joules));
            if (!stream.negative.empty()) existing.append(new Span(stream.negative.from, stream.negative.to, stream.negative.joules));
            for (var span : stream.prefix) existing.append(span);
            for (var span : stream.exact) existing.append(span);
            recordsChanged(existing, before);
            liveRecords -= stream.recordCount(); unlink(stream); streams.remove(key);
            var indexed = chunks.get(chunk(key)); indexed.remove(stream);
            if (indexed.isEmpty()) chunks.remove(chunk(key));
            enqueueRouting(existing);
        }
        changed.accept(chunk(key)); changed.accept(chunk(target));
    }

    private void assign(Stream stream, long ownerId) {
        if (stream.owner != null && stream.owner.id == ownerId) return;
        if (stream.claim != null) {
            if (!stream.claim.closed) throw new IllegalStateException("Cannot reroute an executing heat prefix");
            stream.claim.releaseStream(stream);
        }
        unlink(stream);
        var owner = owners.computeIfAbsent(ownerId, Owner::new);
        stream.owner = owner; stream.previous = owner.last;
        if (owner.last == null) owner.first = stream; else owner.last.next = stream;
        owner.last = stream; owner.targets++; owner.records += stream.recordCount();
        if (ownerId == 0) enqueueRouting(stream);
    }

    private void unlink(Stream stream) {
        var owner = stream.owner;
        if (owner == null) return;
        if (stream.previous == null) owner.first = stream.next; else stream.previous.next = stream.next;
        if (stream.next == null) owner.last = stream.previous; else stream.next.previous = stream.previous;
        owner.records -= stream.recordCount();
        if (--owner.targets == 0) owners.remove(owner.id);
        stream.owner = null; stream.previous = stream.next = null;
    }

    public int targets(long ownerId) { var owner = owners.get(ownerId); return owner == null ? 0 : owner.targets; }
    public int records(long ownerId) { var owner = owners.get(ownerId); return owner == null ? 0 : owner.records; }
    public double pendingEnergyJ() { return pendingJ; }
    public int targetCount() { return streams.size() + sourceWindows.size(); }
    public long retainedBytes() { return 1024L + sourceBytes + 128L * streamHighWater + 1920L * streams.size() + 64L * liveRecords + 16L * routingHighWater; }
    public long firstPendingInChunk(long chunk) {
        var entries = chunks.get(chunk); long first = Long.MAX_VALUE;
        var unbound = sourceChunks.get(chunk);
        if (unbound != null) for (var window : unbound) first = Math.min(first, window.through());
        if (entries != null) for (var stream : entries) {
            first = Math.min(first, stream.firstTick());
            if (stream.claim != null && !stream.claim.accepted) first = Math.min(first, stream.claim.from);
        }
        return first;
    }

    public boolean hasCleanup() { return !retiring.isEmpty(); }

    /** Numerical acceptance changes one watermark. Payload retirement uses the next work slices. */
    public void advanceCleanup(int maximumTargets) {
        while (maximumTargets-- > 0 && !retiring.isEmpty()) {
            var claim = retiring.peekFirst();
            if (claim.cleanupTarget == claim.touched.size()) {
                retiring.removeFirst(); if (claim.retention != null) claim.retention.close(); continue;
            }
            var stream = claim.touched.get(claim.cleanupTarget++);
            claim.releaseStream(stream);
            releaseEmpty(stream);
        }
        if (streams.isEmpty() && sourceWindows.isEmpty()) pendingJ = 0;
    }

    public void collectChunk(long chunk, RecordConsumer consumer) {
        var unbound = sourceChunks.get(chunk);
        if (unbound != null) for (var window : unbound) window.collectChunk(chunk, consumer);
        var entries = chunks.get(chunk);
        if (entries == null) return;
        for (var stream : entries) collectStream(stream, consumer);
    }
    private static void collectStream(Stream stream, RecordConsumer consumer) {
            long from = Long.MAX_VALUE, to = Long.MIN_VALUE; double positive = 0, negative = 0;
            for (var span : stream.prefix) {
                from = Math.min(from, span.from); to = Math.max(to, span.to);
                if (span.joules >= 0) positive += span.joules; else negative += span.joules;
            }
            if (!stream.positive.empty()) {
                from = Math.min(from, stream.positive.from); to = Math.max(to, stream.positive.to); positive += stream.positive.joules;
            }
            if (!stream.negative.empty()) {
                from = Math.min(from, stream.negative.from); to = Math.max(to, stream.negative.to); negative += stream.negative.joules;
            }
            for (var span : stream.exact) {
                from = Math.min(from, span.from); to = Math.max(to, span.to);
                if (span.joules >= 0) positive += span.joules; else negative += span.joules;
            }
            if (stream.claim != null && !stream.claim.accepted)
                for (var span : stream.claimed) {
                    from = Math.min(from, span.from); to = Math.max(to, span.to);
                    if (span.joules >= 0) positive += span.joules; else negative += span.joules;
                }
            if (positive != 0) consumer.accept(stream.target, from, to, positive);
            if (negative != 0) consumer.accept(stream.target, from, to, negative);
    }
    /** Routing only moves prefixes between indexes of this same owner. Amounts are joules. */
    public interface Receiver {
        long availableThrough(SpatialHeatInput.Target target);
        boolean accept(SpatialHeatInput.Target target, long from, long to, double joules);
        default void afterAttempt(SpatialHeatInput.Target target) { }
    }
    private void enqueueRouting(Stream stream) {
        if (stream.owner == null || stream.owner.id != 0 || stream.routingQueued) return;
        stream.routingQueued = true; routingReady.addLast(stream);
        routingHighWater = Math.max(routingHighWater, routingReady.size());
    }
    public boolean hasReadyRouting() { return !routingReady.isEmpty(); }
    public boolean containsUnrouted(SpatialHeatInput.Target target) {
        var stream = streams.get(target); return stream != null && stream.owner != null && stream.owner.id == 0;
    }
    public void retry(SpatialHeatInput.Target target) {
        var stream = streams.get(target); if (stream != null) enqueueRouting(stream);
    }
    public void retryChunk(long chunk) {
        var entries = chunks.get(chunk); if (entries != null) for (var stream : entries) enqueueRouting(stream);
    }
    public void geometryChanged() { geometryChanged(ignored -> { }); }
    public void geometryChanged(java.util.function.Consumer<SpatialHeatInput.Target> classify) {
        var owner = owners.get(0);
        for (var stream = owner == null ? null : owner.first; stream != null; stream = stream.next) {
            classify.accept(stream.target); enqueueRouting(stream);
        }
    }
    private static boolean due(Span span, long through) {
        return span.from < through || span.from == through && span.from == span.to;
    }

    /** Call-local values reused by this store's coordinator; never retains a Span. */
    private static final class DueScan implements java.util.function.Consumer<Span> {
        long through;
        boolean found;

        boolean any(ArrayDeque<Span> spans, long tick) {
            var first = spans.peekFirst();
            if (first == null) return false;
            if (due(first, tick)) return true;
            through = tick; found = false;
            // Cancellation can leave a future head before a due record.
            spans.forEach(this);
            return found;
        }

        @Override public void accept(Span span) { if (!found && due(span, through)) found = true; }
    }

    private final DueScan dueScan = new DueScan();

    public boolean pendingThrough(SpatialHeatInput.Target target, long tick) {
        var stream = streams.get(target);
        if (stream == null || stream.owner == null || stream.owner.id != 0) return false;
        if (!stream.positive.empty() && (stream.positive.from < tick || stream.positive.from == tick && stream.positive.to == tick)) return true;
        if (!stream.negative.empty() && (stream.negative.from < tick || stream.negative.from == tick && stream.negative.to == tick)) return true;
        return dueScan.any(stream.prefix, tick) || dueScan.any(stream.exact, tick);
    }
    public String describePending(SpatialHeatInput.Target target, long tick) {
        var stream = streams.get(target);
        if (stream == null) return "absent";
        return "due=" + pendingThrough(target, tick) + ":owner=" + (stream.owner == null ? -1 : stream.owner.id)
                + ":queued=" + stream.routingQueued + ":first=" + stream.exact.peekFirst()
                + ":prefix=" + stream.prefix.peekFirst() + ":records=" + stream.recordCount();
    }
    public void route(Receiver receiver, int maximumRecords) {
        int targets = Math.min(routingReady.size(), maximumRecords);
        while (targets-- > 0 && maximumRecords > 0) {
            var stream = routingReady.removeFirst(); stream.routingQueued = false;
            if (streams.get(stream.target) != stream || stream.owner == null || stream.owner.id != 0) continue;
            var key = stream.target;
            if (stream.claim != null) {
                if (!stream.claim.closed) { receiver.afterAttempt(key); continue; }
                stream.claim.releaseStream(stream);
            }
            long through = receiver.availableThrough(key);
            if (through < 0) { receiver.afterAttempt(key); continue; }
            for (int sign = 0; sign < 2 && maximumRecords > 0 && stream.owner != null && stream.owner.id == 0; sign++) {
                var summary = sign == 0 ? stream.positive : stream.negative;
                if (summary.empty()) continue;
                var span = new Span(summary.from, summary.to, summary.joules);
                if (!due(span, through)) continue;
                int before = stream.recordCount(); summary.take(); recordsChanged(stream, before);
                if (!routeSpan(receiver, stream, span, through)) {
                    before = stream.recordCount(); summary.add(span); recordsChanged(stream, before); break;
                }
                maximumRecords--;
            }
            for (int part = 0; part < 2 && maximumRecords > 0 && stream.owner != null && stream.owner.id == 0; part++) {
                var values = part == 0 ? stream.prefix : stream.exact;
                // Prefixes restored by cancellation are bounded but need not precede a late exact input.
                int remaining = values.size();
                while (remaining-- > 0 && maximumRecords > 0 && stream.owner != null && stream.owner.id == 0) {
                    var span = values.removeFirst();
                    if (!due(span, through)) { values.addLast(span); continue; }
                    int before = stream.recordCount() + 1; recordsChanged(stream, before);
                    if (!routeSpan(receiver, stream, span, through)) {
                        before = stream.recordCount(); Stream.insert(values, span); recordsChanged(stream, before); break;
                    }
                    maximumRecords--;
                }
            }
            if (stream.empty()) releaseEmpty(stream);
            else if (maximumRecords == 0) enqueueRouting(stream);
            changed.accept(chunk(key)); receiver.afterAttempt(key);
        }
        if (streams.isEmpty() && sourceWindows.isEmpty()) pendingJ = 0;
    }

    /** The popped prefix remains owned by this coordinator operation until its destination accepts it. */
    private boolean routeSpan(Receiver receiver, Stream stream, Span span, long through) {
        long end = Math.min(span.to, through);
        double joules = end == span.to ? span.joules : span.joules * ((double) (end - span.from) / (span.to - span.from));
        if (!receiver.accept(stream.target, span.from, end, joules)) return false;
        stream.pendingJ -= joules; pendingJ -= joules;
        if (end < span.to) {
            int before = stream.recordCount();
            Stream.insert(stream.prefix, new Span(end, span.to, span.joules - joules)); recordsChanged(stream, before);
        }
        return true;
    }

    /** Unresolved Page-lifetime records keep their J while their address index changes. */
    public final class Retarget {
        private final long section, generation;
        private final java.util.List<Stream> selected;
        private int cursor;
        public Retarget(long section, long generation) {
            this.section = section; this.generation = generation;
            var entries = chunks.get(ChunkPos.asLong(net.minecraft.core.SectionPos.x(section), net.minecraft.core.SectionPos.z(section)));
            selected = entries == null ? java.util.List.of() : new ArrayList<>(entries);
        }
        public boolean run(int maximum) {
            int end = Math.min(selected.size(), cursor + maximum);
            while (cursor < end) {
                var stream = selected.get(cursor++); var key = stream.target;
                if (streams.get(key) != stream || stream.owner == null || stream.owner.id != 0) continue;
                long block = key.position();
                if (net.minecraft.core.SectionPos.asLong(BlockPos.getX(block) >> 4, BlockPos.getY(block) >> 4, BlockPos.getZ(block) >> 4) != section
                        || key.minimumPageGeneration() > generation || key.generation() != 0 && key.generation() != generation) continue;
                var target = new SpatialHeatInput.Target(block, key.face(), -1, 0, 0, 0, 0, 0, key.materialStateId(), generation + 1);
                if (stream.claim != null) {
                    if (!stream.claim.closed) { cursor--; return false; }
                    stream.claim.releaseStream(stream);
                }
                changeTarget(stream, target);
            }
            return cursor == selected.size();
        }
    }

    public int checkpointRecordsInChunk(long chunk) {
        var entries = chunks.get(chunk);
        int count = entries == null ? 0 : 2 * entries.size();
        var unbound = sourceChunks.get(chunk);
        if (unbound != null) for (var window : unbound)
            // Saves copy every retained span at its original outlet.
            count += window.size * window.portCount();
        return count;
    }
    public int targetsInChunk(long chunk) {
        var entries = chunks.get(chunk); var unbound = sourceChunks.get(chunk);
        int count = entries == null ? 0 : entries.size();
        if (unbound != null) for (var window : unbound) count += window.portCount();
        return count;
    }
    public int unboundWindowsInChunk(long chunk) {
        var windows = sourceChunks.get(chunk); int count = 0;
        if (windows != null) for (var window : windows) if (window.hasHeat()) count++;
        return count;
    }

    @FunctionalInterface
    public interface RecordConsumer { void accept(SpatialHeatInput.Target target, long from, long to, double joules); }
    public Claim claim(long ownerId, long from, long through) { return claim(ownerId, from, through, null); }
    public Claim claim(long ownerId, long from, long through, RecordConsumer consumer) {
        return new Claim(owners.get(ownerId), from, through, consumer);
    }

    /** Freezes at most the existing owner's tail. Later input can only enter its unfrozen suffix. */
    public final class Claim implements AutoCloseable {
        private final long from, through;
        private final Owner preparingOwner;
        private Stream cursor, stop;
        private final ArrayList<Stream> touched = new ArrayList<>();
        private final SpatialHeatInput.Builder builder = new SpatialHeatInput.Builder();
        private SpatialHeatInput input;
        private boolean closed, accepted;
        private double joules;
        private int cleanupTarget;
        private int allocatedRecords;
        private ThermalMemoryBudget.Reservation retention;
        private RecordConsumer consumer;
        private final boolean streamed;
        private boolean prepared;
        private Claim(Owner owner, long from, long through, RecordConsumer consumer) {
            this.from = from; this.through = through; preparingOwner = owner;
            this.consumer = consumer;
            streamed = consumer != null;
            if (owner != null) {
                if (owner.preparing != null && !owner.preparing.closed && owner.preparing.input == null)
                    throw new IllegalStateException("Owner already has an input preparation");
                owner.preparing = this;
            }
            cursor = owner == null ? null : owner.first; stop = owner == null ? null : owner.last;
        }

        public boolean prepare(long deadline) {
            if (prepared) return true;
            do {
                if (cursor == null) {
                    input = builder.freeze();
                    prepared = true;
                    consumer = null;
                    if (preparingOwner != null && preparingOwner.preparing == this) preparingOwner.preparing = null;
                    return true;
                }
                var current = cursor;
                cursor = current == stop ? null : current.next;
                if (current.claim != null && current.claim != this) {
                    if (!current.claim.closed) throw new IllegalStateException("Heat target already claimed");
                    current.claim.releaseStream(current);
                }
                int previousRecords = current.recordCount();
                current.claim = this; current.claimed = new ArrayList<>(); touched.add(current);
                freeze(current, current.prefix);
                if (!current.positive.empty()) {
                    Span span = current.positive.take(); if (!freeze(current, span)) current.prefix.addLast(span);
                }
                if (!current.negative.empty()) {
                    Span span = current.negative.take(); if (!freeze(current, span)) current.prefix.addLast(span);
                }
                int count = current.exact.size();
                while (count-- > 0) {
                    Span span = current.exact.removeFirst();
                    if (!freeze(current, span)) current.exact.addLast(span);
                }
                recordsChanged(current, previousRecords);
            } while (System.nanoTime() < deadline);
            return false;
        }

        private void freeze(Stream stream, ArrayDeque<Span> prefix) {
            int count = prefix.size();
            while (count-- > 0) { var span = prefix.removeFirst(); if (!freeze(stream, span)) prefix.addLast(span); }
        }

        private boolean freeze(Stream stream, Span span) {
            if (span.from > through || span.from != span.to && span.from >= through) return false;
            long end = Math.min(span.to, through);
            double joules = end == span.to ? span.joules : span.joules * ((double) (end - span.from) / (span.to - span.from));
            var portion = end == span.to ? span : new Span(span.from, end, joules);
            stream.claimed.add(portion);
            allocatedRecords++;
            stream.claimedRecords++;
            stream.claimedJ += joules; this.joules += joules;
            // A late compacted span belongs to this prefix, never to a time already published.
            long begin = Math.max(from, span.from), finish = Math.max(begin, end);
            if (consumer == null) builder.appendAggregate(stream.target, begin, finish, joules);
            else consumer.accept(stream.target, begin, finish, joules);
            if (end < span.to) stream.prefix.addLast(new Span(end, span.to, span.joules - joules));
            return true;
        }

        public SpatialHeatInput input() {
            if (input == null) throw new IllegalStateException("Heat prefix preparation is incomplete");
            return input;
        }
        /** The job can release its numerical scratch while this bounded retirement still owns records. */
        public void retainCleanup(ThermalMemoryBudget.Reservation jobScratch, boolean prepaidWorkspace) {
            input = null; builder.release();
            // The minimum consumer never builds an O(records) payload. Its held
            // span references and retirement metadata stay in the store's charge.
            if (prepaidWorkspace || streamed || retention != null || touched.isEmpty() || jobScratch == null) return;
            long bytes = 512L + 128L * touched.size() + 48L * allocatedRecords;
            retention = jobScratch.split(bytes);
        }

        /** Called only as part of the matching Accepted publication. */
        public void accepted() {
            if (closed) throw new IllegalStateException("Heat prefix already retired");
            closed = accepted = true;
            consumer = null;
            pendingJ -= joules;
            retiring.addLast(this);
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            consumer = null;
            if (preparingOwner != null && preparingOwner.preparing == this) preparingOwner.preparing = null;
            retiring.addLast(this);
        }

        private void releaseStream(Stream stream) {
            if (stream.claim != this) return;
            int previousRecords = stream.recordCount();
            if (accepted) stream.pendingJ -= stream.claimedJ;
            else {
                // One bounded target has at most two summaries and 64 recent records.
                for (int index = stream.claimed.size() - 1; index >= 0; index--)
                    stream.prefix.addFirst(stream.claimed.get(index));
                while (stream.prefix.size() > EXACT_RECORDS) {
                    var old = stream.prefix.removeFirst(); (old.joules >= 0 ? stream.positive : stream.negative).add(old);
                }
            }
            stream.claim = null; stream.claimed = null; stream.claimedJ = 0; stream.claimedRecords = 0;
            recordsChanged(stream, previousRecords);
            changed.accept(chunk(stream.target));
        }
    }

    private void releaseEmpty(Stream stream) {
        if (stream.owner != null && stream.owner.preparing != null && !stream.owner.preparing.closed
                && stream.owner.preparing.input == null) return;
        if (!stream.empty() || !streams.remove(stream.target, stream)) return;
        unlink(stream);
        var indexed = chunks.get(chunk(stream.target));
        indexed.remove(stream); if (indexed.isEmpty()) chunks.remove(chunk(stream.target));
    }

    public void retireChunk(long chunk, ObjDoubleConsumer<SpatialHeatInput.Target> settle) {
        var unbound = sourceChunks.get(chunk);
        if (unbound != null) for (var window : unbound) pendingJ -= window.exportChunk(chunk, settle);
        changed.accept(chunk);
        var indexed = chunks.get(chunk);
        if (indexed == null) return;
        var iterator = indexed.iterator();
        while (iterator.hasNext()) {
            var stream = iterator.next();
            if (stream.claim != null) {
                if (!stream.claim.closed) throw new IllegalStateException("Export waits for its real regional helper");
                stream.claim.releaseStream(stream);
            }
            settle.accept(stream.target, stream.pendingJ); pendingJ -= stream.pendingJ;
            liveRecords -= stream.recordCount(); unlink(stream); streams.remove(stream.target); iterator.remove();
        }
        chunks.remove(chunk); changed.accept(chunk);
        if (streams.isEmpty() && sourceWindows.isEmpty()) pendingJ = 0;
    }

    public void clear() {
        for (var claim : retiring) if (claim.retention != null) claim.retention.close();
        streams.clear(); owners.clear(); chunks.clear(); retiring.clear(); routingReady.clear(); pendingJ = 0; liveRecords = 0;
        sourceWindows.clear(); sourceChunks.clear(); sourceBytes = 0;
    }
}
