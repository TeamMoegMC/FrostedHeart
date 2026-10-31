/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/** Coordinator-owned region. A publication, its physical time and receipt move together. */
final class ThermalRegion {
    final long id, epoch;
    final RegionalAirRuntime.Scope scope;
    final ContinuousAirSolver solver;
    final RegionalQuerySnapshot.Handle publication;
    RegionalQuerySnapshot.Accepted accepted() { return publication.accepted(); }
    LongOpenHashSet dependencySections;
    final Int2DoubleOpenHashMap environmentNaturalC = new Int2DoubleOpenHashMap();
    double environmentWindScale;
    long environmentTick = -1;
    long blockedAfterTick = Long.MAX_VALUE;
    long blockedAfterSequence = Long.MAX_VALUE;
    RegionalQuerySnapshot materialControls;
    RegionalQuerySnapshot.Restoration materialRestoration;
    final it.unimi.dsi.fastutil.ints.IntOpenHashSet changedMaterials = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
    RegionalQuerySnapshot.Template template;
    final RegionPending pending = new RegionPending();
    final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap inputSources = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    java.util.List<ThermalRegion> parents = java.util.List.of();
    RegionJob job;
    MinimumThermalModel.InputPlan minimumInputs;
    boolean retired;
    boolean routingPending;
    final java.util.LinkedHashSet<com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target> routingTargets = new java.util.LinkedHashSet<>();
    RegionalQualityControl.Intent qualityIntent;
    RegionalQualityControl.Change qualityCandidate;
    boolean installed;
    long indexedTick;
    boolean countedPending;
    RegionWorkPolicy workPolicy;
    long buildWorkAllowance;
    int snapshotPauses, snapshotSeals;
    com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation minimumWorkspace;

    void releaseMinimumWorkspace() {
        if (minimumWorkspace != null) { minimumWorkspace.close(); minimumWorkspace = null; }
    }

    ThermalRegion(long id, long epoch, RegionalAirRuntime.Scope scope, ContinuousAirSolver solver) {
        this.id = id; this.epoch = epoch; this.scope = scope; this.solver = solver;
        publication = new RegionalQuerySnapshot.Handle(id);
    }

    long sampleTick() { return accepted().sampleTick(); }
    long consumedSequence() { return accepted().consumedSequence(); }
    long executionLimit() { return blockedAfterTick; }
    long executionSequenceLimit() { return blockedAfterSequence; }
    long workWeight() { return Math.max(1, scope.fineVolumes + scope.cells.length - scope.airComponents); }
    boolean workCoarseningUseful() {
        return workWeight() > com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState.MAXIMUM_NODES;
    }

    void accept(RegionalQuerySnapshot field, long tick, long sequence) {
        solver.acceptPublishedState(field);
        publication.publish(field, tick, sequence);
    }
}
