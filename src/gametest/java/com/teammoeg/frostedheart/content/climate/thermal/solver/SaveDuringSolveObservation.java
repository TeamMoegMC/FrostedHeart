/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

/** Holds an actual helper while the ordinary main-thread save serializes its unconsumed input. */
public final class SaveDuringSolveObservation implements ThermalDiagnostics.Observer, AutoCloseable {
    private final ThermalDiagnostics.Observer previous = ThermalDiagnostics.observer;
    private final BlockPos[] probes;
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicReference<RegionJob> held = new AtomicReference<>();
    private volatile Thread coordinator;
    private volatile RegionalAirRuntime runtime;
    private volatile boolean blocked, sealed, encoded;
    private volatile String failure;
    private volatile int publications;
    private volatile long chunk;
    private double claimedJ, chunkClaimedJ, storedJ, encodedJ, settledJ;

    public SaveDuringSolveObservation(BlockPos... probes) {
        this.probes = probes.clone(); ThermalDiagnostics.observer = this;
    }
    @Override public void engine(RegionalAirRuntime air, ThermalCellArena cells, ThermalSourceLedger sources,
            QueryPublication queries, ThermalMemoryBudget.Reservation backing, TopologyInbox topology, long tick) {
        runtime = air; coordinator = Thread.currentThread();
    }
    @Override public void sliceStarted(RegionWork work) {
        if (!(work instanceof RegionJob job) || job.qualityOnly() || job.heat == null
                || held.get() != null || Thread.currentThread() == coordinator) return;
        BlockPos selected = null;
        for (var probe : probes) if (job.region.scope.containsAirAt(probe.asLong())) { selected = probe; break; }
        if (selected == null || job.heat.size() == 0 || !held.compareAndSet(null, job)) return;
        chunk = new ChunkPos(selected).toLong();
        for (int i = 0; i < job.heat.size(); i++) {
            double energy = job.heat.energyJ(i); claimedJ += energy;
            long position = job.heat.position(i);
            if (ChunkPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4) == chunk) chunkClaimedJ += energy;
        }
        blocked = true;
        try {
            if (!release.await(10, TimeUnit.SECONDS)) failure = "Save waited for the running numerical helper";
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); failure = "Held helper interrupted";
        }
    }
    @Override public void checkpointSealed(long saving, int unboundSourceWindows) {
        var job = held.get();
        if (!blocked || saving != chunk || release.getCount() == 0) return;
        if (job.cancelled || job.region.job != job) failure = "Ordinary save cancelled or replaced the running job";
        double[] total = {0};
        runtime.inputStore().collectChunk(chunk, (target, from, to, joules) -> total[0] += joules);
        storedJ = total[0];
        if (!(chunkClaimedJ > 0) || storedJ + 1e-6 < chunkClaimedJ) failure = "Checkpoint omitted claimed heat";
        sealed = true;
    }
    @Override public void checkpointEncoded(long saving, CompoundTag pending) {
        if (!sealed || saving != chunk || release.getCount() == 0) return;
        double total = 0;
        for (long bits : pending.getLongArray("joules")) total += Double.longBitsToDouble(bits);
        encodedJ = total;
        if (encodedJ + 1e-6 < storedJ) failure = "Serialized checkpoint omitted pending heat";
        encoded = true;
    }
    @Override public boolean beforeAccept(RegionalAirRuntime air, RegionJob job) {
        if (job == held.get()) settledJ = job.deliveredEnergyJ() + job.unacceptedEnergyJ();
        return true;
    }
    @Override public void accepted(RegionalAirRuntime air, RegionJob job, List<ThermalRegion> regions) {
        if (job != held.get()) return;
        if (++publications != 1 || Math.abs(settledJ - claimedJ) > 1e-6) failure = "Saved task did not settle its original input once";
    }
    public boolean blocked() { return blocked; }
    public long chunk() { return chunk; }
    public boolean savedWhileRunning() { return sealed && encoded && !held.get().cancelled; }
    public boolean completed() { return publications == 1; }
    public String failure() { return failure; }
    public void resume() { release.countDown(); }
    public String summary() {
        return "sealed=" + sealed + ",encoded=" + encoded + ",claimedJ=" + claimedJ + ",chunkClaimedJ=" + chunkClaimedJ
                + ",storedJ=" + storedJ + ",encodedJ=" + encodedJ + ",settledJ=" + settledJ
                + ",publications=" + publications + ",cancelled=" + (held.get() != null && held.get().cancelled) + ",failure=" + failure;
    }
    @Override public void close() { resume(); if (ThermalDiagnostics.observer == this) ThermalDiagnostics.observer = previous; }
}
