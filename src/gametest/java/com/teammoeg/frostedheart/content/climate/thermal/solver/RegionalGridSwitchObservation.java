/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Requests real quality changes and samples the actual immutable states at installation. */
public final class RegionalGridSwitchObservation {
    private static BlockPos target, control;
    private static RegionalAirRuntime engine;
    private static int requested, completed, checkpoints;
    private static boolean requestSent;
    private static double maximumRecoveryJumpC, maximumCoarseningJumpC, maximumCheckpointErrorC;
    private static double maximumBackgroundRangeC;
    private static double maximumCoarseningEnergyErrorJ;
    private static int maximumCells;
    private static String failure;
    public static synchronized void arm(BlockPos changing, BlockPos unchanged) {
        target = changing.above().immutable(); control = unchanged.above().immutable(); engine = null;
        requested = completed = checkpoints = 0; requestSent = false; failure = null;
        maximumRecoveryJumpC = maximumCoarseningJumpC = maximumCheckpointErrorC = 0;
        maximumBackgroundRangeC = 0; maximumCells = 0;
        maximumCoarseningEnergyErrorJ = 0;
    }
    public static synchronized void request(int step) { requested = step; requestSent = false; }
    public static synchronized void engine(RegionalAirRuntime solver, ThermalSourceLedger sources, QueryPublication publication) {
        if (target == null || solver == null || failure != null || engine != null && engine != solver) return;
        if (requested != 0 && !requestSent) {
            requestSent = solver != null && solver.qualityControl().requestAt(
                    target.getX(), target.getY(), target.getZ(), requested == 1 ? RegionalQualityControl.Model.FINE : RegionalQualityControl.Model.MINIMUM);
            if (requestSent) engine = solver;
        }
    }
    static synchronized void installing(List<ThermalRegion> old, List<ThermalRegion> next) {
        if (!requestSent || target == null) return;
        var previous = at(old, target); var current = at(next, target);
        if (previous == null || current == null || previous.sampleTick() != current.sampleTick()) return;
        var before = previous.accepted().field(); var after = current.accepted().field();
        if (before == null || after == null) return;
        for (int dx = 0; dx < 3; dx++) {
            double x = target.getX() + dx + .5, y = target.getY() + .5, z = target.getZ() + .5;
            double a = before.airTemperatureC(x, y, z), b = after.airTemperatureC(x, y, z);
            if (Double.isFinite(a) && Double.isFinite(b)) {
                if (requested == 1) maximumRecoveryJumpC = Math.max(maximumRecoveryJumpC, Math.abs(a - b));
                else maximumCoarseningJumpC = Math.max(maximumCoarseningJumpC, Math.abs(a - b));
            }
        }
    }
    static synchronized void qualityPublished(ThermalRegion region, RegionalQuerySnapshot before, RegionalQuerySnapshot after) {
        if (!requestSent || target == null || region.solver.layout().componentAt(target.getX(), target.getY(), target.getZ()) == null) return;
        if (!before.minimumQuality() && after.minimumQuality()) {
            double energy = 0;
            double capacity = com.teammoeg.frostedheart.infrastructure.config.FHConfig.COMMON.THERMAL_RUNTIME.airHeatCapacityJPerBlockK.get();
            for (int index = 0; index < before.airLayout().componentCount(); index++) {
                var component = before.airLayout().component(index); long blocks = component.airBlocks;
                while (blocks != 0) {
                    int block = Long.numberOfTrailingZeros(blocks); blocks &= blocks - 1;
                    // A trilinear detailed field's cell-centre value is its exact volume mean.
                    energy += capacity * before.airTemperatureC(component.minX + (block & 3) + .5,
                            component.minY + (block >>> 4) + .5, component.minZ + (block >>> 2 & 3) + .5);
                }
            }
            for (int body = 0; body < before.materialCount(); body++) energy += before.materialEnthalpyJ(body);
            double difference = Math.abs(energy - after.minimumState().totalEnergyJ());
            maximumCoarseningEnergyErrorJ = Math.max(maximumCoarseningEnergyErrorJ, difference);
            if (!Double.isFinite(difference) || difference > Math.max(1e-6, Math.abs(energy) * 1e-10))
                failure = "Actual coarsening changed total represented energy by " + difference + " J";
        }
        for (int dx = 0; dx < 3; dx++) {
            double x = target.getX() + dx + .5, y = target.getY() + .5, z = target.getZ() + .5;
            double a = before.airTemperatureC(x, y, z), b = after.airTemperatureC(x, y, z);
            if (Double.isFinite(a) && Double.isFinite(b)) {
                if (after.minimumQuality()) maximumCoarseningJumpC = Math.max(maximumCoarseningJumpC, Math.abs(a - b));
                else maximumRecoveryJumpC = Math.max(maximumRecoveryJumpC, Math.abs(a - b));
            }
        }
    }
    public static synchronized void installed(Object owner) { observe((RegionalAirRuntime) owner); }
    static synchronized void observe(RegionalAirRuntime runtime) {
        if (target == null) return;
        int cells = 0;
        for (var region : runtime.observedRegions()) {
            cells += region.scope.cells.length;
            var frame = region.accepted().field();
            if (frame != null && frame.minimumQuality()) maximumBackgroundRangeC = Math.max(maximumBackgroundRangeC,
                    frame.minimumState().layout().background().naturalRangeC());
        }
        maximumCells = Math.max(maximumCells, cells);
        if (!requestSent || requested == 0) return;
        var region = at(runtime.observedRegions(), target);
        var other = at(runtime.observedRegions(), control);
        if (region == null || region.solver.gridStep() != requested) return;
        try {
            if (other == null || other == region || other.solver.gridStep() != 1) throw new AssertionError("Unrelated region lost fine service");
            var field = region.accepted().field();
            if (field == null) return;
            var component = field.airLayout().componentAt(target.getX(), target.getY(), target.getZ());
            int brick = (component.minX & 15) / 4 | ((component.minZ & 15) / 4) << 2 | ((component.minY & 15) / 4) << 4;
            var capture = new AirFieldCheckpoint.Capture(field.referenceTemperatureC()).atTick(field.sampleTick());
            if (field.minimumQuality()) capture.add(field.airLayout(), (x, y, z) -> field.componentTemperatureC(component.arenaSlot, x, y, z),
                    brick, component.arenaSlot, component.arenaGeneration);
            else capture.add(field.airLayout(), field.borrowedAirCoefficients(), field.referenceTemperatureC(), brick, component.arenaSlot, component.arenaGeneration);
            var saved = capture.finish();
            if (saved != null) {
                var restored = AirFieldCheckpoint.decode(saved.encode());
                double x = target.getX() + .5, y = target.getY() + .5, z = target.getZ() + .5;
                maximumCheckpointErrorC = Math.max(maximumCheckpointErrorC, Math.abs(restored.sampleC(x, y, z) - field.airTemperatureC(x, y, z))); checkpoints++;
            }
            completed++; requested = 0;
            FHMain.LOGGER.info("THERMAL_REGIONAL_GRID_STATE,{}", summary());
        } catch (RuntimeException | AssertionError problem) { failure = problem.toString(); }
    }
    private static ThermalRegion at(List<ThermalRegion> regions, BlockPos point) {
        for (var region : regions) if (region.solver.layout().componentAt(point.getX(), point.getY(), point.getZ()) != null) return region;
        return null;
    }
    public static synchronized int completed() { return completed; }
    public static synchronized String failure() { return failure; }
    public static synchronized int maximumCells() { return maximumCells; }
    public static synchronized double maximumBackgroundRangeC() { return maximumBackgroundRangeC; }
    public static synchronized String summary() { return "completed=" + completed + ",checkpoints=" + checkpoints
            + ",maxCoarseningJumpC=" + maximumCoarseningJumpC + ",maxRecoveryJumpC=" + maximumRecoveryJumpC
            + ",maxCheckpointErrorC=" + maximumCheckpointErrorC + ",maximumBackgroundRangeC=" + maximumBackgroundRangeC
            + ",maximumCoarseningEnergyErrorJ=" + maximumCoarseningEnergyErrorJ
            + ",maximumCells=" + maximumCells + ",failure=" + failure; }
    public static synchronized void end() { target = control = null; engine = null; }
}
