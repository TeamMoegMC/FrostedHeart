/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;

/** Scheduler-owned execution state shared by numerical and structural work. */
abstract class RegionWork {
    enum State { NEW, READY, RUNNING, WAITING, FINISHED }
    enum Wait { NONE, INPUT, MEMORY, PROJECTION, CLEANUP }

    @jdk.jfr.Name("frostedheart.ThermalRegionSlice")
    @jdk.jfr.StackTrace(false)
    private static final class Observation extends jdk.jfr.Event {
        public long regionId, epoch, fromTick, targetTick, activeNanos;
        public int buildPhase, cells;
        public boolean construction, completed;
    }

    final ThermalRegion region;
    final long workAllowance;
    State state = State.NEW;
    Wait waiting = Wait.NONE;
    ThermalWorkBudget.Account account;
    ThermalAssemblyTask task;
    ThermalMemoryBudget.Reservation reservation;
    volatile boolean cancelled;
    long durationNanos;
    RegionalQuerySnapshot publication;
    private RegionalQuerySnapshot.Preparation snapshot;
    private RegionalQuerySnapshot.TemplatePreparation template;
    private MinimumThermalModel.Projection minimumProjection;

    RegionWork(ThermalRegion region, long workAllowance) { this.region = region; this.workAllowance = workAllowance; }
    abstract boolean construction();
    abstract boolean resultReady();
    abstract long fromTick();
    abstract long targetTick();
    abstract void advance(long deadline);

    final void runSlice(long deadline) {
        if (cancelled) return;
        var diagnostics = ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.sliceStarted(this);
        long start = System.nanoTime();
        var observation = jdk.jfr.FlightRecorder.isInitialized() ? new Observation() : null;
        if (observation != null) {
            observation.regionId = region.id; observation.epoch = region.epoch;
            observation.construction = construction();
            observation.buildPhase = construction() ? region.solver.preparationPhase() : -1;
            observation.fromTick = fromTick(); observation.targetTick = targetTick();
            observation.cells = region.scope.cells.length; observation.begin();
        }
        try (var work = ThermalWorkBudget.enter(account, ThermalWorkBudget.Phase.TRANSPORT)) {
            advance(deadline);
        } finally {
            long elapsed = System.nanoTime() - start; durationNanos += elapsed;
            if (diagnostics != null) diagnostics.sliceFinished(this, elapsed);
            if (observation != null) {
                observation.activeNanos = elapsed; observation.completed = resultReady();
                observation.end(); observation.commit();
            }
        }
    }

    final boolean prepareFrame(long tick, long topology, long deadline) {
        if (region.template == null) {
            if (template == null) template = region.solver.prepareQueryTemplate(region.publication);
            if (!template.run(deadline)) return false;
            region.template = template.result(); template = null;
        }
        if (snapshot == null) snapshot = region.solver.prepareSnapshot(region.template, tick, topology);
        if (!snapshot.run(deadline)) return false;
        var field = snapshot.result();
        if (minimumProjection == null) minimumProjection = region.solver.prepareMinimumProjection(field);
        if (!minimumProjection.run(deadline)) return false;
        publication = field.withMinimumSummary(minimumProjection.result(), minimumProjection.anchor());
        if (region.solver.gridStep() == 4) publication = publication.activatePreparedMinimum();
        return true;
    }

    final void clearFrame() { publication = null; snapshot = null; template = null; minimumProjection = null; }

    void release() {
        if (reservation != null) { reservation.close(); reservation = null; }
        clearFrame();
    }
}
