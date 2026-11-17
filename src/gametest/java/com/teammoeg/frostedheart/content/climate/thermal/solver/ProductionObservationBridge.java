/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirWorkerRecoveryGameTests;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.ContinuousAirNativeCheckpointProductionGameTests;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.SourceInputProductionObservation;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Current lifecycle hooks for actual fixed-worker fault injection and native save verification. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ProductionObservationBridge implements ThermalDiagnostics.Observer {
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event) { ThermalDiagnostics.observer = new ProductionObservationBridge(); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { ThermalDiagnostics.observer = null; }
    @Override public void stage(String stage, boolean entering) { ContinuousAirWorkerRecoveryGameTests.stage(stage, entering); }
    @Override public void fixedRoundFinished(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine engine) {
        ContinuousAirWorkerRecoveryGameTests.fixedRoundFinished(engine);
    }
    @Override public void checkpointSealed(long chunk, int unboundSourceWindows,
            com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint checkpoint,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer,
            com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint cold, double halfLife) {
        ContinuousAirNativeCheckpointProductionGameTests.checkpointSealed(chunk);
        ContinuousAirWorkerRecoveryGameTests.checkpointSealed(chunk, checkpoint, producer, cold, halfLife);
    }
    @Override public void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) {
        ContinuousAirWorkerRecoveryGameTests.geometry(task, input, stage, completed);
    }
    @Override public void nativeUnload(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        ContinuousAirNativeCheckpointProductionGameTests.nativeRequestCreated(level, chunk);
    }
    @Override public void sourceEvent(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor, boolean registration) {
        SourceInputProductionObservation.afterEvent(source, descriptor, registration);
    }
    @Override public void sourceFrozen(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources frozen) {
        SourceInputProductionObservation.frozen(source, frozen);
    }
    @Override public void sourceFrozen(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources frozen,
            it.unimi.dsi.fastutil.longs.LongSet sections, long tick) {
        SourceInputProductionObservation.frozen(source, frozen, sections, tick);
    }
    @Override public void sourceCommitted(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            long cutoff, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap changes) {
        SourceInputProductionObservation.committed(source, cutoff, changes);
    }
    @Override public void sourceCommitted(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            long cutoff, it.unimi.dsi.fastutil.longs.LongSet committed, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap changes) {
        SourceInputProductionObservation.committed(source, cutoff, committed, changes);
    }
}
