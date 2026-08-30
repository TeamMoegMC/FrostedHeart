/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;

import java.util.Objects;

/** Sole mutable worker authority for one dimension generation. */
final class ThermalDimensionEngine implements ThermalDimensionProcessor {
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
    private final TopologyPlan topologyPlan;
    private final TopologyCommitter topologyCommitter =
            new TopologyCommitter();

    private long lastBatchSequence;
    private long lastTargetTick;
    private int stableBatches;
    private boolean sleeping;
    private boolean closed;

    ThermalDimensionEngine(
            long dimensionGeneration,
            long initialTick,
            ThermalCellArena arena,
            ThermalSignatureRegistry signatures,
            MaterialBoundaryRegistry materials,
            ThermalTopologyParameters parameters,
            FarFieldSettings farField,
            ThermalDimensionLimits limits,
            QueryPublication queries
    ) {
        if (dimensionGeneration < 0L || initialTick < 0L) {
            throw new IllegalArgumentException("engine identity is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.queries = Objects.requireNonNull(queries, "queries");
        ThermalSignatureCatalog catalog = new ThermalSignatureCatalog(
                Objects.requireNonNull(signatures, "signatures"));
        pages = new WorkerPageStore(limits.maximumPages());
        phases = new PhaseTransitionRuntime(
                arena, parameters.phaseRequestCapacity());
        solver = new ThermalSolver(
                arena,
                phases,
                parameters.buoyancyParameters(),
                parameters.referenceTemperatureC(),
                64,
                16,
                128);
        BrickTopologyCompiler compiler = new BrickTopologyCompiler(
                arena, catalog,
                Objects.requireNonNull(materials, "materials"),
                parameters,
                Objects.requireNonNull(farField, "farField"),
                limits.maximumArenaSlots());
        topologyPlan = new TopologyPlan(
                pages,
                arena,
                solver,
                phases,
                catalog,
                compiler,
                parameters,
                limits,
                queries);
        sources = new ThermalSourceLedger(
                initialTick,
                64,
                3,
                limits.maximumSources(),
                new NodePowerAccumulatorArena(
                        64, limits.maximumSourceNodes()),
                arena);
        sourceBindings = new WorkerPhysicalSourceBindings(pages, catalog);
        lastTargetTick = initialTick;
    }

    @Override
    public ThermalCompletion process(ThermalInputBatch batch) {
        Objects.requireNonNull(batch, "batch");
        requireOpen();
        validateBatch(batch);

        for (ThermalInputBatch.PhaseAck ack : batch.phaseAcks()) {
            phases.applyAck(ack.request(), ack.outcome());
        }
        boolean windChanged = batch.hasFarFieldConductanceScale();
        if (windChanged) {
            solver.updateWindScale(batch.farFieldConductanceScale());
        }

        // Source time is settled against the currently installed topology.
        // A topology replacement is prepared only after that settlement so
        // migration cannot overwrite energy delivered in this cut.
        sources.acceptAndAdvance(
                batch.sourceEvents(), batch.targetTick(), sourceBindings);

        PreparedTopologyChange topology = null;
        boolean workLimited = false;
        boolean topologyInput = topologyInputPresent(batch);
        if (topologyInput) {
            try {
                topology = topologyPlan.prepare(batch);
            } catch (TopologyPlan.WorkLimitedException refused) {
                workLimited = true;
            }
        }

        if (topology != null) {
            topologyCommitter.commit(topology, pages, arena, solver, phases);
            sourceBindings.markCommittedSections(
                    topology.sourceDirtySections);
            sourceBindings.rebindDirty(sources);
            topologyCommitter.releaseOldSpans(
                    topology, arena, solver, sources);
        }

        boolean changed = topologyInput
                || !batch.sourceEvents().isEmpty()
                || batch.phaseAcks().length != 0
                || windChanged;
        boolean sleepingAtStart = sleeping;
        if (changed || sources.hasActivePowerOrPendingEnergy()) {
            sleeping = false;
            stableBatches = 0;
        }
        long elapsedTicks = batch.targetTick() - lastTargetTick;
        boolean timeDegraded = elapsedTicks != 0L
                && elapsedTicks != ThermalInputBatch.CUT_INTERVAL_TICKS;
        ThermalSolver.StepStatus step = executeTransport(
                elapsedTicks,
                sleepingAtStart && !changed,
                (batch.sequence() & 1L) != 0L);
        updateSleep(step, changed, timeDegraded);
        publish(batch, sleepingAtStart && sleeping && !changed);

        lastBatchSequence = batch.sequence();
        lastTargetTick = batch.targetTick();
        return completion(
                batch,
                workLimited
                        ? ThermalCompletion.Status.WORK_LIMITED
                        : ThermalCompletion.Status.COMPLETED,
                topology);
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
        if (elapsedTicks == 0L || unchangedSleeping) {
            return ThermalSolver.StepStatus.COMPLETED;
        }
        return solver.step(1.0D, forward);
    }

    private void updateSleep(
            ThermalSolver.StepStatus step,
            boolean changed,
            boolean timeDegraded
    ) {
        if (step == ThermalSolver.StepStatus.NUMERIC_DEGRADED
                || timeDegraded
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
        boolean published = unchangedSleeping
                && queries.republishUnchanged(
                        solver.structuralVersion(),
                        batch.targetTick());
        if (!published) {
            published = queries.publish(
                    arena,
                    parameters.referenceTemperatureC(),
                    solver.structuralVersion(),
                    batch.targetTick());
        }
        if (!published) {
            throw new IllegalStateException(
                    "prepared query publication could not be installed");
        }
    }

    private ThermalCompletion completion(
            ThermalInputBatch batch,
            ThermalCompletion.Status status,
            PreparedTopologyChange topology
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
                continuations(topology));
    }

    private static ThermalCompletion.PageContinuation[] continuations(
            PreparedTopologyChange topology
    ) {
        if (topology == null || topology.pageWrites.length == 0) {
            return ThermalCompletion.NO_CONTINUATIONS;
        }
        int count = 0;
        for (PreparedTopologyChange.PageWrite write : topology.pageWrites) {
            if (!write.retirement) {
                count++;
            }
        }
        ThermalCompletion.PageContinuation[] result =
                new ThermalCompletion.PageContinuation[count];
        int target = 0;
        for (PreparedTopologyChange.PageWrite write : topology.pageWrites) {
            if (write.retirement) {
                continue;
            }
            result[target++] = new ThermalCompletion.PageContinuation(
                    write.page.handle.sectionKey(),
                    write.page.handle.lifecycleGeneration(),
                    write.publication.geometryRevision(),
                    write.publication.topologyGeneration(),
                    write.continuationFaceMask);
        }
        return result;
    }

    private static boolean topologyInputPresent(ThermalInputBatch batch) {
        return batch.admissions().length != 0
                || batch.retirements().length != 0
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
