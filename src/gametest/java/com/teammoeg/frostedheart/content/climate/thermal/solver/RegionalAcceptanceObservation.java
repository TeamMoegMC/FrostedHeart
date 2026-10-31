/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.*;
import com.teammoeg.frostedheart.content.climate.thermal.query.*;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import net.minecraft.core.BlockPos;
import java.util.List;

/** Observe a real small-region commit while its larger neighbor still owns unfinished input. */
public final class RegionalAcceptanceObservation {
    private static BlockPos smallPoint, largePoint;
    private static ThermalCellArena arena;
    private static ThermalSourceLedger ledger;
    private static QueryPublication queries;
    private static boolean completed;
    private static String failure;
    private static int checked, largeNodes, materialBodies;
    private static long sequence, tick;
    private static double confirmedJ, pendingJ, imbalanceJ;
    public static synchronized void arm(BlockPos fire, BlockPos large) {
        smallPoint = fire.above().immutable(); largePoint = large.immutable();
        completed = false; failure = null; checked = largeNodes = materialBodies = 0;
    }
    public static synchronized void engine(RegionalAirRuntime solver, ThermalCellArena cells,
            ThermalSourceLedger sources, QueryPublication publication) {
        if (smallPoint == null || solver == null) return;
        arena = cells; ledger = sources; queries = publication;
    }
    public static synchronized void accepted(Object owner, Object candidate, List<?> regions, boolean accepted) {
        if (!accepted || completed || failure != null || smallPoint == null) return;
        var job = (RegionJob) candidate; var small = job.region;
        if (small.solver.layout().componentAt(smallPoint.getX(), smallPoint.getY(), smallPoint.getZ()) == null) return;
        checked++;
        ThermalRegion large = null;
        for (var item : regions) {
            var region = (ThermalRegion) item;
            if (region.solver.layout().componentAt(largePoint.getX(), largePoint.getY(), largePoint.getZ()) != null) large = region;
        }
        if (large == null || large == small || large.sampleTick() >= job.input.toTick || !large.pending.hasPending()) return;
        try {
            check(job.numericalReady && small.job == null && small.sampleTick() == job.input.toTick, "Small job did not commit independently");
            check(small.pending.frozen() != job.input && small.solver.deliveredEnergyJ() > 0, "Small input was not consumed once");
            var snapshot = small.accepted().field();
            check(snapshot == small.solver.acceptedSnapshot(), "Accepted material/Air versions disagree");
            var material = new MaterialSample();
            for (int index = 0; index < snapshot.materialCount(); index++) check(
                    snapshot.readMaterial(snapshot.materialSlot(index), arena.lifecycleGeneration(snapshot.materialSlot(index)), material)
                    && material.enthalpyJ() == snapshot.materialEnthalpyJ(index)
                    && material.branch() == snapshot.materialBranch(index), "Materials still wait for the large region");
            var sample = new QueryPublication.MutableSample();
            check(queries.tryReadAir(smallPoint.getX() + .5, smallPoint.getY() + .5, smallPoint.getZ() + .5, sample)
                    && sample.sampleTick() == job.input.toTick, "Query does not expose the small region's real time");
            var balance = ledger.energyBalance();
            pendingJ = ((RegionalAirRuntime) owner).pendingEnergyJ();
            imbalanceJ = balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ()
                    - balance.degradedLossJ() - balance.unacceptedJ() - balance.exportedJ() - pendingJ;
            check(pendingJ > 0 && Math.abs(imbalanceJ) < 1e-6, "Independent commit lost unfinished source input");
            sequence = job.input.sequence; tick = job.input.toTick; materialBodies = snapshot.materialCount();
            largeNodes = large.solver.layout().coefficientCount(); confirmedJ = small.solver.deliveredEnergyJ(); completed = true;
        } catch (RuntimeException | AssertionError problem) { failure = problem.toString(); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static synchronized boolean completed() { return completed; }
    public static synchronized String failure() { return failure; }
    public static synchronized String summary() {
        return "completed=" + completed + ",checkedCuts=" + checked + ",sequence=" + sequence + ",targetTick=" + tick
                + ",materialBodies=" + materialBodies + ",largeNodes=" + largeNodes + ",smallConfirmedJ=" + confirmedJ
                + ",unfinishedPendingJ=" + pendingJ + ",imbalanceJ=" + imbalanceJ + ",failure=" + failure;
    }
    public static synchronized void end() { smallPoint = largePoint = null; arena = null; ledger = null; queries = null; }
}
