/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Lightweight reads of accepted real state. No field copies, graph scans or timing injection. */
public final class WorkPolicyObservation {
    private static BlockPos control;
    private static BlockPos[] targets;
    private static RegionalAirRuntime engine;
    private static String phase = "unarmed";
    private static int requested;
    private static double maximumImbalanceJ;
    private static int coarseCuts, recoveredCuts;
    private static boolean sawCoarse;
    private static double maximumRecoveryJumpC;
    private static int recoveryProjectionSamples;
    private static long[] lastTargetAdvancedTicks;
    private static int timeRegressions;

    public static synchronized void arm(BlockPos small, BlockPos... large) {
        control = small.immutable(); targets = large.clone(); engine = null;
        for (int index = 0; index < targets.length; index++) targets[index] = targets[index].immutable();
        maximumImbalanceJ = 0; requested = coarseCuts = recoveredCuts = 0; sawCoarse = false;
        maximumRecoveryJumpC = 0; recoveryProjectionSamples = 0;
        lastTargetAdvancedTicks = new long[targets.length];
        java.util.Arrays.fill(lastTargetAdvancedTicks, -1); timeRegressions = 0;
        phase = "warmup";
    }
    public static synchronized void mark(String label, long tick) {
        phase = label;
        FHMain.LOGGER.info("THERMAL_WORK_POLICY_WINDOW,phase={},gameTick={}", phase, tick);
    }
    public static synchronized void request(int step) {
        if (ThermalWorkBudget.automatic()) throw new IllegalStateException("Automatic validation cannot select grid quality");
        requested = step;
    }
    public static synchronized void begin(RegionalAirRuntime solver) {
        if (targets == null || solver == null || engine != null && engine != solver) return;
        BlockPos target = targets[0];
        if (solver == null || at(solver.observedRegions(), target) == null) return;
        engine = solver;
        if (requested != 0 && solver.qualityControl().requestAt(target.getX(), target.getY(), target.getZ(),
                requested == 1 ? RegionalQualityControl.Model.FINE : RegionalQualityControl.Model.MINIMUM)) requested = 0;
    }
    public static synchronized void end(RegionalAirRuntime solver, ThermalSourceLedger sources, long tick) {
        if (engine != solver || targets == null) return;
        {
            var runtime = solver;
            var regions = runtime.observedRegions();
            ThermalRegion small = at(regions, control);
            if (small == null) return;
            var balance = sources.energyBalance();
            double pending = runtime.pendingEnergyJ();
            double imbalance = balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ()
                    - balance.degradedLossJ() - balance.unacceptedJ() - balance.exportedJ() - pending;
            maximumImbalanceJ = Math.max(maximumImbalanceJ, Math.abs(imbalance));
            for (int index = 0; index < targets.length; index++) {
                ThermalRegion large = at(regions, targets[index]);
                if (large == null) continue;
                int step = large.solver.gridStep();
                if (large.sampleTick() < lastTargetAdvancedTicks[index]) timeRegressions++;
                lastTargetAdvancedTicks[index] = Math.max(lastTargetAdvancedTicks[index], large.sampleTick());
                if (index == 0) {
                    if (step == 4) { sawCoarse = true; coarseCuts++; }
                    else if (sawCoarse) recoveredCuts++;
                }
                FHMain.LOGGER.info("THERMAL_WORK_POLICY_STATE,phase={},targetTick={},controlId={},controlStep={},controlTick={},targetId={},targetStep={},targetAdvancedTick={},deliveredJ={},pendingJ={},imbalanceJ={},baseIndex={},{}",
                        phase, tick, small.id, small.solver.gridStep(), small.sampleTick(),
                        large.id, step, large.sampleTick(), balance.deliveredJ(), pending, imbalance, index,
                        large.workPolicy == null ? "policy=unavailable" : large.workPolicy.summary());
            }
        }
    }
    private static ThermalRegion at(List<ThermalRegion> regions, BlockPos point) {
        for (var region : regions) if (region.solver.layout().componentAt(point.getX(), point.getY(), point.getZ()) != null) return region;
        return null;
    }
    /** Compare accepted fields at their real physical time, including bounded minimum fields. */
    public static synchronized void installing(List<ThermalRegion> previous, List<ThermalRegion> candidates) {
        if (targets == null) return;
        for (var target : targets) {
            var old = at(previous, target); var next = at(candidates, target);
            if (old == null || next == null || old.solver.gridStep() != 4 || next.solver.gridStep() != 1
                    || old.sampleTick() != next.sampleTick()) continue;
            var before = old.accepted().field(); var after = next.accepted().field();
            if (before == null || after == null) continue;
            for (int dx = 0; dx < 3; dx++) {
                double x = target.getX() + dx + .5, y = target.getY() + .5, z = target.getZ() + .5;
                double a = before.airTemperatureC(x, y, z), b = after.airTemperatureC(x, y, z);
                if (Double.isFinite(a) && Double.isFinite(b)) { maximumRecoveryJumpC = Math.max(maximumRecoveryJumpC, Math.abs(a - b)); recoveryProjectionSamples++; }
            }
        }
    }
    public static synchronized double maximumImbalanceJ() { return maximumImbalanceJ; }
    public static synchronized int coarseCuts() { return coarseCuts; }
    public static synchronized int recoveredCuts() { return recoveredCuts; }
    public static synchronized double maximumRecoveryJumpC() { return maximumRecoveryJumpC; }
    public static synchronized int recoveryProjectionSamples() { return recoveryProjectionSamples; }
    public static synchronized int timeRegressions() { return timeRegressions; }
    public static synchronized void stop() { engine = null; targets = null; control = null; }
}
