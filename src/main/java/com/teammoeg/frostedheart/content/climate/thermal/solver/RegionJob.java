/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.source.CutLoadBuffer;

/** One installed-region task: a frozen numerical prefix or a same-time minimum layout. */
final class RegionJob extends RegionWork {
    enum Kind { ADVANCE, MINIMUM_LAYOUT }
    final Kind kind;
    final RegionPending.Input input;
    com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore.Claim heatClaim;
    com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput heat;
    CutLoadBuffer loads;
    ContinuousAirSolver.CutCursor cursor;
    MinimumRegionStep minimum;
    MinimumThermalModel.Preparation minimumLayout;
    RegionalQuerySnapshot.Restoration restoration;
    boolean numericalReady, solved;
    boolean inputsReady, prepaidWorkspace;
    long capacityObserved = -1;
    long workspaceFloor;
    long transportBytes;
    boolean transportAdmitted;
    long topologyGeneration;
    final java.util.ArrayList<com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target>
            blockedInputs = new java.util.ArrayList<>();

    RegionJob(ThermalRegion region, RegionPending.Input input, long workAllowance) {
        this(region, input, workAllowance, Kind.ADVANCE);
    }
    private RegionJob(ThermalRegion region, RegionPending.Input input, long workAllowance, Kind kind) {
        super(region, workAllowance); this.input = input; this.kind = kind;
    }
    static RegionJob minimumLayout(ThermalRegion region, long workAllowance) {
        return new RegionJob(region, new RegionPending.Input(region.consumedSequence(), region.sampleTick(),
                region.sampleTick(), RegionPending.Environment.UNCHANGED, false), workAllowance, Kind.MINIMUM_LAYOUT);
    }
    boolean qualityOnly() { return kind == Kind.MINIMUM_LAYOUT; }
    @Override boolean construction() { return false; }
    @Override boolean resultReady() { return numericalReady; }
    @Override long fromTick() { return input.fromTick; }
    @Override long targetTick() { return input.toTick; }

    @Override void advance(long deadline) {
        if (qualityOnly()) {
            if (minimumLayout == null) minimumLayout = region.solver.prepareMinimumTransport();
            if (minimumLayout.run(deadline)) {
                publication = region.accepted().field().activateMinimum(minimumLayout.result().layout());
                solved = numericalReady = true;
            }
            return;
        }
        if (minimum != null) {
            if (minimum.run(deadline)) { publication = minimum.result(); solved = numericalReady = true; }
            return;
        }
        if (!solved) solved = cursor.run(deadline);
        if (solved && System.nanoTime() < deadline) numericalReady = prepareFrame(input.toTick, topologyGeneration, deadline);
    }

    boolean prepared() { return inputsReady; }
    double deliveredEnergyJ() { return minimum == null ? region.solver.deliveredEnergyJ() : minimum.deliveredEnergyJ(); }
    double unacceptedEnergyJ() { return minimum == null ? region.solver.unacceptedEnergyJ() : minimum.unacceptedEnergyJ(); }

    @Override void release() {
        if (region.job == this && minimum == null && !qualityOnly()) region.solver.releaseStepInputs();
        loads = null; cursor = null; minimum = null; minimumLayout = null; restoration = null;
        if (heatClaim != null) { heatClaim.close(); heatClaim.retainCleanup(reservation, prepaidWorkspace); heatClaim = null; }
        heat = null;
        if (prepaidWorkspace) {
            if (qualityOnly() && reservation != null) {
                if (region.accepted().field() != null && !region.accepted().field().minimumQuality()) region.releaseMinimumWorkspace();
                else reservation.completeResize(MinimumRegionStep.workspaceBytes(region.accepted().field()));
            }
            reservation = null;
        }
        super.release();
    }
}
