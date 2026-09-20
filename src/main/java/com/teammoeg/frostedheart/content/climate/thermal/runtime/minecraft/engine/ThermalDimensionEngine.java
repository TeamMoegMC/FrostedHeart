/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionProcessor;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ContinuousAirSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.CutLoadBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.topology.BrickTopologyCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.PreparedTopologyChange;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.TopologyCommitter;
import com.teammoeg.frostedheart.content.climate.thermal.topology.TopologyUpdatePlanner;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;

import java.util.Objects;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 在 worker 上推进一个维度的热状态。
 *
 * <p>先处理 ACK/意图并结算旧连接的供能，再准备和提交拓扑、重绑热源、
 * 释放旧节点，最后换热并发布查询和回执。Minecraft 对象不得越过 batch 边界
 * 进入该类。</p>
 */
public final class ThermalDimensionEngine implements ThermalDimensionProcessor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double FRONTIER_REFINE_HIGH_C = 1.0D;
    private static final double FRONTIER_RELEASE_LOW_C = 0.5D;
    private final long dimensionGeneration;
    private final ThermalTopologyParameters parameters;
    private final ThermalDimensionLimits limits;
    private final ThermalCellArena arena;
    private final QueryPublication queries;
    private final WorkerPageStore pages;
    private final PhaseTransitionRuntime phases;
    private final ThermalSolver solver;
    private final ThermalSourceLedger sources;
    private final WorkerPhysicalSourceBindings sourceBindings;
    private final TopologyUpdatePlanner topologyPlan;
    private final ContinuousAirSolver continuous;
    private final CutLoadBuffer cutLoads;

    private long lastBatchSequence;
    private long lastTargetTick;
    private int stableBatches;
    private boolean sleeping;
    private boolean closed;

    public ThermalDimensionEngine(
            long dimensionGeneration,
            long initialTick,
            ThermalCellArena arena,
            ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials,
            ThermalTopologyParameters parameters,
            FarFieldSettings farField,
            MinecraftPhysicalSourceProfile campfireProfile,
            ThermalDimensionLimits limits,
            QueryPublication queries
    ) {
        this(dimensionGeneration, initialTick, arena, signatures, materials, parameters,
                farField, campfireProfile, limits, queries, false);
    }

    public ThermalDimensionEngine(long dimensionGeneration, long initialTick, ThermalCellArena arena,
            ThermalSignatureTable signatures, MaterialBoundaryRegistry materials, ThermalTopologyParameters parameters,
            FarFieldSettings farField, MinecraftPhysicalSourceProfile campfireProfile,
            ThermalDimensionLimits limits, QueryPublication queries, boolean continuousAir) {
        if (dimensionGeneration < 0L || initialTick < 0L) {
            throw new IllegalArgumentException("engine identity is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.queries = Objects.requireNonNull(queries, "queries");
        ThermalSignatureTable catalog = Objects.requireNonNull(
                signatures, "signatures");
        pages = new WorkerPageStore(limits.maximumPages());
        pages.configureHotMaskThresholds(
                FRONTIER_REFINE_HIGH_C,
                FRONTIER_RELEASE_LOW_C);
        phases = new PhaseTransitionRuntime(
                arena, parameters.phaseRequestCapacity());
        solver = new ThermalSolver(
                arena,
                parameters.buoyancyParameters(),
                parameters.referenceTemperatureC(),
                64,
                16,
                128);
        sources = new ThermalSourceLedger(
                initialTick, 64, 3, limits.maximumSources(),
                new NodePowerAccumulatorArena(64, limits.maximumSourceNodes()), arena);
        AirLoadTable airLoads = continuousAir ? new AirLoadTable() : null;
        cutLoads = continuousAir ? new CutLoadBuffer() : null;
        sourceBindings = new WorkerPhysicalSourceBindings(
                pages, catalog, Objects.requireNonNull(campfireProfile, "campfireProfile"), airLoads);
        continuous = continuousAir ? new ContinuousAirSolver(arena, pages, solver, parameters, materials, airLoads, catalog) : null;
        BrickTopologyCompiler compiler = new BrickTopologyCompiler(
                arena, catalog,
                Objects.requireNonNull(materials, "materials"),
                parameters,
                Objects.requireNonNull(farField, "farField"),
                limits.maximumArenaSlots(), sourceBindings::collectMixingSources);
        if (continuous != null) compiler.captureSpatialAirContacts();
        topologyPlan = new TopologyUpdatePlanner(
                pages,
                arena,
                solver,
                phases,
                catalog,
                materials,
                compiler,
                parameters,
                limits,
                queries);
        lastTargetTick = initialTick;
    }

    public long routeVisitsLastCut() { return topologyPlan.routeVisitsLastCut(); }

    @Override
    public ThermalCompletion process(ThermalInputBatch batch) {
        Objects.requireNonNull(batch, "batch");
        requireOpen();
        validateBatch(batch);
        topologyPlan.beginCut();

        if (continuous != null) {
            continuous.acceptAdmissions(batch);
            sources.acceptAndRecord(batch.sourceEvents(), batch.targetTick(), sourceBindings, cutLoads);
            continuous.solveCut(lastTargetTick, batch.targetTick(), cutLoads);
            continuous.commit();
            sources.confirmRecordedDelivery(continuous.deliveredEnergyJ(), continuous.unacceptedEnergyJ());
        }

        for (ThermalInputBatch.PhaseAck ack : batch.phaseAcks()) {
            phases.applyAck(ack.request(), ack.outcome());
        }
        for (var intent : batch.phaseIntents()) {
            int slot = pages.materialSlot(intent.page(), intent.blockIndex(), intent.sourceStateId());
            if (slot >= 0) arena.forceMaterialTransition(slot, intent.branch());
        }
        boolean windChanged = batch.hasFarFieldConductanceScale();
        if (windChanged) {
            solver.updateWindScale(batch.farFieldConductanceScale());
        }

        // Source time is settled against the currently installed topology.
        // A topology replacement is prepared only after that settlement so
        // migration cannot overwrite energy delivered in this cut.
        if (continuous == null) sources.acceptAndAdvance(
                batch.sourceEvents(), batch.targetTick(), sourceBindings);
        pages.awaitChangedMaterials(batch, arena);

        PreparedTopologyChange topology = null;
        boolean workLimited = false;
        boolean topologyInput = topologyInputPresent(batch) || topologyPlan.hasPendingRoutes()
                || !sourceBindings.mixingChanges().isEmpty();
        if (topologyInput) {
            try {
                topology = topologyPlan.prepare(batch, sourceBindings.mixingChanges());
            } catch (TopologyUpdatePlanner.WorkLimitedException refused) {
                workLimited = true;
            }
        }

        if (topology != null) {
            TopologyCommitter.commit(topology, pages, arena, solver, phases);
            topologyPlan.committed();
            if (continuous != null) {
                continuous.rebuild(sourceBindings);
                continuous.commit();
                sourceBindings.markAllDirty();
            }
            sourceBindings.mixingCommitted();
        } else if (workLimited) {
            sourceBindings.markCommittedSections(topologyPlan.invalidatedRouteSourceSections());
            sourceBindings.rebindDirty(sources);
        }
        boolean queryPublished = false;
        try {
            if (topology != null) {
                sourceBindings.markCommittedSections(
                        topology.sourceDirtySections);
                sourceBindings.rebindDirty(sources);
                TopologyCommitter.releaseOldSpans(
                        topology, arena, solver, sources);
                for (PreparedTopologyChange.PageWrite write
                        : topology.pageWrites) {
                    if (!write.retirement
                            && write.publicationChangedBrickMask != 0L) {
                        queries.markInfraredBricksChanged(
                                write.publication.workerPageSlot(),
                                write.publicationChangedBrickMask,
                                batch.targetTick());
                    }
                }
            }

            boolean changed = topologyInput
                    || !batch.sourceEvents().isEmpty()
                    || batch.phaseAcks().length != 0
                    || batch.phaseIntents().length != 0
                    || windChanged;
            boolean sleepingAtStart = sleeping;
            if (changed || sources.hasActivePowerOrPendingEnergy()) {
                sleeping = false;
                stableBatches = 0;
            }
            long elapsedTicks = batch.targetTick() - lastTargetTick;
            boolean coalesced = elapsedTicks > ThermalInputBatch.CUT_INTERVAL_TICKS;
            ThermalSolver.StepStatus step = executeTransport(
                    elapsedTicks,
                    sleepingAtStart && !changed,
                    (batch.sequence() & 1L) != 0L);
            if (coalesced && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Coalesced thermal cut: dimensionGeneration={}, sequence={}, elapsedTicks={}, transportSeconds={}",
                        dimensionGeneration, batch.sequence(), elapsedTicks,
                        sleepingAtStart && !changed ? 0.0D : elapsedTicks / 20.0D);
            }
            phases.collectMaterialRequests();
            updateSleep(step, changed, coalesced);
            boolean unchangedSleeping = sleepingAtStart && sleeping && !changed;
            publish(batch, unchangedSleeping);
            queryPublished = true;
            ThermalCompletion.BrickResidency[] residencyUpdates =
                    workLimited || unchangedSleeping
                    ? ThermalCompletion.NO_RESIDENCY_UPDATES
                    : pages.collectResidencyChanges(
                            arena,
                            parameters.referenceTemperatureC(),
                            FRONTIER_REFINE_HIGH_C,
                            FRONTIER_RELEASE_LOW_C);

            lastBatchSequence = batch.sequence();
            lastTargetTick = batch.targetTick();
            return completion(
                    batch,
                    workLimited
                            ? ThermalCompletion.Status.WORK_LIMITED
                            : ThermalCompletion.Status.COMPLETED,
                    topology,
                    residencyUpdates);
        } catch (RuntimeException | Error failure) {
            if (topology != null && !queryPublished) {
                TopologyCommitter.restorePagePublications(topology);
            }
            throw failure;
        }
    }

    private void validateBatch(ThermalInputBatch batch) {
        if (batch.dimensionGeneration() != dimensionGeneration
                || batch.sequence() != lastBatchSequence + 1L
                || batch.targetTick() < lastTargetTick
                || batch.targetTick()
                        % ThermalInputBatch.CUT_INTERVAL_TICKS != 0L) {
            throw new IllegalArgumentException(
                    "thermal batch generation, sequence, or tick is stale");
        }
    }

    private ThermalSolver.StepStatus executeTransport(
            long elapsedTicks,
            boolean unchangedSleeping,
            boolean forward
    ) {
        if (continuous != null) return ThermalSolver.StepStatus.COMPLETED;
        if (elapsedTicks == 0L || unchangedSleeping) {
            return ThermalSolver.StepStatus.COMPLETED;
        }
        // The ledger has settled this entire interval. Advance exchange over the
        // same game time in one bounded pass; 20 ticks still selects the 1 s fast path.
        return solver.step(elapsedTicks / 20.0D, forward);
    }

    private void updateSleep(
            ThermalSolver.StepStatus step,
            boolean changed,
            boolean coalesced
    ) {
        if (continuous != null) {
            sleeping = false;
            stableBatches = 0;
            return;
        }
        if (step == ThermalSolver.StepStatus.NUMERIC_DEGRADED
                || coalesced
                || changed
                || sources.hasActivePowerOrPendingEnergy()) {
            sleeping = false;
            stableBatches = 0;
            return;
        }
        if (sleeping) {
            return;
        }
        if (++stableBatches < limits.stableBatchesBeforeSleep()) {
            return;
        }
        stableBatches = limits.stableBatchesBeforeSleep();
        sleeping = solver.maxTemperatureResidualC()
                <= limits.sleepResidualC();
    }

    private void publish(
            ThermalInputBatch batch,
            boolean unchangedSleeping
    ) {
        if (continuous != null && !queries.stageAir(continuous.layout(), continuous.coefficientsC(), parameters.referenceTemperatureC())) {
            throw new IllegalStateException("Continuous Air publication exceeded its query budget");
        }
        boolean published = unchangedSleeping
                && queries.republishUnchanged(
                        solver.structuralVersion(),
                        batch.targetTick());
        if (!published) {
            published = queries.publish(
                    arena,
                    parameters.referenceTemperatureC(),
                    solver.structuralVersion(),
                    batch.targetTick(),
                    pages.hotMaskScratch());
        }
        if (!published) {
            throw new IllegalStateException(
                    "prepared query publication could not be installed");
        }
    }

    private ThermalCompletion completion(
            ThermalInputBatch batch,
            ThermalCompletion.Status status,
            PreparedTopologyChange topology,
            ThermalCompletion.BrickResidency[] residencyUpdates
    ) {
        return new ThermalCompletion(
                dimensionGeneration,
                batch.sequence(),
                status,
                null,
                phases.drainRequests(
                        parameters.maximumPhaseMutationsPerCompletion()),
                topology == null
                        ? ThermalCompletion.NO_RESYNC_TOKENS
                        : topology.committedResyncTokens,
                residencyUpdates);
    }

    private static boolean topologyInputPresent(ThermalInputBatch batch) {
        return batch.admissions().length != 0
                || batch.retirements().length != 0
                || batch.residencyUpdates().length != 0
                || !batch.geometry().isEmpty()
                || batch.environmentUpdates().length != 0;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            sources.close();
        } finally {
            try {
                pages.close();
            } finally {
                queries.close();
            }
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("thermal dimension engine is closed");
        }
    }
}
