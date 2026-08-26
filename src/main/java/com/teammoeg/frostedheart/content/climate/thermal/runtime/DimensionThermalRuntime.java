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

    public enum SourceTopologyBarrierStatus {
        APPLIED,
        ALREADY_APPLIED,
        INPUTS_PENDING,
        FRAME_MISMATCH,
        UNAVAILABLE
    }

    public long dimensionGeneration() {
        return dimensionGeneration;
    }

    public synchronized boolean sleeping() {
        return sleeping;
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

    /** Acquires the same logical writer used by {@link #runOne()}. */
    public synchronized boolean tryBeginTopologyUpdate() {
        if (unloaded || failureLatched || logicalWriterOwned) {
            return false;
        }
        logicalWriterOwned = true;
        return true;
    }

    /**
     * Advances all non-source inputs as one logical-writer transaction. A
     * {@code null} replacement retains the installed sweep when topology did
     * not change.
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
            if (replacementSweep != null && !replacementSweep.targets(arena)) {
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
            if (replacementSweep != null) {
                replacementSweep.commitPendingFragmentPatch();
                sweep = replacementSweep;
            }
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
    public synchronized SourceTopologyBarrierStatus preApplySourcesForTopology(
            SealedInputFrame frame
    ) {
        Objects.requireNonNull(frame, "frame");
        if (!logicalWriterOwned || unloaded || failureLatched) {
            return SourceTopologyBarrierStatus.UNAVAILABLE;
        }
        Optional<SolveEpoch> candidate = scheduler.inFlight();
        if (candidate.isEmpty()) {
            candidate = scheduler.tryStartLatest();
        }
        if (candidate.isEmpty()) {
            return SourceTopologyBarrierStatus.INPUTS_PENDING;
        }
        SolveEpoch epoch = candidate.orElseThrow();
        if (epoch.targetTick() != frame.effectiveTick()
                || epoch.dimensionGeneration() != frame.dimensionGeneration()
                || !epoch.sealedWatermarks().equals(frame.watermarks())) {
            return SourceTopologyBarrierStatus.FRAME_MISMATCH;
        }
        if (sources.isPreApplied(epoch)) {
            return SourceTopologyBarrierStatus.ALREADY_APPLIED;
        }
        if (!sources.isReady(epoch)) {
            return SourceTopologyBarrierStatus.INPUTS_PENDING;
        }
        sources.preApplyForTopology(epoch);
        return SourceTopologyBarrierStatus.APPLIED;
    }

    public synchronized LatestSolveEpochScheduler.SealResult sealFrame(
            SealedInputFrame frame
    ) {
        return sealFrame(frame, false);
    }

    /** Seals a frame, allowing concrete input events to bypass steady cadence. */
    public synchronized LatestSolveEpochScheduler.SealResult sealFrame(
            SealedInputFrame frame,
            boolean urgent
    ) {
        if (unloaded) {
            return LatestSolveEpochScheduler.SealResult.GENERATION_MISMATCH;
        }
        LatestSolveEpochScheduler.SealResult result = scheduler.sealLatest(frame, urgent);
        if (result == LatestSolveEpochScheduler.SealResult.ACCEPTED) {
            if (!frame.watermarks().equals(latestSealedWatermarks)) {
                wakeLocked();
            }
            latestSealedWatermarks = frame.watermarks();
        }
        return result;
    }

    /** Runs at most one epoch under this runtime's logical-writer gate. */
    public void runOne() {
        synchronized (this) {
            if (logicalWriterOwned) {
                return;
            }
            logicalWriterOwned = true;
        }
        try {
            runOneOwned();
        } finally {
            synchronized (this) {
                logicalWriterOwned = false;
            }
        }
    }

    private void runOneOwned() {
        SolveEpoch epoch;
        InputWatermarks acknowledged;
        long acknowledgedGeometryRevision;
        long acknowledgedTopologyGeneration;
        boolean sleepingAtStart;
        synchronized (this) {
            if (unloaded) {
                return;
            }
            if (failureLatched) {
                return;
            }
            Optional<SolveEpoch> candidate = scheduler.inFlight();
            if (candidate.isEmpty()) {
                candidate = scheduler.tryStartLatest();
            }
            if (candidate.isEmpty()) {
                return;
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
            return;
        }
        if (!epoch.nonSourceInputsSatisfiedBy(dimensionGeneration, acknowledged)
                || !sources.isReady(epoch)) {
            return;
        }

        try {
            if (sleepingAtStart && !sources.hasActivePowerOrPendingEnergy()) {
                runSleepingEpoch(
                        epoch,
                        acknowledged,
                        acknowledgedGeometryRevision,
                        acknowledgedTopologyGeneration);
                return;
            }
            runActiveEpoch(
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
        }
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

    private void runSleepingEpoch(
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
                return;
            }
            LatestSolveEpochScheduler.CompletionResult completion = scheduler.complete(
                    epoch, dimensionGeneration, actualApplied);
            if (completion != LatestSolveEpochScheduler.CompletionResult.COMPLETED) {
                return;
            }
            appliedWatermarks = mergeAppliedWatermarks(
                    appliedWatermarks, actualApplied);
            sources.completePreApplied(epoch);
        }
        publication.republishUnchanged(
                dimensionGeneration,
                acknowledgedGeometryRevision,
                acknowledgedTopologyGeneration,
                epoch.epochId(),
                epoch.targetTick());
    }

    private void runActiveEpoch(
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
            return;
        }

        synchronized (this) {
            if (unloaded) {
                return;
            }
            LatestSolveEpochScheduler.CompletionResult completion = scheduler.complete(
                    epoch, dimensionGeneration, step.appliedWatermarks());
            if (completion != LatestSolveEpochScheduler.CompletionResult.COMPLETED) {
                return;
            }
            appliedWatermarks = mergeAppliedWatermarks(
                    appliedWatermarks, step.appliedWatermarks());
            sources.completePreApplied(epoch);
            updateSleepStateLocked(step);
        }
        boolean publicationReady = publication.tryEnsureCapacity(
                arena.highWaterMark());
        if (publicationReady) {
            publication.publish(
                arena,
                referenceTemperatureC,
                dimensionGeneration,
                acknowledgedGeometryRevision,
                acknowledgedTopologyGeneration,
                epoch.epochId(),
                epoch.targetTick(),
                0,
                arena.highWaterMark());
        }
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
