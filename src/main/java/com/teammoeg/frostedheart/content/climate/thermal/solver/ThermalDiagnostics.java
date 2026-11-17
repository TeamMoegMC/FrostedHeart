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

/** Optional lifecycle observation. Production has no listener and creates no diagnostic snapshots. */
public final class ThermalDiagnostics {
    private ThermalDiagnostics() { }
    public static final boolean TRACE = Boolean.parseBoolean(System.getenv("FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS"));
    public static volatile Observer observer;
    public interface Observer {
        /** Called with the fixed coordinator's executing ownership, before that turn is released. */
        default void fixedRoundFinished(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine engine) { }
        /** Executing ownership is released; main may process checkpoint control before the next continuation. */
        default void coordinatorReleased(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine engine) { }
        default void stage(String stage, boolean entering) { }
        /** Actual player air result, before radiation and analytic fields compose the environment. */
        default void playerAirSampled(net.minecraft.server.level.ServerPlayer player,
                com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample sample, String source) { }
        default void engine(ThermalCellArena cells, ThermalSourceLedger sources,
                QueryPublication queries, ThermalMemoryBudget.Reservation backing, TopologyInbox topology, long tick) { }
        default void roundFinished(ThermalCellArena cells, ThermalSourceLedger sources,
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
        default void checkpointSealed(long chunk, int unboundSourceWindows,
                com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint checkpoint,
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer,
                com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint cold,
                double halfLife) { checkpointSealed(chunk, unboundSourceWindows); }
        default void checkpointEncoded(long chunk, net.minecraft.nbt.CompoundTag pending) { }
        default void sourceEvent(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor, boolean registration) { }
        default void sourceFrozen(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources frozen) { }
        default void sourceFrozen(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources frozen,
                it.unimi.dsi.fastutil.longs.LongSet sections, long tick) { sourceFrozen(source, frozen); }
        default void sourceCommitted(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                long cutoff, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap changes) { }
        default void sourceCommitted(com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings source,
                long cutoff, it.unimi.dsi.fastutil.longs.LongSet committed,
                it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap changes) { sourceCommitted(source, cutoff, changes); }

        /** A diagnostic may request ordinary cancellation; it cannot replace a result or fabricate a future. */

        default void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean completed) { }
        default void mixingUpdated(ThermalInputBatch input, int bricks) { }
        default void mainTick(net.minecraft.server.level.ServerLevel level) { }
        default void mainIdle(boolean idle) { }
        default void nativeUnload(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) { }
    }
}
