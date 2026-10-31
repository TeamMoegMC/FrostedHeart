/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import net.minecraft.core.BlockPos;

/** Observes real ownership and Accepted publications. It never injects heat or changes scheduling. */
public final class RuntimeConvergenceObservation {
    @jdk.jfr.Name("frostedheart.ThermalRegionWait")
    @jdk.jfr.StackTrace(false)
    private static final class WaitObservation extends jdk.jfr.Event {
        public long regionId, receivedTick, sampleTick, pendingThrough, executionLimit, effectiveLimit, jobTarget;
        public String role, state;
        public boolean routing, snapshot, material, quality;
    }
    private static final long[] waitSignatures = {Long.MIN_VALUE, Long.MIN_VALUE};
    public record Snapshot(int boundSources, long region, long controlRegion, int step, boolean minimum, long sampleTick,
            long commits, long releasedWindows, long acceptedAheadOfBinding, int orderingFailures,
            int maximumOwnedWindows, double maximumImbalanceJ, long[] pressureCommits,
            int unboundSnapshots, int unboundDiskWrites, int boundReplacements, long cacheHits, long cacheMisses,
            long bindingWaits, int diskReadAttempts, int encodingFailures, long energyObservations) { }
    private static BlockPos target, control;
    private static final java.util.Set<Long> sourceIds = new java.util.HashSet<>();
    private static final java.util.Map<Long, SpatialSourceIngress.Progress> accepted = new java.util.HashMap<>();
    private static final java.util.Map<Long, Integer> boundGenerations = new java.util.HashMap<>();
    private static final java.util.Set<Long> chunks = new java.util.HashSet<>();
    private static final java.util.Set<Long> unboundChunks = new java.util.HashSet<>();
    private static long lastObservedTick = -1, lastPhysicalTick = -1, commits, released, ahead;
    private static long regionId, controlId, pressureStart = Long.MAX_VALUE;
    private static int boundSources, step, failures, maximumOwned;
    private static int unboundSnapshots, unboundDiskWrites, boundReplacements;
    private static boolean minimum;
    private static long cacheHits, cacheMisses;
    private static java.util.function.BiConsumer<Long, net.minecraft.nbt.CompoundTag> unboundWrite;
    private static java.util.function.LongConsumer bindingSave;
    private static boolean saveScheduled;
    private static long lastSaveTick;
    private static boolean readScheduled;
    private static int diskReadAttempts, encodingFailures;
    private static long bindingWaits, energyObservations;
    private static double imbalance;
    private static final long[] windows = new long[4];
    public static synchronized void arm(BlockPos normal, BlockPos stress, BlockPos[] fires) {
        target = stress.immutable(); control = normal.immutable(); sourceIds.clear(); accepted.clear(); boundGenerations.clear(); chunks.clear();
        unboundChunks.clear();
        for (var fire : fires) { sourceIds.add(fire.asLong()); chunks.add(net.minecraft.world.level.ChunkPos.asLong(fire.getX() >> 4, fire.getZ() >> 4)); }
        lastObservedTick = lastPhysicalTick = -1; commits = released = ahead = regionId = controlId = 0;
        pressureStart = Long.MAX_VALUE; boundSources = step = failures = maximumOwned = 0; imbalance = 0;
        java.util.Arrays.fill(windows, 0);
        unboundSnapshots = unboundDiskWrites = boundReplacements = 0; minimum = false; cacheHits = cacheMisses = 0;
        unboundWrite = null; readScheduled = false; diskReadAttempts = encodingFailures = 0;
        bindingSave = null; saveScheduled = false; lastSaveTick = -1;
        bindingWaits = energyObservations = 0;
        java.util.Arrays.fill(waitSignatures, Long.MIN_VALUE);
    }
    public static synchronized void pressure(long tick) { pressureStart = tick; java.util.Arrays.fill(windows, 0); }
    static synchronized void source(long id, SpatialSourceIngress.Progress next, SpatialSourceIngress.Progress bound,
            boolean producerReleased, int owned, int pending, double unboundJ, boolean bindingWaiting) {
        if (target == null || !sourceIds.contains(id)) return;
        maximumOwned = Math.max(maximumOwned, owned);
        if (owned > 2) failures++;
        if (producerReleased) {
            released++;
            var previous = accepted.put(id, next);
            if (previous != null && (next.generation() < previous.generation()
                    || next.generation() == previous.generation() && (next.tick() < previous.tick()
                    || next.sequence() < previous.sequence()
                    || next.sequence() == previous.sequence() && next.ordinal() <= previous.ordinal()))) failures++;
            if (bound == null || bound.generation() < next.generation() || bound.tick() < next.tick()) ahead++;
        }
        if (bound != null && next != null && bound.generation() == next.generation() && bound.tick() > next.tick()) failures++;
        if (bound != null) {
            var previous = boundGenerations.put(id, bound.generation());
            if (previous != null && bound.generation() > previous) boundReplacements++;
        }
        if (bindingWaiting && unboundJ != 0) {
            bindingWaits++;
            if (bindingSave != null && !saveScheduled && !readScheduled && unboundDiskWrites == 0
                    && next != null && next.tick() > lastSaveTick) {
                saveScheduled = true; lastSaveTick = next.tick();
                var position = BlockPos.of(id);
                bindingSave.accept(net.minecraft.world.level.ChunkPos.asLong(position.getX() >> 4, position.getZ() >> 4));
            }
        }
    }
    public static synchronized void whenBindingWait(java.util.function.LongConsumer saveOnMain) { bindingSave = saveOnMain; }
    public static synchronized void bindingSaveCompleted() { saveScheduled = false; }
    public static synchronized void whenUnboundWrite(java.util.function.BiConsumer<Long, net.minecraft.nbt.CompoundTag> observeOnMain) {
        unboundWrite = observeOnMain;
    }
    public static synchronized void unboundReadCompleted() { readScheduled = false; }
    public static synchronized void unboundDiskWritten() { unboundDiskWrites++; }
    static synchronized void checkpoint(long chunk, int unbound) {
        if (target == null || !chunks.contains(chunk)) return;
        if (unbound == 0) { unboundChunks.remove(chunk); return; }
        unboundChunks.add(chunk); unboundSnapshots++;
    }
    static synchronized void encoded(long chunk, net.minecraft.nbt.CompoundTag pending) {
        if (target == null || !unboundChunks.remove(chunk)) return;
        boolean originalOutlet = false;
        byte[] faces = pending.getByteArray("faces"); long[] energy = pending.getLongArray("joules");
        for (int index = 0; index < Math.min(faces.length, energy.length); index++)
            originalOutlet |= faces[index] >= 6 && faces[index] <= 11 && Double.longBitsToDouble(energy[index]) != 0;
        if (!originalOutlet) { encodingFailures++; return; }
        if (unboundWrite != null && unboundDiskWrites == 0 && !readScheduled) {
            readScheduled = true; diskReadAttempts++; unboundWrite.accept(chunk, pending.copy());
        }
    }
    static synchronized void round(RegionalAirRuntime runtime, ThermalSourceLedger ledger, long tick) {
        if (target == null) return;
        var region = at(runtime, target);
        var normal = at(runtime, control);
        trace(runtime, region, 0); trace(runtime, normal, 1);
        if (tick == lastObservedTick) return;
        lastObservedTick = tick;
        var balance = ledger.energyBalance();
        imbalance = Math.max(imbalance, Math.abs(balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ()
                - balance.degradedLossJ() - balance.unacceptedJ() - balance.exportedJ() - runtime.pendingEnergyJ()));
        energyObservations++;
        if (region == null || normal == null) return;
        regionId = region.id; controlId = normal.id; step = region.solver.gridStep();
        minimum = region.accepted().field() != null && region.accepted().field().minimumQuality();
        cacheHits = region.minimumInputs == null ? 0 : region.minimumInputs.hits();
        cacheMisses = region.minimumInputs == null ? 0 : region.minimumInputs.misses();
        boundSources = 0;
        for (long source : sourceIds) if (region.inputSources.containsKey(source) && ledger.suppliesPower(source)) boundSources++;
    }
    private static void trace(RegionalAirRuntime runtime, ThermalRegion region, int role) {
        if (region == null) return;
        var job = region.job;
        long through = region.pending.throughTick(region.sampleTick());
        long signature = region.id;
        signature = signature * 31 + runtime.receivedThrough();
        signature = signature * 31 + region.sampleTick(); signature = signature * 31 + through;
        signature = signature * 31 + region.executionLimit();
        signature = signature * 31 + (job == null ? -1 : job.input.toTick);
        signature = signature * 31 + (job == null ? -1 : job.state.ordinal());
        signature = signature * 31 + (job == null ? -1 : job.waiting.ordinal());
        signature = signature * 31 + (region.routingPending ? 1 : 0);
        signature = signature * 31 + region.snapshotPauses;
        signature = signature * 31 + region.changedMaterials.size();
        signature = signature * 31 + (region.materialControls == null ? 0 : 1);
        signature = signature * 31 + (region.qualityCandidate == null ? 0 : 1);
        if (signature == waitSignatures[role]) return;
        waitSignatures[role] = signature;
        var event = new WaitObservation();
        if (!event.isEnabled()) return;
        event.role = role == 0 ? "stress" : "control"; event.regionId = region.id;
        event.receivedTick = runtime.receivedThrough(); event.sampleTick = region.sampleTick();
        event.pendingThrough = through; event.executionLimit = region.executionLimit();
        event.effectiveLimit = runtime.executionLimit(region);
        event.jobTarget = job == null ? -1 : job.input.toTick;
        event.state = job == null ? "none" : job.state + ":" + job.waiting;
        event.routing = region.routingPending; event.snapshot = region.snapshotPauses != 0;
        event.material = !region.changedMaterials.isEmpty() || region.materialControls != null;
        event.quality = region.qualityCandidate != null; event.commit();
    }
    static synchronized void committed(RegionalAirRuntime runtime, RegionJob job) {
        if (target == null || job.region.solver.layout().componentAt(target.getX(), target.getY(), target.getZ()) == null) return;
        long tick = job.input.toTick;
        if (tick < lastPhysicalTick) failures++;
        if (tick > lastPhysicalTick) {
            commits++;
            long elapsed = runtime.receivedThrough() - pressureStart;
            if (elapsed >= 0 && elapsed < 1600) windows[(int) (elapsed / 400)]++;
        }
        lastPhysicalTick = Math.max(lastPhysicalTick, tick);
    }
    private static ThermalRegion at(RegionalAirRuntime runtime, BlockPos point) {
        for (var region : runtime.observedRegions())
            if (region.solver.layout().componentAt(point.getX(), point.getY(), point.getZ()) != null) return region;
        return null;
    }
    public static synchronized Snapshot snapshot() {
        return new Snapshot(boundSources, regionId, controlId, step, minimum, lastPhysicalTick, commits, released, ahead,
                failures, maximumOwned, imbalance, windows.clone(), unboundSnapshots, unboundDiskWrites, boundReplacements, cacheHits, cacheMisses,
                bindingWaits, diskReadAttempts, encodingFailures, energyObservations);
    }
    public static synchronized void stop() {
        target = control = null; unboundWrite = null; bindingSave = null; sourceIds.clear(); accepted.clear(); boundGenerations.clear(); chunks.clear(); unboundChunks.clear();
    }
}
