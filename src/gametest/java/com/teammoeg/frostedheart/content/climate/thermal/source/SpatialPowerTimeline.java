/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test-only power/replay driver for isolated core checks. The production engine
 * uses its original SpatialSourceTimeline and SpatialHeatStore instead.
 * No producer history is retained: changing power first integrates its old
 * interval into the spatial bank. A replay ends and releases its own receiver.
 */
public final class SpatialPowerTimeline implements AutoCloseable {
    private static final class Source {
        final int generation;
        final long[] receivers;
        final double[] shares;
        final long[] chunks;
        final ThermalMemoryBudget.Reservation credit;
        long through;
        double powerW;
        boolean enabled;
        Source(int generation, long[] receivers, double[] shares, long[] chunks, double powerW, boolean enabled, long tick,
                ThermalMemoryBudget.Reservation credit) {
            this.generation = generation; this.receivers = receivers; this.shares = shares;
            this.chunks = chunks;
            this.powerW = powerW; this.enabled = enabled; through = tick; this.credit = credit;
        }
    }

    private static final class Replay {
        final long receiver, from, to;
        final long chunk;
        final double joules;
        final ThermalMemoryBudget.Reservation credit;
        long through;
        double remaining;
        Replay(long receiver, long chunk, long from, long to, double joules, ThermalMemoryBudget.Reservation credit) {
            this.receiver = receiver; this.from = through = from; this.to = to;
            this.joules = remaining = joules; this.credit = credit;
            this.chunk = chunk;
        }
    }

    private final ThermalMemoryBudget budget;
    private final SpatialEnergyBanks banks;
    private final int maximumSources, maximumReplays;
    private Long2ObjectOpenHashMap<Source> sources = new Long2ObjectOpenHashMap<>();
    private final ArrayList<Replay> replays = new ArrayList<>();
    private final Long2ObjectOpenHashMap<ReferenceOpenHashSet<Source>> sourcesByChunk = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ReferenceOpenHashSet<Replay>> replaysByChunk = new Long2ObjectOpenHashMap<>();
    private ThermalMemoryBudget.Reservation directory;
    private int sourceCapacity, replayCapacity, maximumObservedPorts;
    private long throughTick;
    private double producedJ, replayedJ, declaredLossJ, unavailableReceiverJ;
    private boolean closed;

    public SpatialPowerTimeline(ThermalMemoryBudget budget, SpatialEnergyBanks banks,
            int maximumSources, int maximumReplays, long initialTick) {
        if (maximumSources <= 0 || maximumReplays <= 0 || initialTick < 0) throw new IllegalArgumentException("Invalid spatial power capacity or time base");
        this.budget = budget; this.banks = banks; this.maximumSources = maximumSources;
        this.maximumReplays = maximumReplays; throughTick = initialTick;
        directory = budget.tryReserve(directoryBytes(0, 0, 0));
        if (directory == null) throw new ThermalMemoryBudget.AllocationRefused(directoryBytes(0, 0, 0));
    }

    private static long directoryBytes(int sources, int replays, int ports) { return 1024L + 64L * sources * (1 + ports) + 128L * replays; }

    private boolean prepareDirectory(int sourceCount, int replayCount, int ports) {
        if (sourceCount > maximumSources || replayCount > maximumReplays) return false;
        int nextSources = sourceCount > sourceCapacity ? Math.min(maximumSources, Math.max(sourceCount, Math.max(16, sourceCapacity * 2))) : sourceCapacity;
        int nextReplays = replayCount > replayCapacity ? Math.min(maximumReplays, Math.max(replayCount, Math.max(16, replayCapacity * 2))) : replayCapacity;
        int nextPorts = Math.max(maximumObservedPorts, ports);
        if (nextSources == sourceCapacity && nextReplays == replayCapacity && nextPorts == maximumObservedPorts) return true;
        var credit = budget.tryReserve(directoryBytes(nextSources, nextReplays, nextPorts));
        if (credit == null) return false;
        try {
            if (nextSources != sourceCapacity) {
                var replacement = new Long2ObjectOpenHashMap<Source>(nextSources); replacement.putAll(sources); sources = replacement;
            }
            if (nextReplays != replayCapacity) replays.ensureCapacity(nextReplays);
        } catch (RuntimeException | Error failure) { credit.close(); throw failure; }
        sourceCapacity = nextSources; replayCapacity = nextReplays; maximumObservedPorts = nextPorts; directory.close(); directory = credit;
        return true;
    }

    /** Null targets are declared environmental/radiation loss, not pending Air input. */
    public boolean tryRegister(long id, int generation, SpatialEnergyBanks.Address[] targets, double[] shares,
            double powerW, boolean enabled, long tick) {
        if (closed || sources.containsKey(id)) return false;
        return prepareSource(id, generation, targets, shares, powerW, enabled, tick, false);
    }

    /** Prepares every new port before retiring the old binding or changing its power. */
    public boolean tryRetarget(long id, int generation, SpatialEnergyBanks.Address[] targets, double[] shares, long tick) {
        var old = sources.get(id);
        return !closed && old != null && old.generation == generation
                && prepareSource(id, generation, targets, shares, old.powerW, old.enabled, tick, true);
    }

    private boolean prepareSource(long id, int generation, SpatialEnergyBanks.Address[] targets, double[] shares,
            double powerW, boolean enabled, long tick, boolean replacing) {
        if (targets.length == 0 || targets.length > 16 || targets.length != shares.length || !Double.isFinite(powerW) || tick < throughTick)
            throw new IllegalArgumentException("Invalid declared source ports, power or time");
        double sum = 0;
        for (double share : shares) {
            if (!Double.isFinite(share) || share < 0) throw new IllegalArgumentException("Invalid source share");
            sum += share;
        }
        if (Math.abs(sum - 1) > 1e-12) throw new IllegalArgumentException("Source shares must account for all declared power");
        if (!prepareDirectory(sources.size() + (replacing ? 0 : 1), replays.size(), targets.length)) return false;
        var credit = budget.tryReserve(256L + 256L * targets.length);
        if (credit == null) return false;
        long[] handles = new long[targets.length]; Arrays.fill(handles, SpatialEnergyBanks.NO_RECEIVER);
        boolean transferred = false;
        try {
            for (int port = 0; port < handles.length; port++) if (targets[port] != null) {
                handles[port] = banks.retain(targets[port]);
                if (handles[port] == SpatialEnergyBanks.NO_RECEIVER) return false;
            }
            long[] chunks = new long[targets.length]; int chunkCount = 0;
            for (var target : targets) if (target != null) {
                long chunk = target.chunk(); boolean found = false;
                for (int index = 0; index < chunkCount; index++) if (chunks[index] == chunk) { found = true; break; }
                if (!found) chunks[chunkCount++] = chunk;
            }
            var next = new Source(generation, handles, shares.clone(), Arrays.copyOf(chunks, chunkCount), powerW, enabled, tick, credit);
            var previous = sources.get(id);
            if (previous != null) { integrate(previous, tick); release(previous); }
            sources.put(id, next);
            for (long chunk : next.chunks) sourcesByChunk.computeIfAbsent(chunk, ignored -> new ReferenceOpenHashSet<>(2)).add(next);
            transferred = true;
            return true;
        } finally {
            if (!transferred) {
                for (long handle : handles) if (handle != SpatialEnergyBanks.NO_RECEIVER) banks.release(handle);
                credit.close();
            }
        }
    }

    public boolean update(long id, int generation, double powerW, boolean enabled, long tick) {
        var source = sources.get(id);
        if (closed || source == null || source.generation != generation) return false;
        if (!Double.isFinite(powerW)) throw new IllegalArgumentException("Non-finite declared power");
        integrate(source, tick); source.powerW = powerW; source.enabled = enabled;
        return true;
    }

    public boolean remove(long id, int generation, long tick) {
        var source = sources.get(id);
        if (closed || source == null || source.generation != generation) return false;
        integrate(source, tick); sources.remove(id); release(source);
        return true;
    }

    private void integrate(Source source, long tick) {
        if (tick < source.through) throw new IllegalArgumentException("A source event moved backwards");
        double energy = source.enabled ? source.powerW * ((tick - source.through) / 20.0) : 0;
        if (energy != 0) {
            producedJ += energy;
            double remaining = energy;
            for (int port = 0; port < source.receivers.length; port++) {
                double share = port == source.receivers.length - 1 ? remaining : energy * source.shares[port]; remaining -= share;
                if (source.receivers[port] == SpatialEnergyBanks.NO_RECEIVER) declaredLossJ += share;
                else if (!banks.add(source.receivers[port], share)) unavailableReceiverJ += share;
            }
        }
        source.through = tick;
    }

    /** Geometry/lifecycle resolution precedes this call; refusal accepts no archive energy. */
    public boolean tryReplay(SpatialEnergyBanks.Address target, long from, long to, double joules) {
        if (closed) return false;
        if (from != throughTick || to < from || !Double.isFinite(joules)) throw new IllegalArgumentException("Cold replay must start at the integrated main-thread cut");
        if (joules == 0) return true;
        if (!prepareDirectory(sources.size(), replays.size() + 1, 0)) return false;
        var credit = budget.tryReserve(256);
        if (credit == null) return false;
        long handle = banks.retain(target);
        if (handle == SpatialEnergyBanks.NO_RECEIVER) { credit.close(); return false; }
        if (from == to) {
            if (banks.add(handle, joules)) replayedJ += joules; else unavailableReceiverJ += joules;
            banks.release(handle); credit.close();
        } else {
            var replay = new Replay(handle, target.chunk(), from, to, joules, credit);
            replays.add(replay); replaysByChunk.computeIfAbsent(replay.chunk, ignored -> new ReferenceOpenHashSet<>(2)).add(replay);
        }
        return true;
    }

    public void integrate(long tick) {
        if (closed) return;
        if (tick < throughTick) throw new IllegalArgumentException("Spatial power time moved backwards");
        for (var source : sources.values()) integrate(source, tick);
        for (int index = 0; index < replays.size();) {
            var replay = replays.get(index);
            integrate(replay, tick);
            if (replay.through == replay.to) {
                banks.release(replay.receiver); replay.credit.close();
                var indexed = replaysByChunk.get(replay.chunk); indexed.remove(replay);
                if (indexed.isEmpty()) replaysByChunk.remove(replay.chunk);
                int last = replays.size() - 1; replays.set(index, replays.get(last)); replays.remove(last);
            } else index++;
        }
        throughTick = tick;
    }

    private void integrate(Replay replay, long tick) {
        long end = Math.min(tick, replay.to);
        if (end <= replay.through) return;
        double amount = end == replay.to ? replay.remaining : replay.joules * ((end - replay.through) / (double) (replay.to - replay.from));
        if (banks.add(replay.receiver, amount)) replayedJ += amount; else unavailableReceiverJ += amount;
        replay.remaining -= amount; replay.through = end;
    }

    /** Saving one Chunk never walks producers or archives in unrelated Chunks. */
    public void integrateChunk(long chunk, long tick) {
        if (closed) return;
        var indexedSources = sourcesByChunk.get(chunk);
        if (indexedSources != null) for (var source : indexedSources) integrate(source, tick);
        var indexedReplays = replaysByChunk.get(chunk);
        if (indexedReplays != null) for (var replay : indexedReplays) integrate(replay, tick);
    }

    public SpatialEnergyBanks.Batch seal(long tick) { integrate(tick); return banks.seal(tick); }
    public int sourceCount() { return sources.size(); }
    public int replayCount() { return replays.size(); }
    public double producedJ() { return producedJ; }
    public double replayedJ() { return replayedJ; }
    public double declaredLossJ() { return declaredLossJ; }
    public double unavailableReceiverJ() { return unavailableReceiverJ; }

    /** Capture includes cold energy that is still scheduled beyond saveTick. */
    @FunctionalInterface public interface ReplayConsumer {
        void accept(SpatialEnergyBanks.Address address, long fromTick, long toTick, double joules);
    }

    public int replayCountInChunk(long chunk) {
        var indexed = replaysByChunk.get(chunk);
        return indexed == null ? 0 : indexed.size();
    }

    public void pendingReplaysInChunk(long chunk, ReplayConsumer consumer) {
        // The bank owns already integrated J. Replay addresses are borrowed from
        // generation-checked receivers and remain valid while this row owns one.
        var indexed = replaysByChunk.get(chunk);
        if (indexed == null) return;
        for (var replay : indexed) {
            var address = banks.address(replay.receiver);
            if (address != null && address.chunk() == chunk && replay.remaining != 0)
                consumer.accept(address, replay.through, replay.to, replay.remaining);
        }
    }

    private void release(Source source) {
        for (long chunk : source.chunks) {
            var indexed = sourcesByChunk.get(chunk); indexed.remove(source);
            if (indexed.isEmpty()) sourcesByChunk.remove(chunk);
        }
        for (long handle : source.receivers) if (handle != SpatialEnergyBanks.NO_RECEIVER) banks.release(handle);
        source.credit.close();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        for (var source : sources.values()) release(source);
        for (var replay : replays) { banks.release(replay.receiver); replay.credit.close(); }
        sources.clear(); sources.trim(0); replays.clear(); replays.trimToSize();
        sourcesByChunk.clear(); sourcesByChunk.trim(0); replaysByChunk.clear(); replaysByChunk.trim(0); directory.close();
    }
}
