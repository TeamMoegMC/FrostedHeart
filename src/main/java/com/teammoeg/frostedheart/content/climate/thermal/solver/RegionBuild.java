/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources;
import com.teammoeg.frostedheart.content.climate.thermal.topology.PreparedTopologyChange;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;

/** A structural owner's private child. It never consumes ordinary regional input. */
final class RegionBuild extends RegionWork {
    private final FrozenSources sources;
    private final PreparedTopologyChange geometry;
    private final com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation graphCredit;
    private RegionalQuerySnapshot[] parents;
    boolean ready;
    private boolean built;
    Throwable failure;

    RegionBuild(ThermalRegion region, FrozenSources sources, PreparedTopologyChange geometry,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation graphCredit) {
        super(region, region.buildWorkAllowance); this.sources = sources; this.geometry = geometry;
        this.graphCredit = graphCredit;
        parents = region.parents.stream().map(parent -> parent.accepted().field()).toArray(RegionalQuerySnapshot[]::new);
        region.solver.preparationMaterials(parents);
        account = ThermalWorkBudget.newAccount();
    }

    @Override boolean construction() { return true; }
    @Override boolean resultReady() { return ready; }
    @Override long fromTick() { return region.sampleTick(); }
    @Override long targetTick() { return region.sampleTick(); }

    @Override void advance(long deadline) {
        if (!built) built = region.solver.prepareCandidateSlice(sources, geometry, graphCredit, deadline);
        if (built && System.nanoTime() < deadline) ready = prepareFrame(region.sampleTick(),
                geometry.installedVersion(region.solver.geometryVersion()), deadline);
    }

    void applyQualityIntent() {
        if (region.qualityIntent == null || region.qualityIntent.target() != RegionalQualityControl.Model.MINIMUM
                || region.solver.gridStep() == 4) return;
        region.solver.abortPreparedRebuild(); region.solver.gridStep(4);
        region.solver.releaseDetailedWorkspaces();
        region.solver.seed(null, new double[0], region.solver.shapeStore());
        ready = built = false; failure = null;
        clearFrame(); region.template = null;
        // Aborting private solver preparation releases its borrowed parent pointer.
        // The build owns that input until acceptance/cancellation, including a coarse retry.
        region.solver.preparationMaterials(parents);
    }

    void resumeProjection(AirFieldLayout previous, double[] values, RegionalQuerySnapshot[] parents) {
        this.parents = parents;
        region.solver.resumeCandidateProjection(previous, values, parents);
    }

    @Override void release() { parents = null; super.release(); }
}
