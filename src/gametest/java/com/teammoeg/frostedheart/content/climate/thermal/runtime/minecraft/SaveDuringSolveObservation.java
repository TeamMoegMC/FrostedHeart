/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

/** Pauses a real private trial while its coordinator still owns the executing turn. */
public final class SaveDuringSolveObservation implements ThermalDiagnostics.Observer, AutoCloseable {
    /** Test-only return hook sees fast private solves before the original engine publishes them. */
    public static void privateAdvanceFinished(TiledThermalField field) {
        if (ThermalDiagnostics.observer instanceof SaveDuringSolveObservation observation && observation.field == field
                && observation.armed && !observation.blocked) observation.observePrivateValues();
    }
    private final ThermalDiagnostics.Observer previous = ThermalDiagnostics.observer;
    private final BlockPos[] probes;
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicReference<ThermalDimensionEngine> held = new AtomicReference<>();
    private final TiledThermalField.SectionSnapshot initial = new TiledThermalField.SectionSnapshot();
    private QueryPublication expectedPublication;
    private SpatialHeatStore.Claim claim;
    private TiledThermalField field;
    private SpatialHeatBuffer banks;
    private ThermalSourceLedger ledger;
    private BlockPos point;
    private volatile boolean armed, readyToHold, blocked, sealed, encoded, diskVerified, completed;
    private volatile String failure;
    private long chunk, fromTick, throughTick, pairsBefore;
    private int publications, completedBeforeHold, observedRounds, privateRounds, matchedProbes;
    private double claimedJ, chunkClaimedJ, pendingBeforeJ, deliveredBeforeJ, unacceptedBeforeJ;
    private double storeJ, bankDebtJ, producerTailJ, replayJ, expectedJ, encodedJ, diskJ, settledJ;
    private double acceptedC, workingC, savedC, diskC, maximumLedgerErrorJ;
    private long saveNanos;

    public SaveDuringSolveObservation(QueryPublication publication, BlockPos... probes) {
        expectedPublication = publication; this.probes = probes.clone(); ThermalDiagnostics.observer = this;
    }

    @Override public void fixedRoundFinished(ThermalDimensionEngine engine) {
        if (held.get() == null) {
            QueryPublication publication = read(engine, "queries");
            if (publication != expectedPublication) return;
            observedRounds++;
            TiledThermalField candidate = read(engine, "fixedField");
            SpatialHeatStore.Claim input = read(engine, "fixedClaim");
            long target = read(engine, "fixedSolveTick");
            if (candidate == null || candidate.idle() || input == null || target <= (long) read(engine, "fixedFromTick")) return;
            privateRounds++;
            BlockPos selected = null; double selectedJ = 0;
            var sample = new TiledThermalField.Sample();
            for (var probe : probes) {
                if (!candidate.read(probe.asLong(), false, sample)
                        || !candidate.snapshotAccepted(SectionPos.asLong(probe), initial)) continue;
                long owner = new ChunkPos(probe).toLong(); double joules = input.energyJInChunk(owner);
                if (joules > 0) { selected = probe; selectedJ = joules; break; }
            }
            if (selected == null || !held.compareAndSet(null, engine)) return;
            matchedProbes++;
            claim = input; field = candidate; point = selected; chunk = new ChunkPos(point).toLong();
            banks = read(engine, "fixedHeat"); ledger = read(engine, "sources");
            claimedJ = input.energyJ();
            chunkClaimedJ = selectedJ; fromTick = read(engine, "fixedFromTick"); throughTick = target;
            field.read(point.asLong(), false, sample); acceptedC = sample.temperatureC();
            pendingBeforeJ = banks.committedPendingJoules();
            var balance = ledger.energyBalance(); deliveredBeforeJ = balance.deliveredJ(); unacceptedBeforeJ = balance.unacceptedJ();
            pairsBefore = field.pairUpdates(); readyToHold = false;
            armed = true;
            return;
        }
        if (held.get() != engine || !armed) return;
        boolean accepted = read(claim, "accepted"), closed = read(claim, "closed");
        if (!blocked) {
            if (closed || field.idle()) {
                completedBeforeHold++; armed = false; held.set(null); return;
            }
            observePrivateValues();
            return;
        }
        var balance = ledger.energyBalance();
        double error = balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ() - balance.degradedLossJ()
                - balance.unacceptedJ() - balance.exportedJ() - engine.inputStore().pendingEnergyJ() - banks.committedPendingJoules();
        maximumLedgerErrorJ = Math.max(maximumLedgerErrorJ, Math.abs(error));
        if (!near(0, error)) fail("The original ledger lost ownership after save: errorJ=" + error);
        if (completed) return;
        if (closed && !accepted) { fail("The held numerical claim was cancelled"); return; }
        if (!accepted) return;
        settledJ = balance.deliveredJ() - deliveredBeforeJ + balance.unacceptedJ() - unacceptedBeforeJ
                + banks.committedPendingJoules() - pendingBeforeJ;
        if (field.sectionSampleTick(SectionPos.asLong(point)) != throughTick || !near(settledJ, claimedJ))
            fail("The original claim did not publish its original J once");
        publications++; completed = true;
    }

    private void observePrivateValues() {
        if (field.pairUpdates() > pairsBefore) {
            workingC = workingTemperature();
            readyToHold = Double.isFinite(workingC) && !near(workingC, acceptedC);
        }
        if (readyToHold) holdExecutingTurn();
    }

    private void holdExecutingTurn() {
        blocked = true;
        try {
            if (!release.await(2, TimeUnit.SECONDS) && !sealed) {
                fail("Ordinary SAVE waited for ownership of the executing coordinator turn");
                release.countDown();
            } else if (!release.await(10, TimeUnit.SECONDS)) {
                fail("Disk verification did not release the held private trial");
                release.countDown();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); fail("Held coordinator continuation interrupted");
        }
    }

    @Override public void checkpointSealed(long saving, int unboundSourceWindows, RegionalCheckpoint checkpoint,
            SpatialSourceTimeline.Capture producer, PendingHeatCheckpoint cold, double halfLife) {
        if (!blocked || saving != chunk || release.getCount() == 0) return;
        var engine = held.get();
        if (read(engine, "fixedClaim") != claim || field.idle() || field.sectionSampleTick(SectionPos.asLong(point)) != fromTick)
            fail("Ordinary SAVE cancelled, replaced or caught up the held trial");
        double[] store = {0}, debt = {0}, tail = {0}, replay = {0};
        engine.inputStore().collectChunk(chunk, (target, from, to, joules) -> store[0] += joules);
        banks.committedPendingInChunk(chunk, (address, joules, since) -> debt[0] += joules);
        producer.pending(chunk, engine::sourcePortDestination, (target, from, to, joules) -> tail[0] += joules);
        cold.collectRemainingForSave(0, producer.tick, halfLife, (target, from, to, joules) -> replay[0] += joules);
        java.util.ArrayDeque<?> restores = read(engine, "restoreRequests");
        for (var restore : restores) {
            long restoring = read(restore, "chunk"); if (restoring != chunk) continue;
            PendingHeatCheckpoint pending = read(restore, "pending"); int cursor = read(restore, "cursor");
            pending.collectRemainingForSave(cursor, producer.tick, halfLife, (target, from, to, joules) -> replay[0] += joules);
        }
        storeJ = store[0]; bankDebtJ = debt[0]; producerTailJ = tail[0]; replayJ = replay[0];
        expectedJ = storeJ + bankDebtJ + producerTailJ + replayJ;
        double capturedJ = 0; var pending = checkpoint.pending();
        for (int index = 0; index < pending.size(); index++) capturedJ += pending.energyJ(index);
        if (!(chunkClaimedJ > 0) || storeJ + 1e-6 < chunkClaimedJ || !near(expectedJ, capturedJ))
            fail("Captured input must contain the store claim once plus the independently owned tail/debt/replay");
        boolean found = false;
        for (var section : checkpoint.sections()) if (section.sectionKey() == SectionPos.asLong(point)) {
            var air = section.captureAir(0);
            savedC = air == null ? Double.NaN : air.sampleC(point.getX() + .5, point.getY() + .5, point.getZ() + .5);
            if (section.sampleTick() != fromTick || !near(savedC, acceptedC) || near(savedC, workingC))
                fail("SAVE included Working temperature or the wrong sample clock");
            found = true;
        }
        if (!found) fail("The actual checkpoint omitted the Accepted source Section");
        sealed = true;
    }

    @Override public void checkpointEncoded(long saving, CompoundTag pending) {
        if (!sealed || saving != chunk || release.getCount() == 0) return;
        encodedJ = energy(pending);
        if (!near(expectedJ, encodedJ)) fail("Encoded pending input differs from its independent ownership total");
        encoded = true;
    }

    public void verifyDisk(CompoundTag disk, int minimumSectionY, int sectionCount) {
        diskJ = energy(disk.getCompound("FrostedHeartThermal").getCompound("pending"));
        var stored = DormantChunkThermalState.decode(disk, minimumSectionY, sectionCount);
        var section = stored == null ? null : stored.air(point.getY() >> 4);
        var air = section == null ? null : section.spatialAir();
        diskC = air == null ? Double.NaN : air.sampleC(point.getX() + .5, point.getY() + .5, point.getZ() + .5);
        if (!near(expectedJ, diskJ) || !near(acceptedC, diskC)) fail("Flushed disk NBT differs from the held trial's Accepted state and pending J");
        if (!savedWhileRunning()) fail("Disk save did not finish while the same original trial was unfinished");
        diskVerified = true;
    }

    private static double energy(CompoundTag pending) {
        double result = 0; for (long bits : pending.getLongArray("joules")) result += Double.longBitsToDouble(bits); return result;
    }
    /** Read only while the real coordinator owns its turn; no numerical state is changed. */
    private double workingTemperature() {
        it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<?> tiles = read(field, "bySection");
        var tile = tiles.get(SectionPos.asLong(point)); byte[] slots = read(tile, "brickSlots");
        int brick = (point.getX() & 15) >>> 2 | ((point.getZ() & 15) >>> 2) << 2 | ((point.getY() & 15) >>> 2) << 4;
        int cell = slots[brick] * 64 + (point.getX() & 3) + ((point.getZ() & 3) << 2) + ((point.getY() & 3) << 4);
        Object[] frames = read(tile, "frames"); int accepted = read(tile, "current");
        double[] h = read(frames[accepted ^ 1], "h");
        return h[cell] / field.airCapacityJPerK();
    }
    private static boolean near(double first, double second) {
        return Double.isFinite(first) && Double.isFinite(second) && Math.abs(first - second) <= 1e-7 * Math.max(1, Math.max(Math.abs(first), Math.abs(second)));
    }
    private void fail(String message) { if (failure == null) failure = message; }
    private static <T> T read(Object target, String field) { return ThermalLoadedWorldGameTests.read(target, field); }
    public boolean blocked() { return blocked; }
    public long chunk() { return chunk; }
    public boolean capturedWhileRunning() {
        var engine = held.get();
        return sealed && engine != null && read(engine, "fixedClaim") == claim && !field.idle() && field.sectionSampleTick(SectionPos.asLong(point)) == fromTick;
    }
    public boolean savedWhileRunning() {
        return encoded && capturedWhileRunning();
    }
    public boolean completed() { return completed && publications == 1 && diskVerified; }
    public void saveDuration(long nanos) { saveNanos = nanos; }
    public String failure() { return failure; }
    public void resume() { release.countDown(); }
    public String summary() {
        return "sealed=" + sealed + ",encoded=" + encoded + ",diskVerified=" + diskVerified + ",fromTick=" + fromTick + ",throughTick=" + throughTick
                + ",claimedJ=" + claimedJ + ",chunkClaimedJ=" + chunkClaimedJ + ",storeJ=" + storeJ + ",acceptedDebtJ=" + bankDebtJ
                + ",producerTailJ=" + producerTailJ + ",replayJ=" + replayJ + ",expectedJ=" + expectedJ + ",encodedJ=" + encodedJ + ",diskJ=" + diskJ
                + ",acceptedC=" + acceptedC + ",workingC=" + workingC + ",savedC=" + savedC + ",diskC=" + diskC + ",settledJ=" + settledJ
                + ",completedBeforeHold=" + completedBeforeHold
                + ",observedRounds=" + observedRounds + ",privateRounds=" + privateRounds + ",matchedProbes=" + matchedProbes
                + ",saveNs=" + saveNanos
                + ",maximumLedgerErrorJ=" + maximumLedgerErrorJ + ",publications=" + publications + ",failure=" + failure;
    }
    @Override public void close() {
        resume(); if (ThermalDiagnostics.observer == this) ThermalDiagnostics.observer = previous;
        // A callback already in flight may finish after the global listener is replaced.
        held.set(null); expectedPublication = null;
    }
}
