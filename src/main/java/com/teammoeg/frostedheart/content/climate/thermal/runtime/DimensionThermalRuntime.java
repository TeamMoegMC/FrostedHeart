/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SolveEpoch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalStepExecutor;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalStepPlan;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;

import java.util.Objects;
import java.util.Optional;

/**
 * Single logical writer for one dimension lifecycle. Main-thread sealing and
 * acknowledgement are brief synchronized updates; source integration, sweep,
 * and publication remain worker-owned.
 */
public final class DimensionThermalRuntime implements AutoCloseable {
    private final long runtimeId;
    private final long dimensionGeneration;
    private final ThermalCellArena arena;
    private final ThermalSourceTimeline sources;
    private ThermalSweep sweep;
    private final QueryPublication publication;
    private final ThermalTimePolicy timePolicy;
    private final LatestSolveEpochScheduler scheduler;
    private final double referenceTemperatureC;
    private final Limits limits;

    private InputWatermarks appliedWatermarks;
    private InputWatermarks latestSealedWatermarks;
    private long geometryRevision;
    private long topologyGeneration;
    private boolean topologyResolved;
    private int stableEpochCount;
    private boolean sleeping;
    private boolean unloaded;
    private boolean failureLatched;
    private boolean logicalWriterOwned;

    public DimensionThermalRuntime(
            long runtimeId,
            long dimensionGeneration,
            long initialCompletedTick,
            InputWatermarks initialAppliedWatermarks,
            long initialGeometryRevision,
            long initialTopologyGeneration,
            boolean initialTopologyResolved,
            ThermalTimePolicy timePolicy,
            ThermalCellArena arena,
            ThermalSourceTimeline sources,
            ThermalSweep sweep,
            QueryPublication publication,
            double referenceTemperatureC,
            Limits limits
    ) {
        if (dimensionGeneration < 0L || initialCompletedTick < 0L
                || initialGeometryRevision < 0L || initialTopologyGeneration < 0L) {
            throw new IllegalArgumentException(
                    "generation, tick, and revisions must be non-negative");
        }
        if (!Double.isFinite(referenceTemperatureC)) {
            throw new IllegalArgumentException("referenceTemperatureC must be finite");
        }
        this.runtimeId = runtimeId;
        this.dimensionGeneration = dimensionGeneration;
        this.timePolicy = Objects.requireNonNull(timePolicy, "timePolicy");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.sources = Objects.requireNonNull(sources, "sources");
        this.sweep = Objects.requireNonNull(sweep, "sweep");
        this.publication = Objects.requireNonNull(publication, "publication");
        this.limits = Objects.requireNonNull(limits, "limits");
        if (!sources.targets(arena) || !sweep.targets(arena)) {
            throw new IllegalArgumentException(
                    "runtime source timeline and sweep must own the same arena");
        }
        if (publication.capacity() < arena.highWaterMark()) {
            throw new IllegalArgumentException(
                    "publication capacity must cover the arena high-water mark");
        }
        this.referenceTemperatureC = referenceTemperatureC;
        this.appliedWatermarks = Objects.requireNonNull(
                initialAppliedWatermarks, "initialAppliedWatermarks");
        this.latestSealedWatermarks = initialAppliedWatermarks;
        this.geometryRevision = initialGeometryRevision;
        this.topologyGeneration = initialTopologyGeneration;
        this.topologyResolved = initialTopologyResolved;
        this.scheduler = new LatestSolveEpochScheduler(
                dimensionGeneration,
                initialCompletedTick,
                initialAppliedWatermarks,
                timePolicy);
    }

    public record Limits(
            int maxActiveCells,
            int maxPairOperations,
            int maxBoundaryOperations,
            int stableEpochsBeforeSleep,
            double sleepResidualEpsilonC
    ) {
        public Limits {
            if (maxActiveCells < 0
                    || maxPairOperations < 0
                    || maxBoundaryOperations < 0
                    || stableEpochsBeforeSleep <= 0
                    || !Double.isFinite(sleepResidualEpsilonC)
                    || sleepResidualEpsilonC < 0.0D) {
                throw new IllegalArgumentException("runtime limits are invalid");
            }
        }
    }

    public enum AcknowledgeResult {
        APPLIED,
        DUPLICATE,
        GENERATION_MISMATCH,
        WATERMARK_REGRESSION,
        REVISION_REGRESSION,
        UNLOADED
    }

    public enum RunStatus {
        COMPLETED,
        SLEEP_SKIPPED,
        NO_WORK,
        INPUTS_PENDING,
        WORK_LIMIT_EXCEEDED,
        RECOVERY_REQUIRED,
        STALE_GENERATION
    }

    public enum SourceTopologyBarrierStatus {
        APPLIED,
        ALREADY_APPLIED,
        INPUTS_PENDING,
        FRAME_MISMATCH,
        UNAVAILABLE
    }

    public record SourceTopologyBarrierReport(
            SourceTopologyBarrierStatus status,
            double sourceAppliedJ
    ) {
    }

    public record RunReport(
            RunStatus status,
            long epochId,
            boolean published,
            boolean sleeping,
            ThermalStepExecutor.Report thermalStep
    ) {
        private static RunReport withoutStep(
                RunStatus status,
                long epochId,
                boolean sleeping
        ) {
            return new RunReport(status, epochId, false, sleeping, null);
        }
    }

    /** Stable, non-blocking diagnostic cut for the shadow runtime. */
    public record Diagnostics(
            boolean writerBusy,
            boolean unloaded,
            boolean failureLatched,
            boolean sleeping,
            long lastCompletedTargetTick,
            InputWatermarks appliedWatermarks,
            InputWatermarks latestSealedWatermarks,
            long geometryRevision,
            long topologyGeneration,
            boolean topologyResolved,
            int arenaCapacity,
            int arenaHighWaterMark,
            int liveCellCount,
            int pairOperationCount,
            int boundaryOperationCount,
            int phaseOperationCount,
            int publicationCapacity,
            long publicationReservedBytes
    ) {
    }

    public long runtimeId() {
        return runtimeId;
    }

    public long dimensionGeneration() {
        return dimensionGeneration;
    }

    public synchronized boolean unloaded() {
        return unloaded;
    }

    public synchronized boolean sleeping() {
        return sleeping;
    }

    public synchronized boolean failureLatched() {
        return failureLatched;
    }

    public synchronized long lastCompletedTargetTick() {
        return scheduler.lastCompletedTargetTick();
    }

    public synchronized InputWatermarks appliedWatermarks() {
        return appliedWatermarks;
    }

    public synchronized long geometryRevision() {
        return geometryRevision;
    }

    public synchronized long topologyGeneration() {
        return topologyGeneration;
    }

    public synchronized boolean topologyResolved() {
        return topologyResolved;
    }

    public ThermalCellArena thermalCellArena() {
        return arena;
    }

    /**
     * Never waits for the logical writer. Mutable arena/sweep counts are
     * unavailable while a worker owns them, rather than being read racy.
     */
    public synchronized Diagnostics diagnostics() {
        boolean busy = logicalWriterOwned;
        return new Diagnostics(
                busy,
                unloaded,
                failureLatched,
                sleeping,
                scheduler.lastCompletedTargetTick(),
                appliedWatermarks,
                latestSealedWatermarks,
                geometryRevision,
                topologyGeneration,
                topologyResolved,
                busy ? -1 : arena.capacity(),
                busy ? -1 : arena.highWaterMark(),
                busy ? -1 : arena.liveCellCount(),
                busy ? -1 : sweep.pairOperationCount(),
                busy ? -1 : sweep.boundaryOperationCount(),
                busy ? -1 : sweep.phaseOperationCount(),
                publication.capacity(),
                publication.reservedBytes());
    }

    /**
     * Reads one cell only when the publication still matches the runtime's
     * complete dimension topology cut. Callers must also resolve the slot from
     * current published Page geometry before entering this method.
     */
    public synchronized boolean tryReadPublishedCell(
            int arenaSlot,
            QueryPublication.MutableSample out
    ) {
        Objects.requireNonNull(out, "out");
        if (arenaSlot < 0 || unloaded || failureLatched || logicalWriterOwned) {
            return false;
        }
        return publication.tryRead(
                arenaSlot, dimensionGeneration, geometryRevision, out)
                && out.lifecycleGeneration() == dimensionGeneration
                && out.geometryRevision() == geometryRevision
                && out.topologyGeneration() == topologyGeneration;
    }

    /** Checks only bindings that can keep one concrete arena span alive. */
    public boolean sourceTopologyReferences(ArenaSpan span) {
        Objects.requireNonNull(span, "span");
        return span.count() != 0 && sources.mayReferenceThermalNodeRange(
                span.firstSlot(), span.endSlotExclusive());
    }

    public synchronized int sweepPairOperationCount() {
        return sweep.pairOperationCount();
    }

    public synchronized int sweepBoundaryOperationCount() {
        return sweep.boundaryOperationCount();
    }

    public synchronized int sweepPhaseOperationCount() {
        return sweep.phaseOperationCount();
    }

    /** Acquires the same logical writer used by {@link #runOne()}. */
    public synchronized boolean tryBeginTopologyUpdate() {
        if (unloaded || failureLatched || logicalWriterOwned) {
            return false;
        }
        logicalWriterOwned = true;
        return true;
    }

    /**
     * Installs one arena-bound sweep and advances all non-source inputs as one
     * logical-writer transaction.
     */
    public synchronized AcknowledgeResult finishTopologyUpdate(
            long appliedDimensionGeneration,
            InputWatermarks acknowledgedWatermarks,
            long acknowledgedGeometryRevision,
            long acknowledgedTopologyGeneration,
            boolean acknowledgedTopologyResolved,
            ThermalSweep replacementSweep
    ) {
        if (!logicalWriterOwned) {
            throw new IllegalStateException("topology update does not own the logical writer");
        }
        try {
            if (unloaded) {
                return AcknowledgeResult.UNLOADED;
            }
            if (appliedDimensionGeneration != dimensionGeneration) {
                return AcknowledgeResult.GENERATION_MISMATCH;
            }
            Objects.requireNonNull(acknowledgedWatermarks, "acknowledgedWatermarks");
            Objects.requireNonNull(replacementSweep, "replacementSweep");
            if (!replacementSweep.targets(arena)) {
                throw new IllegalArgumentException("replacement sweep must own the runtime arena");
            }
            if (!acknowledgedWatermarks.coversNonSourceStreams(appliedWatermarks)) {
                return AcknowledgeResult.WATERMARK_REGRESSION;
            }
            if (acknowledgedGeometryRevision < geometryRevision
                    || acknowledgedTopologyGeneration < topologyGeneration) {
                return AcknowledgeResult.REVISION_REGRESSION;
            }
            boolean changed = !appliedWatermarks.coversNonSourceStreams(
                    acknowledgedWatermarks)
                    || acknowledgedGeometryRevision != geometryRevision
                    || acknowledgedTopologyGeneration != topologyGeneration
                    || acknowledgedTopologyResolved != topologyResolved;
            sweep = replacementSweep;
            if (!changed) {
                return AcknowledgeResult.DUPLICATE;
            }
            appliedWatermarks = new InputWatermarks(
                    acknowledgedWatermarks.geometry(),
                    appliedWatermarks.source(),
                    acknowledgedWatermarks.chunk(),
                    acknowledgedWatermarks.profile(),
                    acknowledgedWatermarks.transitionAck());
            geometryRevision = acknowledgedGeometryRevision;
            topologyGeneration = acknowledgedTopologyGeneration;
            topologyResolved = acknowledgedTopologyResolved;
            wakeLocked();
            return AcknowledgeResult.APPLIED;
        } finally {
            logicalWriterOwned = false;
        }
    }

    public synchronized void cancelTopologyUpdate() {
        if (!logicalWriterOwned) {
            throw new IllegalStateException("topology update does not own the logical writer");
        }
        logicalWriterOwned = false;
    }

    /** Main-thread frame producers seal the complete source cut offered so far. */
    public long latestOfferedSourceWatermark() {
        return sources.latestOfferedWatermark();
    }

    public ThermalSourceTimeline sourceTimeline() {
        return sources;
    }

    /** Settles old source bindings at a topology cut while the logical writer is held. */
    public synchronized SourceTopologyBarrierReport preApplySourcesForTopology(
            SealedInputFrame frame
    ) {
        Objects.requireNonNull(frame, "frame");
        if (!logicalWriterOwned || unloaded || failureLatched) {
            return new SourceTopologyBarrierReport(
                    SourceTopologyBarrierStatus.UNAVAILABLE, 0.0D);
        }
        Optional<SolveEpoch> candidate = scheduler.inFlight();
        if (candidate.isEmpty()) {
            candidate = scheduler.tryStartLatest();
        }
        if (candidate.isEmpty()) {
            return new SourceTopologyBarrierReport(
                    SourceTopologyBarrierStatus.INPUTS_PENDING, 0.0D);
        }
        SolveEpoch epoch = candidate.orElseThrow();
        if (epoch.targetTick() != frame.effectiveTick()
                || epoch.dimensionGeneration() != frame.dimensionGeneration()
                || !epoch.sealedWatermarks().equals(frame.watermarks())) {
            return new SourceTopologyBarrierReport(
                    SourceTopologyBarrierStatus.FRAME_MISMATCH, 0.0D);
        }
        if (sources.isPreApplied(epoch)) {
            return new SourceTopologyBarrierReport(
                    SourceTopologyBarrierStatus.ALREADY_APPLIED,
                    sources.preAppliedEnergyJ(epoch));
        }
        if (!sources.isReady(epoch)) {
            return new SourceTopologyBarrierReport(
                    SourceTopologyBarrierStatus.INPUTS_PENDING, 0.0D);
        }
        return new SourceTopologyBarrierReport(
                SourceTopologyBarrierStatus.APPLIED,
                sources.preApplyForTopology(epoch));
    }

    public synchronized LatestSolveEpochScheduler.SealResult sealFrame(
            SealedInputFrame frame
    ) {
        if (unloaded) {
            return LatestSolveEpochScheduler.SealResult.GENERATION_MISMATCH;
        }
        LatestSolveEpochScheduler.SealResult result = scheduler.sealLatest(frame);
        if (result == LatestSolveEpochScheduler.SealResult.ACCEPTED) {
            if (!frame.watermarks().equals(latestSealedWatermarks)) {
                wakeLocked();
            }
            latestSealedWatermarks = frame.watermarks();
        }
        return result;
    }

    /**
     * Explicitly acknowledges concrete non-source stream application. Source
     * readiness remains owned by ThermalSourceTimeline.
     */
    public synchronized AcknowledgeResult acknowledgeNonSourceInputs(
            long appliedDimensionGeneration,
            InputWatermarks acknowledgedWatermarks,
            long acknowledgedGeometryRevision,
            long acknowledgedTopologyGeneration,
            boolean acknowledgedTopologyResolved
    ) {
        if (unloaded) {
            return AcknowledgeResult.UNLOADED;
        }
        if (appliedDimensionGeneration != dimensionGeneration) {
            return AcknowledgeResult.GENERATION_MISMATCH;
        }
        Objects.requireNonNull(acknowledgedWatermarks, "acknowledgedWatermarks");
        if (!acknowledgedWatermarks.coversNonSourceStreams(appliedWatermarks)) {
            return AcknowledgeResult.WATERMARK_REGRESSION;
        }
        if (acknowledgedGeometryRevision < geometryRevision
                || acknowledgedTopologyGeneration < topologyGeneration) {
            return AcknowledgeResult.REVISION_REGRESSION;
        }
        boolean changed = !appliedWatermarks.coversNonSourceStreams(
                acknowledgedWatermarks)
                || acknowledgedGeometryRevision != geometryRevision
                || acknowledgedTopologyGeneration != topologyGeneration
                || acknowledgedTopologyResolved != topologyResolved;
        if (!changed) {
            return AcknowledgeResult.DUPLICATE;
        }
        appliedWatermarks = new InputWatermarks(
                acknowledgedWatermarks.geometry(),
                appliedWatermarks.source(),
                acknowledgedWatermarks.chunk(),
                acknowledgedWatermarks.profile(),
                acknowledgedWatermarks.transitionAck());
        geometryRevision = acknowledgedGeometryRevision;
        topologyGeneration = acknowledgedTopologyGeneration;
        topologyResolved = acknowledgedTopologyResolved;
        wakeLocked();
        return AcknowledgeResult.APPLIED;
    }

    /** Runs at most one epoch. The coordinator guarantees one concurrent caller. */
    public RunReport runOne() {
        synchronized (this) {
            if (logicalWriterOwned) {
                return RunReport.withoutStep(RunStatus.NO_WORK, -1L, sleeping);
            }
            logicalWriterOwned = true;
        }
        try {
            return runOneOwned();
        } finally {
            synchronized (this) {
                logicalWriterOwned = false;
            }
        }
    }

    private RunReport runOneOwned() {
        SolveEpoch epoch;
        InputWatermarks acknowledged;
        long acknowledgedGeometryRevision;
        long acknowledgedTopologyGeneration;
        boolean sleepingAtStart;
        synchronized (this) {
            if (unloaded) {
                return RunReport.withoutStep(
                        RunStatus.STALE_GENERATION, -1L, false);
            }
            if (failureLatched) {
                return RunReport.withoutStep(
                        RunStatus.RECOVERY_REQUIRED, -1L, false);
            }
            Optional<SolveEpoch> candidate = scheduler.inFlight();
            if (candidate.isEmpty()) {
                candidate = scheduler.tryStartLatest();
            }
            if (candidate.isEmpty()) {
                return RunReport.withoutStep(RunStatus.NO_WORK, -1L, sleeping);
            }
            epoch = candidate.get();
            acknowledged = appliedWatermarks;
            acknowledgedGeometryRevision = geometryRevision;
            acknowledgedTopologyGeneration = topologyGeneration;
            sleepingAtStart = sleeping
                    && epoch.sealedWatermarks().equals(
                            scheduler.lastCompletedWatermarks());
        }

        if (!workWithinLimits()) {
            return RunReport.withoutStep(
                    RunStatus.WORK_LIMIT_EXCEEDED, epoch.epochId(), false);
        }
        if (!epoch.nonSourceInputsSatisfiedBy(dimensionGeneration, acknowledged)
                || !sources.isReady(epoch)) {
            return RunReport.withoutStep(
                    RunStatus.INPUTS_PENDING, epoch.epochId(), sleepingAtStart);
        }

        try {
            if (sleepingAtStart && !sources.hasActivePowerOrPendingEnergy()) {
                return runSleepingEpoch(
                        epoch,
                        acknowledged,
                        acknowledgedGeometryRevision,
                        acknowledgedTopologyGeneration);
            }
            return runActiveEpoch(
                    epoch,
                    acknowledged,
                    acknowledgedGeometryRevision,
                    acknowledgedTopologyGeneration);
        } catch (RuntimeException exception) {
            synchronized (this) {
                failureLatched = true;
                sleeping = false;
                stableEpochCount = 0;
            }
            publication.invalidate();
            return RunReport.withoutStep(
                    RunStatus.RECOVERY_REQUIRED, epoch.epochId(), false);
        }
    }

    public synchronized boolean hasReadyWork() {
        if (unloaded || failureLatched || logicalWriterOwned || !workWithinLimits()) {
            return false;
        }
        Optional<SolveEpoch> inFlight = scheduler.inFlight();
        if (inFlight.isPresent()) {
            SolveEpoch epoch = inFlight.get();
            return epoch.nonSourceInputsSatisfiedBy(
                    dimensionGeneration, appliedWatermarks)
                    && sources.isReady(epoch);
        }
        return scheduler.canStartLatest();
    }

    /** Main-thread unload invalidates publication before stale work can commit. */
    public void unload() {
        synchronized (this) {
            if (unloaded) {
                return;
            }
            unloaded = true;
            sleeping = false;
        }
        publication.retire();
    }

    @Override
    public void close() {
        unload();
        publication.close();
    }

    private RunReport runSleepingEpoch(
            SolveEpoch epoch,
            InputWatermarks acknowledged,
            long acknowledgedGeometryRevision,
            long acknowledgedTopologyGeneration
    ) {
        double sourceEnergy = sources.apply(
                epoch, epoch.previousTick(), epoch.targetTick());
        if (sourceEnergy != 0.0D || sources.hasActivePowerOrPendingEnergy()) {
            throw new IllegalStateException(
                    "a sleeping solve set received physical source energy");
        }
        InputWatermarks actualApplied = acknowledged.withSource(
                sources.appliedWatermark());
        synchronized (this) {
            if (unloaded) {
                return RunReport.withoutStep(
                        RunStatus.STALE_GENERATION, epoch.epochId(), false);
            }
            LatestSolveEpochScheduler.CompletionResult completion = scheduler.complete(
                    epoch, dimensionGeneration, actualApplied);
            if (completion != LatestSolveEpochScheduler.CompletionResult.COMPLETED) {
                return RunReport.withoutStep(
                        RunStatus.INPUTS_PENDING, epoch.epochId(), sleeping);
            }
            appliedWatermarks = mergeAppliedWatermarks(
                    appliedWatermarks, actualApplied);
            sources.completePreApplied(epoch);
        }
        boolean published = publication.republishUnchanged(
                dimensionGeneration,
                acknowledgedGeometryRevision,
                acknowledgedTopologyGeneration,
                epoch.epochId(),
                epoch.targetTick());
        return new RunReport(
                RunStatus.SLEEP_SKIPPED,
                epoch.epochId(),
                published,
                true,
                null);
    }

    private RunReport runActiveEpoch(
            SolveEpoch epoch,
            InputWatermarks acknowledged,
            long acknowledgedGeometryRevision,
            long acknowledgedTopologyGeneration
    ) {
        ThermalStepPlan plan = timePolicy.plan(epoch);
        ThermalStepExecutor.Report step = ThermalStepExecutor.execute(
                plan,
                dimensionGeneration,
                acknowledged,
                arena,
                sources,
                sweep,
                referenceTemperatureC);
        if (step.status() == ThermalStepExecutor.Status.INPUTS_PENDING) {
            return new RunReport(
                    RunStatus.INPUTS_PENDING,
                    epoch.epochId(),
                    false,
                    false,
                    step);
        }

        boolean nowSleeping;
        synchronized (this) {
            if (unloaded) {
                return new RunReport(
                        RunStatus.STALE_GENERATION,
                        epoch.epochId(),
                        false,
                        false,
                        step);
            }
            LatestSolveEpochScheduler.CompletionResult completion = scheduler.complete(
                    epoch, dimensionGeneration, step.appliedWatermarks());
            if (completion != LatestSolveEpochScheduler.CompletionResult.COMPLETED) {
                return new RunReport(
                        RunStatus.INPUTS_PENDING,
                        epoch.epochId(),
                        false,
                        false,
                        step);
            }
            appliedWatermarks = mergeAppliedWatermarks(
                    appliedWatermarks, step.appliedWatermarks());
            sources.completePreApplied(epoch);
            updateSleepStateLocked(step);
            nowSleeping = sleeping;
        }
        boolean published = publication.publish(
                arena,
                referenceTemperatureC,
                dimensionGeneration,
                acknowledgedGeometryRevision,
                acknowledgedTopologyGeneration,
                epoch.epochId(),
                epoch.targetTick(),
                0,
                arena.highWaterMark());
        return new RunReport(
                RunStatus.COMPLETED,
                epoch.epochId(),
                published,
                nowSleeping,
                step);
    }

    private boolean workWithinLimits() {
        return arena.liveCellCount() <= limits.maxActiveCells()
                && sweep.pairOperationCount() <= limits.maxPairOperations()
                && sweep.boundaryOperationCount() + sweep.phaseOperationCount()
                <= limits.maxBoundaryOperations();
    }

    private void updateSleepStateLocked(ThermalStepExecutor.Report step) {
        boolean stable = step.status() == ThermalStepExecutor.Status.COMPLETED
                && step.timeStatus() == ThermalStepPlan.Status.NORMAL
                && appliedWatermarks.equals(step.appliedWatermarks())
                && scheduler.pendingTargetCount() == 0
                && topologyResolved
                && !sources.hasActivePowerOrPendingEnergy()
                && sweep.maxTemperatureResidualC(referenceTemperatureC)
                        <= limits.sleepResidualEpsilonC();
        if (!stable) {
            wakeLocked();
            return;
        }
        stableEpochCount++;
        sleeping = stableEpochCount >= limits.stableEpochsBeforeSleep();
    }

    private void wakeLocked() {
        stableEpochCount = 0;
        sleeping = false;
    }

    private static InputWatermarks mergeAppliedWatermarks(
            InputWatermarks current,
            InputWatermarks completed
    ) {
        return new InputWatermarks(
                Math.max(current.geometry(), completed.geometry()),
                Math.max(current.source(), completed.source()),
                Math.max(current.chunk(), completed.chunk()),
                Math.max(current.profile(), completed.profile()),
                Math.max(current.transitionAck(), completed.transitionAck()));
    }
}
