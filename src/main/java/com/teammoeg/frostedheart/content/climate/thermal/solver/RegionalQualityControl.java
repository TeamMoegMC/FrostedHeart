/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.LinkedHashSet;

/** Sole coordinator for quality intent and candidate ownership. Cost history has no execution authority. */
public final class RegionalQualityControl {
    public enum Model { FINE, MINIMUM }
    record Intent(Model target, String reason) { }
    public record Change(long regionId, long epoch, long tick, ThermalPageHandle page, LongSet sections, Model target) { }
    private final RegionalAirRuntime runtime;
    private final RegionalWorkQueue work;
    private final WorkerPageStore pages;
    private final LinkedHashSet<ThermalRegion> ready = new LinkedHashSet<>();
    private Change recovery;
    private long nextRecoveryCheck;

    RegionalQualityControl(RegionalAirRuntime runtime, RegionalWorkQueue work, WorkerPageStore pages) {
        this.runtime = runtime; this.work = work; this.pages = pages;
    }
    void reconsider(ThermalRegion region) { ready.add(region); }
    void resourcesReturned(ThermalRegion region) { ready.add(region); }
    void clear() { ready.clear(); recovery = null; }
    void retired(ThermalRegion region) {
        ready.remove(region);
        if (region.qualityCandidate == recovery) recovery = null;
        region.qualityCandidate = null;
    }
    static boolean minimumPending(ThermalRegion region) {
        return region.qualityIntent != null && region.qualityIntent.target == Model.MINIMUM
                && region.accepted().field() != null && region.accepted().field().minimumSummary() != null
                && !region.accepted().field().minimumQuality();
    }
    static boolean recovering(ThermalRegion region) {
        return region.qualityCandidate != null && region.qualityCandidate.target == Model.FINE
                && region.qualityIntent != null && region.qualityIntent.target == Model.FINE;
    }
    static boolean holdsNumericalWork(ThermalRegion region) {
        return minimumPending(region) || region.qualityCandidate != null && region.qualityCandidate.target == Model.MINIMUM;
    }
    static boolean requestsChange(ThermalRegion region) {
        return region.qualityIntent != null && (region.qualityIntent.target == Model.FINE ? 1 : 4) != region.solver.gridStep();
    }
    /** Coordinator entry used by explicit comparison runs; it submits the same intent as the cost policy. */
    public boolean requestAt(int x, int y, int z, Model target) {
        for (var region : runtime.observedRegions()) if (region.solver.layout().componentAt(x, y, z) != null) {
            if (target == Model.MINIMUM) minimum(region, "explicit_request");
            else {
                region.qualityIntent = new Intent(target, "explicit_request");
                if (region.job != null && region.job.qualityOnly()) work.cancel(region.job);
                ready.add(region);
            }
            return true;
        }
        return false;
    }
    boolean hasReadyWork() {
        long tick = runtime.receivedThrough();
        for (var region : ready) {
            if (minimumReady(region)) return true;
            if (minimumPending(region) || !region.installed || region.retired || region.snapshotPauses != 0
                    || region.qualityCandidate != null || region.executionLimit() != Long.MAX_VALUE) continue;
            int step = selectedStep(region, tick);
            if (step != region.solver.gridStep()
                    && (step == 4 ? region.job == null : recoveryEligible(region, tick))) return true;
        }
        return false;
    }
    private int selectedStep(ThermalRegion region, long tick) {
        return requestsChange(region) ? region.qualityIntent.target == Model.FINE ? 1 : 4
                : ThermalWorkBudget.automatic() ? runtime.automaticStep(region, tick, runtime.regionWorkAllowance(region)) : region.solver.gridStep();
    }
    private boolean minimumReady(ThermalRegion region) {
        return minimumPending(region) && region.job == null && region.snapshotPauses == 0
                && region.changedMaterials.isEmpty() && region.materialControls == null;
    }
    private ThermalRegion owner(Change change) {
        if (change == null) return null;
        for (var region : runtime.observedRegions()) if (region.id == change.regionId && region.epoch == change.epoch) return region;
        return null;
    }
    public boolean current(Change change) {
        var region = owner(change);
        return region != null && region.qualityCandidate == change && region.qualityIntent != null
                && region.qualityIntent.target == change.target
                && (change.target != Model.FINE || !region.pending.hasAggregatedInput());
    }
    /** Called only after this candidate's actual helpers and workspace have been returned. */
    public void abandon(Change change) {
        var region = owner(change);
        if (region != null && region.qualityCandidate == change) {
            region.qualityCandidate = null;
            if (change.target == Model.FINE && region.workPolicy != null) region.workPolicy.coarse(runtime.receivedThrough(), true);
            region.qualityIntent = new Intent(Model.MINIMUM, "candidate_cancelled");
            work.offer(region);
        }
        if (recovery == change) recovery = null;
    }
    public Change next() {
        long tick = runtime.receivedThrough();
        if (ThermalWorkBudget.automatic() && tick >= nextRecoveryCheck) {
            nextRecoveryCheck = tick + ThermalInputBatch.CUT_INTERVAL_TICKS;
            for (var region : runtime.observedRegions()) if (region.solver.gridStep() == 4) ready.add(region);
        }
        int remaining = Math.min(4, ready.size());
        while (remaining-- > 0 && !ready.isEmpty()) {
            var iterator = ready.iterator(); var region = iterator.next(); iterator.remove();
            if (!region.installed || region.retired) continue;
            if (minimumPending(region)) {
                if (region.job != null && !region.job.qualityOnly()) work.cancel(region.job);
                else if (minimumReady(region)) work.offer(region);
                if (region.job == null) ready.add(region);
                continue;
            }
            if (region.qualityCandidate != null) continue;
            if (region.snapshotPauses != 0 || region.executionLimit() != Long.MAX_VALUE) { ready.add(region); continue; }
            int step = selectedStep(region, tick);
            if (step == region.solver.gridStep()) continue;
            if (step == 4 && region.accepted().field().minimumSummary() != null) {
                minimum(region, "quality_policy"); continue;
            }
            if (step == 1) {
                if (!recoveryEligible(region, tick)) { ready.add(region); continue; }
            } else if (region.job != null && !work.cancel(region.job)) { ready.add(region); continue; }
            ThermalPageHandle page = null;
            for (long section : region.scope.sections()) {
                var current = pages.pageAt(section); if (current != null) { page = current.handle; break; }
            }
            if (page == null) continue;
            var target = step == 1 ? Model.FINE : Model.MINIMUM;
            if (!requestsChange(region)) region.qualityIntent = new Intent(target, "cost_policy");
            var change = new Change(region.id, region.epoch, region.sampleTick(), page, new LongOpenHashSet(region.scope.sections()), target);
            region.qualityCandidate = change;
            if (target == Model.FINE) recovery = change;
            return change;
        }
        return null;
    }
    private boolean recoveryEligible(ThermalRegion region, long tick) {
        return recovery == null && tick - region.sampleTick() <= 20 && !region.pending.hasAggregatedInput()
                && (region.job == null || !region.job.cancelled);
    }
    void minimum(ThermalRegion region, String reason) {
        boolean first = !minimumPending(region);
        region.qualityIntent = new Intent(Model.MINIMUM, reason);
        if (region.workPolicy == null) region.workPolicy = new RegionWorkPolicy();
        region.workPolicy.coarse(runtime.receivedThrough(), false);
        if (first && region.accepted().field() != null && !region.accepted().field().minimumQuality())
            FHMain.LOGGER.info("THERMAL_REGION_MINIMUM_REQUEST,receivedTick={},regionId={},reason={},allowanceNs={},jobActiveNs={},policy={}",
                    runtime.receivedThrough(), region.id, reason, runtime.regionWorkAllowance(region),
                    region.job == null || region.job.account == null ? -1 : region.job.account.activeNanos(), region.workPolicy.summary());
        if (region.accepted().field() != null && region.accepted().field().minimumSummary() != null) {
            if (region.job != null && !region.job.qualityOnly()) work.cancel(region.job);
        }
        ready.add(region);
        work.offer(region);
    }

    /** All capacity exits use this owner; they never cancel unrelated preparation. */
    void resourcePressure(ThermalRegion region) {
        if (ThermalWorkBudget.automatic()) minimum(region, "resource_pressure");
    }
    public void resourcePressure(com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary boundary) {
        if (!ThermalWorkBudget.automatic()) return;
        ThermalRegion selected = null;
        boolean local = false;
        for (var region : runtime.observedRegions()) {
            var field = region.accepted().field();
            if (region.retired || region.snapshotPauses != 0
                    || field == null || field.minimumQuality() || field.minimumSummary() == null) continue;
            if (minimumPending(region)) return;
            boolean touches = runtime.boundaryTouches(region, boundary);
            if (selected == null || touches && !local || touches == local
                    && region.solver.privateWorkspaceBytes() > selected.solver.privateWorkspaceBytes()) {
                selected = region; local = touches;
            }
        }
        if (selected != null) resourcePressure(selected);
    }
    /** Observe a returned slice; the work owner applies any requested private-model retry. */
    void observed(RegionWork item) {
        if (!ThermalWorkBudget.automatic() || item.account == null) return;
        var region = item.region;
        if (region.workPolicy == null) region.workPolicy = new RegionWorkPolicy();
        long cost = item.account.activeNanos();
        if (item instanceof RegionBuild build) {
            boolean capacity = build.failure instanceof ThermalMemoryBudget.AllocationRefused
                    || build.failure instanceof ContinuousAirSolver.PreparationLimitedException;
            if (region.solver.gridStep() == 1 && (capacity || region.workCoarseningUseful()
                    && cost > RegionalAirRuntime.buildWorkLimit(item.workAllowance))) {
                region.workPolicy.built(1, cost);
                minimumCandidate(region, capacity ? "resource_pressure" : "construction_work");
                if (capacity) FHMain.LOGGER.info("THERMAL_CANDIDATE_MEMORY,regionId={},cause={}", region.id, build.failure.toString());
            } else if (build.ready) region.workPolicy.built(region.solver.gridStep(), cost);
        } else if (item instanceof RegionJob job && !job.qualityOnly()
                && region.solver.gridStep() == 1 && region.workCoarseningUseful() && runtime.receivedThrough() - region.sampleTick() > 20
                && cost > 2 * item.workAllowance) minimum(region, "advance_work");
    }
    void minimumCandidate(ThermalRegion region, String reason) {
        if (region.qualityIntent == null || region.qualityIntent.target != Model.MINIMUM)
            FHMain.LOGGER.info("THERMAL_CANDIDATE_MINIMUM,receivedTick={},regionId={},reason={},cells={},allowanceNs={}",
                    runtime.receivedThrough(), region.id, reason, region.scope.cells.length, region.buildWorkAllowance);
        region.qualityIntent = new Intent(Model.MINIMUM, reason);
        if (region.workPolicy != null) region.workPolicy.coarse(runtime.receivedThrough(),
                region.parents.stream().anyMatch(parent -> parent.accepted().field() != null && parent.accepted().field().minimumQuality()));
    }
    void prepareCandidateModel(ThermalRegion region) {
        minimumCandidate(region, "resource_pressure");
        region.solver.gridStep(4);
    }
}
