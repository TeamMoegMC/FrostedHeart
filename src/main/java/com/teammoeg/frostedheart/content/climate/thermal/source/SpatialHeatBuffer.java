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

/**
 * Coordinator-owned numerical input: a disposable copy of a Store claim, plus
 * committed deferred heat. Fresh input remains owned by SpatialHeatStore until
 * publication; saves add only this buffer's committed deferred heat to it.
 *
 * Preparation ends before the field borrows a Batch. Later source input stays in
 * the Store, so these arrays need no producer handles or second filling bank.
 */
public final class SpatialHeatBuffer implements AutoCloseable {
    /** -1 denotes Air; a material generation survives phase changes, but not replacement. */
    public record Address(long position, int materialGeneration, int face) {
        /** -1 is a volume input; physical Air ports retain their actual face. */
        public Address(long position, int materialGeneration) { this(position, materialGeneration, -1); }
        public long chunk() {
            return ChunkPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
        }
    }

    @FunctionalInterface public interface PendingConsumer {
        void accept(Address address, double joules, long oldestIntervalTick);
    }

    /** Fixed input arrays and private output balances for one numerical step. */
    public static final class Batch implements ThermalHeatStep {
        public final long sequence, fromTick, toTick;
        public final int size;
        private final int[] slots;
        private final Address[] addresses;
        private final double[] joules, acceptedPending, trialPending;
        private final long[] acceptedPendingSince, trialPendingSince;
        private final SpatialHeatBuffer owner;

        private Batch(SpatialHeatBuffer owner, long sequence, long fromTick, long toTick, int size, int[] slots, Address[] addresses, double[] joules,
                double[] acceptedPending, double[] trialPending, long[] acceptedPendingSince, long[] trialPendingSince) {
            this.owner = owner;
            this.sequence = sequence; this.fromTick = fromTick; this.toTick = toTick; this.size = size; this.slots = slots;
            this.addresses = addresses; this.joules = joules;
            this.acceptedPending = acceptedPending; this.trialPending = trialPending;
            this.acceptedPendingSince = acceptedPendingSince; this.trialPendingSince = trialPendingSince;
        }

        public Address address(int slot) { return addresses[slots[slot]]; }
        @Override public long sequence() { return sequence; }
        @Override public long fromTick() { return fromTick; }
        @Override public long toTick() { return toTick; }
        @Override public int size() { return size; }
        @Override public long position(int slot) { return address(slot).position(); }
        @Override public int materialGeneration(int slot) { return address(slot).materialGeneration(); }
        @Override public int face(int slot) { return address(slot).face(); }
        @Override public void commit() { owner.commit(sequence); }
        @Override public void cancel() { owner.discardPreparation(); }
        public double joules(int slot) { return joules[slots[slot]]; }
        public double pendingJoules(int slot) { return acceptedPending[slots[slot]]; }
        /** Oldest retained integration interval, not a claim that its J were injected then. */
        public long pendingFromTick(int slot) { return acceptedPendingSince[slots[slot]]; }
        public void defer(int slot, double energyJ) {
            if (energyJ == 0) return;
            slot = slots[slot];
            long since = acceptedPending[slot] != 0 ? Math.min(fromTick, acceptedPendingSince[slot]) : fromTick;
            trialPending[slot] += energyJ;
            trialPendingSince[slot] = trialPending[slot] == 0 ? Long.MAX_VALUE
                    : Math.min(trialPendingSince[slot], since);
        }
    }

    private final ThermalMemoryBudget budget;
    private final int maximumTargets;
    private Object2IntOpenHashMap<Address> targets;
    private Long2ObjectOpenHashMap<IntOpenHashSet> byChunk;
    private final java.util.Set<Long> retainedChunks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private ThermalMemoryBudget.Reservation storage;
    private Address[] addresses;
    private int[] nextFree, selectedSlots;
    private double[] joules, acceptedPending, trialPending;
    private long[] acceptedPendingSince, trialPendingSince;
    private int highWater, freeHead = -1;
    private long preparationFrom, sequence;
    private Batch sealed;
    private boolean closed;

    public SpatialHeatBuffer(ThermalMemoryBudget budget, int initialCapacity, int maximumTargets, long initialTick) {
        if (initialCapacity <= 0 || maximumTargets < initialCapacity || initialTick < 0)
            throw new IllegalArgumentException("Invalid spatial input capacity or baseline");
        this.budget = budget; this.maximumTargets = maximumTargets; preparationFrom = initialTick;
        storage = budget.tryReserve(storageBytes(initialCapacity));
        if (storage == null) throw new ThermalMemoryBudget.AllocationRefused(storageBytes(initialCapacity));
        try {
            addresses = new Address[initialCapacity]; nextFree = new int[initialCapacity]; selectedSlots = new int[initialCapacity]; Arrays.fill(nextFree, -1);
            joules = new double[initialCapacity]; acceptedPending = new double[initialCapacity]; trialPending = new double[initialCapacity];
            acceptedPendingSince = new long[initialCapacity]; trialPendingSince = new long[initialCapacity];
            Arrays.fill(acceptedPendingSince, Long.MAX_VALUE); Arrays.fill(trialPendingSince, Long.MAX_VALUE);
            targets = new Object2IntOpenHashMap<>(initialCapacity); targets.defaultReturnValue(-1);
            byChunk = new Long2ObjectOpenHashMap<>(initialCapacity);
        } catch (RuntimeException | Error failure) { storage.close(); throw failure; }
    }

    /** Array capacity, address values and conservative map/set capacity, including publication membership. */
    private static long storageBytes(int count) {
        return 1024L + 384L * count + AirBackingMeter.arrayPayloadBytes(count, 8)
                + 2 * AirBackingMeter.arrayPayloadBytes(count, 4) + 5 * AirBackingMeter.arrayPayloadBytes(count, 8);
    }

    /** Aggregate positive and negative J at the canonical receiver before nonlinear phase application. */
    public boolean add(Address address, double energyJ) {
        if (!Double.isFinite(energyJ)) throw new IllegalArgumentException("Non-finite source input");
        if (closed) return false;
        if (sealed != null) throw new IllegalStateException("New input stays in the Store while the field borrows its batch");
        if (energyJ == 0) return true;
        int slot = targets.getInt(address);
        if (slot < 0) {
            if (freeHead < 0 && highWater == addresses.length && !grow()) return false;
            if (freeHead >= 0) { slot = freeHead; freeHead = nextFree[slot]; nextFree[slot] = -1; }
            else slot = highWater++;
            addresses[slot] = address; targets.put(address, slot);
            byChunk.computeIfAbsent(address.chunk(), ignored -> new IntOpenHashSet()).add(slot);
            retainedChunks.add(address.chunk());
        }
        joules[slot] += energyJ;
        return true;
    }

    public void prepareInterval(long fromTick) { preparationFrom = fromTick; }
    public Batch seal(long fromTick, long toTick) { return seal(fromTick, toTick, position -> true); }
    /** The recipient supplies its real interval. This buffer owns no simulation clock. */
    public Batch seal(long fromTick, long toTick, java.util.function.LongPredicate included) {
        if (closed || sealed != null) return null;
        if (toTick < fromTick) throw new IllegalArgumentException("Spatial input time moved backwards");
        preparationFrom = fromTick;
        int count = 0;
        for (int slot = 0; slot < highWater; slot++) if (addresses[slot] != null
                && (joules[slot] != 0 || acceptedPending[slot] != 0) && included.test(addresses[slot].position())) {
            selectedSlots[count++] = slot; trialPending[slot] = 0; trialPendingSince[slot] = Long.MAX_VALUE;
        }
        sealed = new Batch(this, ++sequence, fromTick, toTick, count, selectedSlots, addresses, joules,
                acceptedPending, trialPending, acceptedPendingSince, trialPendingSince);
        return sealed;
    }

    /** Runs inside the same transition that publishes the matching Accepted field and Store claim. */
    public void commit(long acceptedSequence) {
        if (sealed == null || sealed.sequence != acceptedSequence)
            throw new IllegalStateException("Spatial input does not match the field commit");
        for (int index = 0; index < sealed.size; index++) {
            int slot = selectedSlots[index]; joules[slot] = 0;
            acceptedPending[slot] = trialPending[slot]; acceptedPendingSince[slot] = trialPendingSince[slot];
        }
        sealed = null;
        collectRetired();
    }

    /** Cancel partial preparation or a quiesced numerical step; the Store still owns the original J. */
    public void discardPreparation() {
        sealed = null;
        Arrays.fill(joules, 0, highWater, 0);
        collectRetired();
    }

    /** Diagnostics/test fixtures may read the copy; native saves use committedPendingInChunk instead. */
    public void pendingInChunk(long chunk, PendingConsumer consumer) {
        var indexed = byChunk.get(chunk);
        if (indexed == null) return;
        for (int slot : indexed) {
            double pending = acceptedPending[slot] + joules[slot];
            long since = acceptedPending[slot] == 0 ? Long.MAX_VALUE : acceptedPendingSince[slot];
            if (joules[slot] != 0) since = Math.min(since, preparationFrom);
            if (pending != 0) consumer.accept(addresses[slot], pending, since);
        }
    }

    /** Only committed deferred J is additional save data; the Store already captures its original claim. */
    public void committedPendingInChunk(long chunk, PendingConsumer consumer) {
        var indexed = byChunk.get(chunk);
        if (indexed == null) return;
        for (int slot : indexed) if (acceptedPending[slot] != 0)
            consumer.accept(addresses[slot], acceptedPending[slot], acceptedPendingSince[slot]);
    }

    /** Successful unload exports this debt once, after cancelling preparation. */
    public double exportCommittedPendingInChunk(long chunk) {
        if (sealed != null) throw new IllegalStateException("Cancel numerical preparation before exporting committed input");
        var indexed = byChunk.get(chunk);
        if (indexed == null) return 0;
        double total = 0;
        for (int slot : indexed) {
            total += acceptedPending[slot]; acceptedPending[slot] = 0; acceptedPendingSince[slot] = Long.MAX_VALUE;
        }
        collectRetired();
        return total;
    }

    public int targetCount() { return targets.size(); }
    /** Batch independent receivers only within paid capacity; optional growth never creates a capacity waiter. */
    public int prepareTargets(int wanted) {
        if (closed) return 0;
        wanted = Math.min(maximumTargets, wanted);
        while (!closed && sealed == null && addresses.length < wanted) {
            int next = Math.min(maximumTargets, Math.max(addresses.length + 1, addresses.length * 2));
            if (!budget.canReserve(storageBytes(next)) || !grow()) break;
        }
        return addresses.length;
    }
    public int targetCountInChunk(long chunk) { var indexed = byChunk.get(chunk); return indexed == null ? 0 : indexed.size(); }
    /** Main reads published membership, never the coordinator's mutable receiver sets. */
    public boolean hasRetainedInputInChunk(long chunk) { return retainedChunks.contains(chunk); }
    /** Reserved capacity; actual retained heap must be measured independently. */
    public long reservedBytes() { return storage.bytes(); }
    public double committedPendingJoules() {
        double total = 0;
        for (int slot = 0; slot < highWater; slot++) total += acceptedPending[slot];
        return total;
    }

    private void collectRetired() {
        for (int slot = 0; slot < highWater; slot++) if (addresses[slot] != null && joules[slot] == 0 && acceptedPending[slot] == 0) {
            var address = addresses[slot]; targets.removeInt(address);
            var indexed = byChunk.get(address.chunk()); indexed.remove(slot);
            if (indexed.isEmpty()) { byChunk.remove(address.chunk()); retainedChunks.remove(address.chunk()); }
            addresses[slot] = null;
            acceptedPendingSince[slot] = trialPendingSince[slot] = Long.MAX_VALUE;
            nextFree[slot] = freeHead; freeHead = slot;
        }
    }

    /** All replacement overlap is reserved before any borrowed arrays can exist. */
    private boolean grow() {
        int old = addresses.length;
        if (old == maximumTargets) return false;
        int count = Math.min(maximumTargets, Math.max(old + 1, old * 2));
        var replacement = budget.tryReserve(storageBytes(count));
        if (replacement == null) return false;
        try {
            addresses = Arrays.copyOf(addresses, count);
            nextFree = Arrays.copyOf(nextFree, count); Arrays.fill(nextFree, old, count, -1);
            selectedSlots = Arrays.copyOf(selectedSlots, count);
            joules = Arrays.copyOf(joules, count);
            acceptedPending = Arrays.copyOf(acceptedPending, count); trialPending = Arrays.copyOf(trialPending, count);
            acceptedPendingSince = Arrays.copyOf(acceptedPendingSince, count); trialPendingSince = Arrays.copyOf(trialPendingSince, count);
            Arrays.fill(acceptedPendingSince, old, count, Long.MAX_VALUE); Arrays.fill(trialPendingSince, old, count, Long.MAX_VALUE);
            var nextTargets = new Object2IntOpenHashMap<Address>(count); nextTargets.defaultReturnValue(-1); nextTargets.putAll(targets);
            var nextByChunk = new Long2ObjectOpenHashMap<IntOpenHashSet>(count); nextByChunk.putAll(byChunk);
            targets = nextTargets; byChunk = nextByChunk;
        } catch (RuntimeException | Error failure) { replacement.close(); throw failure; }
        storage.close(); storage = replacement;
        return true;
    }

    @Override public void close() {
        if (closed) return;
        closed = true; sealed = null;
        targets.clear(); byChunk.clear(); retainedChunks.clear(); targets.trim(0); byChunk.trim(0);
        addresses = null; nextFree = selectedSlots = null; joules = acceptedPending = trialPending = null;
        acceptedPendingSince = trialPendingSince = null;
        storage.close();
    }
}
