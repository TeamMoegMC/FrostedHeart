/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Historical producer-bank fixture for isolated numerical/persistence comparisons; excluded from production. */
public final class SpatialEnergyBanks implements AutoCloseable {
    public static final long NO_RECEIVER = -1L;
    public record Address(long position, int materialGeneration, int face) {
        public Address(long position, int materialGeneration) { this(position, materialGeneration, -1); }
        public long chunk() { return ChunkPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4); }
    }
    @FunctionalInterface public interface PendingConsumer { void accept(Address address, double joules); }
    @FunctionalInterface public interface TimedPendingConsumer { void accept(Address address, double joules, long oldestIntervalTick); }

    public static final class Batch implements ThermalHeatStep {
        public final long sequence, fromTick, toTick;
        public final int size;
        private final SpatialEnergyBanks owner;
        private final Address[] addresses;
        private final double[] joules, acceptedPending, trialPending;
        private final long[] acceptedPendingSince, trialPendingSince;
        private Batch(SpatialEnergyBanks owner, long sequence, long fromTick, long toTick, int size, Address[] addresses, double[] joules,
                double[] acceptedPending, double[] trialPending, long[] acceptedPendingSince, long[] trialPendingSince) {
            this.owner = owner; this.sequence = sequence; this.fromTick = fromTick; this.toTick = toTick; this.size = size;
            this.addresses = addresses; this.joules = joules; this.acceptedPending = acceptedPending; this.trialPending = trialPending;
            this.acceptedPendingSince = acceptedPendingSince; this.trialPendingSince = trialPendingSince;
        }
        @Override public long sequence() { return sequence; }
        @Override public long fromTick() { return fromTick; }
        @Override public long toTick() { return toTick; }
        @Override public int size() { return size; }
        @Override public long position(int slot) { return addresses[slot].position(); }
        @Override public int materialGeneration(int slot) { return addresses[slot].materialGeneration(); }
        @Override public int face(int slot) { return addresses[slot].face(); }
        @Override public void commit() { owner.commit(sequence); }
        @Override public void cancel() { owner.cancel(sequence); }
        public Address address(int slot) { return addresses[slot]; }
        public double joules(int slot) { return joules[slot]; }
        public double pendingJoules(int slot) { return acceptedPending[slot]; }
        public long pendingFromTick(int slot) { return acceptedPendingSince[slot]; }
        public void defer(int slot, double energyJ) {
            if (energyJ == 0) return;
            long since = acceptedPending[slot] != 0 ? Math.min(fromTick, acceptedPendingSince[slot]) : fromTick;
            trialPending[slot] += energyJ;
            trialPendingSince[slot] = trialPending[slot] == 0 ? Long.MAX_VALUE : Math.min(trialPendingSince[slot], since);
        }
    }

    private final ThermalMemoryBudget budget;
    private final int maximumReceivers;
    private Object2IntOpenHashMap<Address> receivers;
    private Long2ObjectOpenHashMap<IntOpenHashSet> byChunk;
    private final java.util.Set<Long> retainedChunks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private ThermalMemoryBudget.Reservation storage;
    private Address[] addresses;
    private int[] references, generations, nextFree;
    private double[][] joules;
    private double[] acceptedPending, trialPending;
    private long[] acceptedPendingSince, trialPendingSince;
    private int highWater, freeHead = -1, nextGeneration, filling, live;
    private long throughTick, sequence;
    private Batch sealed;
    private boolean closed;

    public SpatialEnergyBanks(ThermalMemoryBudget budget, int initialCapacity, int maximumReceivers, long initialTick) {
        if (initialCapacity <= 0 || maximumReceivers < initialCapacity || initialTick < 0) throw new IllegalArgumentException("Invalid spatial input capacity or baseline");
        this.budget = budget; this.maximumReceivers = maximumReceivers; throughTick = initialTick;
        storage = budget.tryReserve(storageBytes(initialCapacity));
        if (storage == null) throw new ThermalMemoryBudget.AllocationRefused(storageBytes(initialCapacity));
        try { allocate(initialCapacity); } catch (RuntimeException | Error failure) { storage.close(); throw failure; }
    }
    private static long storageBytes(int count) {
        return 1024L + 384L * count + AirBackingMeter.arrayPayloadBytes(count, 8)
                + 3 * AirBackingMeter.arrayPayloadBytes(count, 4) + 6 * AirBackingMeter.arrayPayloadBytes(count, 8);
    }
    private void allocate(int count) {
        addresses = new Address[count]; references = new int[count]; generations = new int[count];
        nextFree = new int[count]; Arrays.fill(nextFree, -1);
        joules = new double[][] {new double[count], new double[count]};
        acceptedPending = new double[count]; trialPending = new double[count];
        acceptedPendingSince = new long[count]; trialPendingSince = new long[count];
        Arrays.fill(acceptedPendingSince, Long.MAX_VALUE); Arrays.fill(trialPendingSince, Long.MAX_VALUE);
        receivers = new Object2IntOpenHashMap<>(count); receivers.defaultReturnValue(-1);
        byChunk = new Long2ObjectOpenHashMap<>(count);
    }
    public long retain(Address address) {
        if (closed) return NO_RECEIVER;
        int existing = receivers.getInt(address);
        if (existing >= 0) { references[existing]++; return handle(existing); }
        int slot;
        if (sealed != null) { if (highWater == addresses.length) return NO_RECEIVER; slot = highWater++; }
        else {
            if (freeHead < 0 && highWater == addresses.length && !grow()) return NO_RECEIVER;
            if (freeHead >= 0) { slot = freeHead; freeHead = nextFree[slot]; nextFree[slot] = -1; } else slot = highWater++;
        }
        addresses[slot] = address; references[slot] = 1;
        generations[slot] = nextGeneration = Math.incrementExact(nextGeneration); receivers.put(address, slot);
        byChunk.computeIfAbsent(address.chunk(), ignored -> new IntOpenHashSet()).add(slot); retainedChunks.add(address.chunk()); live++;
        return handle(slot);
    }
    public void release(long receiver) {
        int slot = slot(receiver);
        if (slot < 0 || references[slot] == 0) return;
        references[slot]--;
        if (sealed == null && references[slot] == 0 && joules[0][slot] == 0 && joules[1][slot] == 0 && acceptedPending[slot] == 0) retire(slot);
    }
    public boolean add(long receiver, double energyJ) {
        if (!Double.isFinite(energyJ)) throw new IllegalArgumentException("Non-finite source input");
        int slot = slot(receiver);
        if (slot < 0 || references[slot] == 0) return false;
        joules[filling][slot] += energyJ; return true;
    }
    public Batch seal(long toTick) {
        if (closed || sealed != null) return null;
        if (toTick < throughTick) throw new IllegalArgumentException("Spatial input time moved backwards");
        Arrays.fill(trialPending, 0, highWater, 0); Arrays.fill(trialPendingSince, 0, highWater, Long.MAX_VALUE);
        sealed = new Batch(this, ++sequence, throughTick, toTick, highWater, addresses, joules[filling], acceptedPending, trialPending, acceptedPendingSince, trialPendingSince);
        filling ^= 1; throughTick = toTick; return sealed;
    }
    public void commit(long acceptedSequence) {
        requireSealed(acceptedSequence); Arrays.fill(sealed.joules, 0, highWater, 0);
        var previous = acceptedPending; acceptedPending = trialPending; trialPending = previous;
        var previousSince = acceptedPendingSince; acceptedPendingSince = trialPendingSince; trialPendingSince = previousSince;
        sealed = null; collectRetired();
    }
    public void cancel(long cancelledSequence) {
        requireSealed(cancelledSequence);
        for (int slot = 0; slot < highWater; slot++) { joules[filling][slot] += sealed.joules[slot]; sealed.joules[slot] = 0; }
        throughTick = sealed.fromTick; sealed = null; collectRetired();
    }
    public void discardPreparedCopy() {
        if (sealed != null) throw new IllegalStateException("Cancel the borrowed numerical bank before discarding its copy");
        Arrays.fill(joules[0], 0, highWater, 0); Arrays.fill(joules[1], 0, highWater, 0); collectRetired();
    }
    public void pendingInChunk(long chunk, PendingConsumer consumer) {
        var indexed = byChunk.get(chunk); if (indexed == null) return;
        for (int slot : indexed) { double pending = acceptedPending[slot] + joules[0][slot] + joules[1][slot]; if (pending != 0) consumer.accept(addresses[slot], pending); }
    }
    public void pendingTimedInChunk(long chunk, TimedPendingConsumer consumer) {
        var indexed = byChunk.get(chunk); if (indexed == null) return;
        for (int slot : indexed) { double pending = acceptedPending[slot] + joules[0][slot] + joules[1][slot]; if (pending != 0) consumer.accept(addresses[slot], pending, oldestInterval(slot)); }
    }
    public void acceptedPendingInChunk(long chunk, TimedPendingConsumer consumer) {
        var indexed = byChunk.get(chunk); if (indexed == null) return;
        for (int slot : indexed) if (acceptedPending[slot] != 0) consumer.accept(addresses[slot], acceptedPending[slot], acceptedPendingSince[slot]);
    }
    public double exportAcceptedPendingInChunk(long chunk) {
        if (sealed != null) throw new IllegalStateException("Cancel the numerical claim before exporting Accepted debt");
        var indexed = byChunk.get(chunk); if (indexed == null) return 0;
        double total = 0;
        for (int slot : indexed) { total += acceptedPending[slot]; acceptedPending[slot] = 0; acceptedPendingSince[slot] = Long.MAX_VALUE; }
        collectRetired(); return total;
    }
    public long oldestUncommittedTick() {
        long oldest = Long.MAX_VALUE;
        for (int slot = 0; slot < highWater; slot++) if (addresses[slot] != null) oldest = Math.min(oldest, oldestInterval(slot));
        return oldest;
    }
    private long oldestInterval(int slot) {
        long oldest = acceptedPending[slot] == 0 ? Long.MAX_VALUE : acceptedPendingSince[slot];
        if (joules[filling][slot] != 0) oldest = Math.min(oldest, throughTick);
        if (sealed != null && sealed.joules[slot] != 0) oldest = Math.min(oldest, sealed.fromTick);
        return oldest;
    }
    public int receiverCount() { return live; }
    public Address address(long receiver) { int slot = slot(receiver); return slot < 0 ? null : addresses[slot]; }
    public int receiverCountInChunk(long chunk) { var indexed = byChunk.get(chunk); return indexed == null ? 0 : indexed.size(); }
    public boolean hasReceiversInChunk(long chunk) { return retainedChunks.contains(chunk); }
    public boolean hasSealedBank() { return sealed != null; }
    public long reservedBytes() { return storage.bytes(); }
    public double acceptedPendingJoules() { double total = 0; for (int slot = 0; slot < highWater; slot++) total += acceptedPending[slot]; return total; }
    private int slot(long handle) {
        if (closed || handle == NO_RECEIVER) return -1;
        int slot = (int) handle;
        return slot >= 0 && slot < highWater && addresses[slot] != null && generations[slot] == (int) (handle >>> 32) ? slot : -1;
    }
    private long handle(int slot) { return (long) generations[slot] << 32 | Integer.toUnsignedLong(slot); }
    private void collectRetired() {
        for (int slot = 0; slot < highWater; slot++) if (addresses[slot] != null && references[slot] == 0 && joules[0][slot] == 0 && joules[1][slot] == 0 && acceptedPending[slot] == 0) retire(slot);
    }
    private void retire(int slot) {
        var address = addresses[slot]; receivers.removeInt(address);
        var indexed = byChunk.get(address.chunk()); indexed.remove(slot);
        if (indexed.isEmpty()) { byChunk.remove(address.chunk()); retainedChunks.remove(address.chunk()); }
        addresses[slot] = null; acceptedPendingSince[slot] = trialPendingSince[slot] = Long.MAX_VALUE;
        nextFree[slot] = freeHead; freeHead = slot; live--;
    }
    private boolean grow() {
        int old = addresses.length;
        if (old == maximumReceivers) return false;
        int count = Math.min(maximumReceivers, Math.max(old + 1, old * 2));
        var replacement = budget.tryReserve(storageBytes(count)); if (replacement == null) return false;
        try {
            addresses = Arrays.copyOf(addresses, count); references = Arrays.copyOf(references, count); generations = Arrays.copyOf(generations, count);
            nextFree = Arrays.copyOf(nextFree, count); Arrays.fill(nextFree, old, count, -1);
            joules[0] = Arrays.copyOf(joules[0], count); joules[1] = Arrays.copyOf(joules[1], count);
            acceptedPending = Arrays.copyOf(acceptedPending, count); trialPending = Arrays.copyOf(trialPending, count);
            acceptedPendingSince = Arrays.copyOf(acceptedPendingSince, count); trialPendingSince = Arrays.copyOf(trialPendingSince, count);
            Arrays.fill(acceptedPendingSince, old, count, Long.MAX_VALUE); Arrays.fill(trialPendingSince, old, count, Long.MAX_VALUE);
            var nextReceivers = new Object2IntOpenHashMap<Address>(count); nextReceivers.defaultReturnValue(-1); nextReceivers.putAll(receivers);
            var nextByChunk = new Long2ObjectOpenHashMap<IntOpenHashSet>(count); nextByChunk.putAll(byChunk); receivers = nextReceivers; byChunk = nextByChunk;
        } catch (RuntimeException | Error failure) { replacement.close(); throw failure; }
        storage.close(); storage = replacement; return true;
    }
    private void requireSealed(long expected) {
        if (sealed == null || sealed.sequence != expected) throw new IllegalStateException("Spatial input does not match the field commit");
    }
    @Override public void close() {
        if (closed) return;
        closed = true; sealed = null; receivers.clear(); byChunk.clear(); retainedChunks.clear(); receivers.trim(0); byChunk.trim(0);
        addresses = null; references = generations = nextFree = null; joules = null; acceptedPending = trialPending = null; acceptedPendingSince = trialPendingSince = null; storage.close();
    }
}
