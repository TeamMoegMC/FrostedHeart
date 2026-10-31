/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import java.util.List;

/** Optional lifecycle observation. Production has no listener and creates no diagnostic snapshots. */
public final class ThermalDiagnostics {
    private ThermalDiagnostics() { }
    public static final boolean TRACE = Boolean.parseBoolean(System.getenv("FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS"));
    public static volatile Observer observer;
    public interface Observer {
        default void stage(String stage, boolean entering) { }
        default void engine(RegionalAirRuntime air, ThermalCellArena cells, ThermalSourceLedger sources,
                QueryPublication queries, ThermalMemoryBudget.Reservation backing, TopologyInbox topology, long tick) { }
        default void roundFinished(RegionalAirRuntime air, ThermalCellArena cells, ThermalSourceLedger sources,
                TopologyInbox topology, long tick, boolean pending) { }
        default void closed(ThermalCellArena cells) { }
        default void inputAccepted(ThermalInputEnvelope input, boolean unfinishedWork) { }
        default void inputRetired(long sequence, long section) { }
        default void transportAcknowledged(long sequence, boolean unfinishedWork) { }
        default void sourceProgress(long source,
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress.Progress accepted,
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress.Progress bound,
                boolean producerReleased, int ownedWindows, int producerWindows, double unboundJ, boolean bindingWaiting) { }
        default void checkpointSealed(long chunk, int unboundSourceWindows) { }
        default void checkpointEncoded(long chunk, net.minecraft.nbt.CompoundTag pending) { }
        default void sourceEvent(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor, boolean registration) { }
        default void sourceFrozen(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources frozen) { }
        default void sourceCommitted(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                long cutoff, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap changes) { }
        default void sliceStarted(RegionWork work) { }
        default void sliceFinished(RegionWork work, long nanos) { }
        /** A diagnostic may request ordinary cancellation; it cannot replace a result or fabricate a future. */
        default boolean beforeAccept(RegionalAirRuntime runtime, RegionJob job) { return true; }
        default void accepted(RegionalAirRuntime runtime, RegionJob job, List<ThermalRegion> regions) { }
        default void installing(RegionalAirRuntime runtime, List<ThermalRegion> previous, List<ThermalRegion> candidates) { }
        default void installed(RegionalAirRuntime runtime) { }
        default void qualityPublished(ThermalRegion region, com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot before,
                com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot after) { }
        default void blocked(RegionalContinuationBoundary boundary, long tick, List<ThermalRegion> regions) { }
        default void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) { }
        default void mainTick(net.minecraft.server.level.ServerLevel level) { }
        default void mainIdle(boolean idle) { }
        default void nativeUnload(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) { }
    }
}
