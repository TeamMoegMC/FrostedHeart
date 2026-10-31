/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Work items own execution state; private construction does not use a live region's numerical job. */
final class RegionalWorkQueue {
    private final RegionalAirRuntime runtime;
    private final ArrayDeque<RegionWork> ready = new ArrayDeque<>();
    private final ArrayDeque<RegionJob> waitingMemory = new ArrayDeque<>();
    private final ConcurrentLinkedQueue<RegionWork> completed = new ConcurrentLinkedQueue<>();
    private final Set<RegionWork> active = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ArrayDeque<RegionJob> cleanup = new ArrayDeque<>();
    private final Object wakeLock = new Object();
    private Runnable wake;
    private int running;
    private boolean preferCleanup;
    private volatile boolean capacityAvailable;
    private final Runnable capacityWake = () -> { capacityAvailable = true; signal(); };

    RegionalWorkQueue(RegionalAirRuntime runtime) { this.runtime = runtime; }

    void replace(List<ThermalRegion> removed, List<ThermalRegion> added) {
        for (var old : removed) {
            old.retired = true;
            if (old.job != null) cancel(old.job);
        }
        for (var region : added) offer(region);
    }

    void offer(ThermalRegion region) {
        if (region.retired || region.snapshotPauses != 0
                || region.routingPending && !RegionalQualityControl.minimumPending(region)) return;
        if (!region.changedMaterials.isEmpty() || region.materialControls != null) return;
        var job = region.job;
        if (job == null) {
            if (RegionalQualityControl.minimumPending(region))
                region.job = job = RegionJob.minimumLayout(region, runtime.regionWorkAllowance(region));
            else {
                if (RegionalQualityControl.holdsNumericalWork(region)) return;
                var input = region.pending.freeze(runtime.executionLimit(region), region.executionSequenceLimit());
                if (input == null) return;
                region.job = job = new RegionJob(region, input, runtime.regionWorkAllowance(region));
            }
        }
        if (job.cancelled || job.state == RegionWork.State.RUNNING
                || job.state == RegionWork.State.WAITING && job.waiting == RegionWork.Wait.MEMORY) return;
        if (!job.qualityOnly() && job.input.aggregated && region.solver.gridStep() == 1
                && com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget.automatic()) {
            runtime.qualityControl().minimum(region, "aggregated_input"); return;
        }
        if (crossesBoundary(job)) { cancel(job); return; }
        enqueue(job);
    }

    void offerBuild(RegionBuild build) {
        if (build.ready || build.cancelled || build.state == RegionWork.State.RUNNING) return;
        active.add(build);
        if (build.region.solver.candidateProjectionHeld()) {
            build.state = RegionWork.State.WAITING; build.waiting = RegionWork.Wait.PROJECTION; return;
        }
        enqueue(build);
    }

    private void enqueue(RegionWork work) {
        if (work.cancelled || work.state == RegionWork.State.READY || work.state == RegionWork.State.FINISHED) return;
        active.add(work); work.state = RegionWork.State.READY; work.waiting = RegionWork.Wait.NONE;
        ready.addLast(work); signal();
    }

    void installedBuilds(List<RegionBuild> builds) {
        for (var build : builds) { build.release(); finish(build); }
    }

    boolean cancelBuilds(List<RegionBuild> builds) {
        boolean complete = true;
        for (var build : builds) {
            build.region.retired = true;
            if (!cancel(build)) { complete = false; continue; }
            build.region.solver.abortPreparedRebuild(); build.region.pending.retireRetention(); build.region.releaseMinimumWorkspace();
        }
        return complete;
    }

    boolean cancel(RegionWork work) {
        work.cancelled = true;
        if (work.task != null) { work.task.discardBeforeStart(); return false; }
        if (work.state == RegionWork.State.FINISHED) return true;
        if (work instanceof RegionJob job && job.region.job == job) {
            if (job.state != RegionWork.State.WAITING || job.waiting != RegionWork.Wait.CLEANUP) {
                job.state = RegionWork.State.WAITING; job.waiting = RegionWork.Wait.CLEANUP;
                cleanup.addLast(job); active.add(job); signal();
            }
            return false;
        }
        work.release(); finish(work); return true;
    }

    private void finish(RegionWork work) {
        work.state = RegionWork.State.FINISHED; work.waiting = RegionWork.Wait.NONE; active.remove(work);
    }

    private void cleanupOne(long deadline) {
        var job = cleanup.pollFirst();
        if (job == null) return;
        if (!runtime.discardJobSlice(job, deadline)) cleanup.addLast(job);
        else { finish(job); offer(job.region); }
    }

    void runRound(long deadline) {
        if (capacityAvailable) { capacityAvailable = false; retryMemory(); }
        collect(8, deadline);
        if (System.nanoTime() >= deadline) return;
        if (!cleanup.isEmpty() && (preferCleanup || ready.isEmpty())) {
            preferCleanup = false; cleanupOne(deadline); return;
        }
        preferCleanup = true;
        int available = Math.max(1, ThermalWorkerPool.shared().parallelism() - running);
        RegionWork inline = null;
        while (available-- > 0 && !ready.isEmpty() && System.nanoTime() < deadline) {
            var work = ready.removeFirst();
            if (work.state != RegionWork.State.READY) continue;
            work.state = RegionWork.State.NEW;
            if (work.cancelled || work.region.retired) { cancel(work); continue; }
            if (work instanceof RegionJob job) {
                if (job.region.job != job) { cancel(job); continue; }
                if (job.region.routingPending && !job.qualityOnly()) {
                    job.state = RegionWork.State.WAITING; job.waiting = RegionWork.Wait.INPUT; continue;
                }
                if (crossesBoundary(job)) { cancel(job); continue; }
                if (job.numericalReady) {
                    long revision = runtime.capacityRevision();
                    boolean accepted = runtime.acceptJob(job);
                    if (job.cancelled) continue;
                    if (!accepted) waitForMemory(job, revision);
                    else { finish(job); retryMemory(); offer(job.region); }
                    continue;
                }
                if (!job.prepared()) {
                    long revision = runtime.capacityRevision();
                    if (!runtime.prepareJob(job, deadline)) {
                        if (job.waiting == RegionWork.Wait.MEMORY) waitForMemory(job, revision); else enqueue(job);
                        continue;
                    }
                }
            } else if (work.region.solver.candidateProjectionHeld()) {
                work.state = RegionWork.State.WAITING; work.waiting = RegionWork.Wait.PROJECTION; continue;
            }
            work.task = new ThermalAssemblyTask(work::runSlice);
            work.state = RegionWork.State.RUNNING; running++;
            work.task.finished.whenComplete((ignored, failure) -> { completed.add(work); signal(); });
            if (inline == null) inline = work;
            else if (!ThermalWorkerPool.shared().tryExecute(work.task)) work.task.discardBeforeStart();
        }
        if (inline != null) inline.task.run(deadline);
        collect(8, deadline);
    }

    private void collect(int maximum, long deadline) {
        RegionWork work;
        while (maximum-- > 0 && System.nanoTime() < deadline && (work = completed.poll()) != null) {
            running--;
            try { work.task.finished.join(); }
            catch (CompletionException failure) {
                if (work instanceof RegionBuild build) { build.failure = failure.getCause(); build.ready = true; }
                else if (!work.cancelled && !work.region.retired) throw failure;
            }
            work.task = null; work.state = RegionWork.State.NEW;
            if (work.cancelled || work.region.retired || work instanceof RegionJob job && job.region.job != job) {
                cancel(work); continue;
            }
            runtime.qualityControl().observed(work);
            if (work instanceof RegionBuild build) {
                build.applyQualityIntent();
                if (build.ready) { build.state = RegionWork.State.WAITING; build.waiting = RegionWork.Wait.INPUT; }
                else offerBuild(build);
            } else offer(work.region);
        }
    }

    private void waitForMemory(RegionJob work, long observed) {
        work.state = RegionWork.State.WAITING; work.waiting = RegionWork.Wait.MEMORY; waitingMemory.addLast(work);
        work.capacityObserved = observed;
    }

    private boolean crossesBoundary(RegionJob job) {
        if (job.qualityOnly()) return false; // Changes no physical time and consumes no input prefix.
        var input = job.input; var region = job.region;
        long limit = runtime.executionLimit(region);
        return input.toTick > limit || input.fromTick == input.toTick
                && input.toTick == limit && input.sequence > region.executionSequenceLimit();
    }

    void boundaryChanged(ThermalRegion region) {
        if (region.job != null && crossesBoundary(region.job)) cancel(region.job);
        offer(region);
    }

    void retryMemory() {
        int remaining = waitingMemory.size();
        while (remaining-- > 0) {
            var work = waitingMemory.removeFirst();
            if (work.cancelled || work.state == RegionWork.State.FINISHED) continue;
            work.state = RegionWork.State.NEW;
            offer(work.region);
        }
    }
    void resourcesReturned(ThermalRegion region) {
        int remaining = waitingMemory.size();
        while (remaining-- > 0) {
            var item = waitingMemory.removeFirst();
            if (item.cancelled || item.state == RegionWork.State.FINISHED) continue;
            if (item.region != region) { waitingMemory.addLast(item); continue; }
            item.state = RegionWork.State.NEW;
            offer(region);
        }
    }

    boolean hasReady() { return capacityAvailable || !ready.isEmpty() || !completed.isEmpty() || !cleanup.isEmpty(); }
    boolean hasRunning() { return running != 0; }
    void whenReady(Runnable continuation) {
        synchronized (wakeLock) { if (hasReady()) { continuation.run(); return; } wake = continuation; }
        long observed = Long.MAX_VALUE;
        for (var item : waitingMemory) if (!item.cancelled && item.state == RegionWork.State.WAITING)
            observed = Math.min(observed, item.capacityObserved);
        if (observed != Long.MAX_VALUE) runtime.whenCapacityChanges(observed, capacityWake);
    }
    private void signal() {
        Runnable continuation;
        synchronized (wakeLock) { continuation = wake; wake = null; }
        if (continuation != null) continuation.run();
    }

    void cancelAll() { for (var work : List.copyOf(active)) cancel(work); }

    boolean settle() {
        long deadline = System.nanoTime() + 2_000_000L;
        for (var region : runtime.observedRegions()) if (region.job != null) { region.retired = true; cancel(region.job); }
        for (var work : List.copyOf(active)) { work.region.retired = true; cancel(work); }
        collect(8, deadline);
        if (System.nanoTime() < deadline) cleanupOne(deadline);
        if (running != 0 || !active.isEmpty()) return false;
        ready.clear(); waitingMemory.clear(); completed.clear();
        runtime.cancelCapacityWait(capacityWake);
        synchronized (wakeLock) { wake = null; }
        return true;
    }
}
