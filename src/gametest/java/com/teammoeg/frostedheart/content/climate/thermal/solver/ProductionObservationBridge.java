/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.*;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.topology.*;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

/** Compiled only for GameTest. Production calls an explicit optional observer, never its own Mixin. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ProductionObservationBridge implements ThermalDiagnostics.Observer {
    private long observedCommit = -1;
    private static final boolean REGION_STATE = "true".equalsIgnoreCase(System.getenv("FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS"));
    private long lastStateTick = -1;
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event) { ThermalDiagnostics.observer = new ProductionObservationBridge(); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (ThermalNativeCpuObservation.ENABLED) ThermalNativeCpuObservation.close();
        var scattered = ScatteredLoadObservation.current();
        if (scattered != null) scattered.close();
        ThermalDiagnostics.observer = null;
    }
    @Override public void engine(RegionalAirRuntime air, ThermalCellArena cells, ThermalSourceLedger sources,
            QueryPublication queries, ThermalMemoryBudget.Reservation backing, TopologyInbox topology, long tick) {
        SourceStepProductionObservation.engine(cells, backing);
        RegionalAcceptanceObservation.engine(air, cells, sources, queries);
        RegionalBoundaryObservation.engine(air, sources, queries);
        RegionalGridSwitchObservation.engine(air, sources, queries);
        WorkPolicyObservation.begin(air);
    }
    @Override public void roundFinished(RegionalAirRuntime air, ThermalCellArena cells, ThermalSourceLedger sources,
            TopologyInbox topology, long tick, boolean pending) {
        QueuedInputObservation.round(air, sources, pending);
        RegionalBoundaryObservation.round(air, topology, pending);
        var runtime = air;
        RuntimeConvergenceObservation.round(runtime, sources, tick);
        var scattered = ScatteredLoadObservation.current();
        if (scattered != null) scattered.roundFinished(air, cells, sources, tick);
        if (REGION_STATE && tick >= lastStateTick + 200) {
            lastStateTick = tick;
            for (var region : runtime.observedRegions()) FHMain.LOGGER.info(
                    "THERMAL_REGION_SCHEDULING,receivedTick={},runtime={},region={},sampleTick={},pending={},routing={},routingTargets={},limit={},snapshotPauses={},snapshotSeals={},qualityIntent={},qualityCandidate={},materials={},job={},jobWait={}",
                    tick, System.identityHashCode(runtime), region.id, region.sampleTick(), region.pending.hasPending(),
                    region.routingPending, region.routingTargets.size(), region.executionLimit(), region.snapshotPauses,
                    region.snapshotSeals, region.qualityIntent, region.qualityCandidate, region.changedMaterials.size(),
                    region.job == null ? "none" : region.job.state, region.job == null ? "none" : region.job.waiting);
            for (var region : runtime.observedRegions()) for (var source : region.inputSources.long2IntEntrySet())
                FHMain.LOGGER.info("THERMAL_REGION_SOURCE_WATERMARK,receivedTick={},region={},source={},generation={},boundThrough={}",
                        tick, region.id, source.getLongKey(), source.getIntValue(),
                        sources.spatialBoundThrough(source.getLongKey(), source.getIntValue()));
        }
        RegionalGridSwitchObservation.observe(runtime);
        if (runtime.commitRevision() != observedCommit) {
            observedCommit = runtime.commitRevision(); WorkPolicyObservation.end(air, sources, tick);
            GeometryInputProductionObservation.afterCut(cells);
        }
    }
    @Override public void sourceProgress(long source,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress.Progress accepted,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress.Progress bound,
            boolean producerReleased, int ownedWindows, int producerWindows, double unboundJ, boolean bindingWaiting) {
        RuntimeConvergenceObservation.source(source, accepted, bound, producerReleased, ownedWindows, producerWindows, unboundJ, bindingWaiting);
        var scattered = ScatteredLoadObservation.current();
        if (scattered != null) scattered.sourceProgress(source, accepted, bound, producerReleased, ownedWindows);
    }
    @Override public void checkpointSealed(long chunk, int unboundSourceWindows) {
        RuntimeConvergenceObservation.checkpoint(chunk, unboundSourceWindows);
    }
    @Override public void checkpointEncoded(long chunk, net.minecraft.nbt.CompoundTag pending) {
        RuntimeConvergenceObservation.encoded(chunk, pending);
    }
    @Override public boolean beforeAccept(RegionalAirRuntime runtime, RegionJob job) { return SourceStepProductionObservation.beforeAccept(runtime, job); }
    @Override public void accepted(RegionalAirRuntime runtime, RegionJob job, List<ThermalRegion> regions) {
        RuntimeConvergenceObservation.committed(runtime, job);
        SourceStepProductionObservation.afterAccept(job, true);
        RegionalAcceptanceObservation.accepted(runtime, job, regions, true);
        RegionalBoundaryObservation.accepted(runtime, job, true);
    }
    @Override public void installing(RegionalAirRuntime runtime, List<ThermalRegion> previous, List<ThermalRegion> candidates) {
        WorkPolicyObservation.installing(previous, candidates); RegionalGridSwitchObservation.installing(previous, candidates);
    }
    @Override public void installed(RegionalAirRuntime runtime) { RegionalGridSwitchObservation.installed(runtime); }
    @Override public void qualityPublished(ThermalRegion region, com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot before,
            com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot after) {
        RegionalGridSwitchObservation.qualityPublished(region, before, after);
        ContinuousAirMultiDimensionProductionGameTests.qualityNumbers(before, after);
    }
    @Override public void blocked(RegionalContinuationBoundary boundary, long tick, List<ThermalRegion> regions) { RegionalBoundaryObservation.blocked(boundary, tick, regions); }
    @Override public void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) {
        ContinuousAirWorkerRecoveryGameTests.geometry(input, stage, completed);
        QueuedInputObservation.structure(task, input.targetTick(), completed);
        RegionalBoundaryObservation.geometry(task, input, boundary, stage, completed);
        if ("PUBLISHED".equals(stage)) StructuralIsolationObservation.published(task, input);
        else if (completed) StructuralIsolationObservation.finished(task);
        else StructuralIsolationObservation.advancing(task, input, boundary, stage);
    }
    @Override public void mainTick(net.minecraft.server.level.ServerLevel level) {
        Runnable action = QueuedInputObservation.takeCaptureAction(level); if (action != null) action.run();
    }
    @Override public void mainIdle(boolean idle) { RegionalBoundaryObservation.mainIdle(idle); }
    @Override public void nativeUnload(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        ContinuousAirNativeCheckpointProductionGameTests.nativeRequestCreated(level, chunk);
        ContinuousAirSourceIngressProductionGameTests.nativeRequestCreated(level, chunk);
    }
    @Override public void inputAccepted(ThermalInputEnvelope input, boolean unfinished) { QueuedInputObservation.accepted(input, unfinished); }
    @Override public void inputRetired(long sequence, long section) { QueuedInputObservation.retired(sequence, section); }
    @Override public void transportAcknowledged(long sequence, boolean unfinished) { QueuedInputObservation.acknowledged(sequence, unfinished); }
    @Override public void sourceEvent(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor, boolean registration) { SourceInputProductionObservation.afterEvent(source, descriptor, registration); }
    @Override public void sourceFrozen(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources frozen) { SourceInputProductionObservation.frozen(source, frozen); }
    @Override public void sourceCommitted(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
            long cutoff, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap changes) { SourceInputProductionObservation.committed(source, cutoff, changes); }
    @Override public void sliceStarted(RegionWork work) {
        if (ThermalNativeCpuObservation.ENABLED) ThermalNativeCpuObservation.registerCurrent();
    }
    @Override public void closed(ThermalCellArena cells) {
        GeometryInputProductionObservation.closing(cells); SourceStepProductionObservation.closed(cells);
        var scattered = ScatteredLoadObservation.current();
        if (scattered != null) scattered.runtimeClosed(cells);
    }
}
