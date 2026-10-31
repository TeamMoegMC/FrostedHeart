/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import java.util.Arrays;
import net.minecraft.core.BlockPos;

/** Cancels completed, unpublished production work twice through ordinary scheduler cleanup. */
public final class SourceStepProductionObservation {
    private static BlockPos fire;
    private static ThermalCellArena arena;
    private static RegionPending.Input input;
    private static RegionalQuerySnapshot.Accepted before;
    private static double[] resultC, resultH, arenaBefore;
    private static double[] resultMinimumJ;
    private static byte[] resultBranches;
    private static int cancellations, publications;
    private static double energyJ, deliveredJ, unacceptedJ;
    private static double maximumReplayEnergyDifferenceJ, maximumReplayTemperatureDifferenceC;
    private static boolean completed;
    private static String failure;
    public static synchronized void arm(BlockPos source) {
        fire = source.immutable(); input = null; before = null; cancellations = publications = 0;
        resultC = resultH = resultMinimumJ = arenaBefore = null; resultBranches = null;
        maximumReplayEnergyDifferenceJ = maximumReplayTemperatureDifferenceC = 0;
        completed = false; failure = null;
    }
    public static synchronized void engine(ThermalCellArena cells, ThermalMemoryBudget.Reservation backing) {
        if (fire != null && !completed) arena = cells;
    }
    public static synchronized boolean beforeAccept(RegionalAirRuntime runtime, RegionJob job) {
        if (fire == null || arena == null || completed || failure != null
                || job.region.solver.layout().componentAt(fire.getX(), fire.getY() + 1, fire.getZ()) == null
                || input == null && job.deliveredEnergyJ() + job.unacceptedEnergyJ() <= 0) return true;
        try {
            var frame = job.publication;
            check(job.task == null && job.numericalReady && frame != null, "Observe only completed real work");
            if (input == null) {
                input = job.input; before = job.region.accepted();
                resultC = frame.minimumQuality() ? null : frame.borrowedAirCoefficients().clone();
                resultMinimumJ = frame.minimumQuality()
                        ? new double[com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState.MAXIMUM_NODES] : null;
                if (resultMinimumJ != null) for (int i = 0; i < resultMinimumJ.length; i++)
                    resultMinimumJ[i] = frame.minimumState().energyJ(i);
                resultH = new double[frame.materialCount()]; resultBranches = new byte[resultH.length];
                for (int i = 0; i < resultH.length; i++) { resultH[i] = frame.materialEnthalpyJ(i); resultBranches[i] = frame.materialBranch(i); }
                arenaBefore = new double[job.region.scope.cells.length];
                for (int i = 0; i < arenaBefore.length; i++) arenaBefore[i] = arena.enthalpyJ(job.region.scope.cells[i]);
                energyJ = job.loads == null ? job.deliveredEnergyJ() + job.unacceptedEnergyJ() : job.loads.pendingEnergyJ();
                deliveredJ = job.deliveredEnergyJ(); unacceptedJ = job.unacceptedEnergyJ();
                check(deliveredJ > 0 && resultH.length > 0, "Real source interval must exchange with materials");
            } else {
                check(job.input == input && job.region.accepted() == before, "Cancellation changed accepted state or frozen input");
                check(frame.minimumQuality() == (resultMinimumJ != null), "Cancellation changed field quality");
                if (resultMinimumJ == null) check(Arrays.equals(resultC, frame.borrowedAirCoefficients()), "Reclaimed input changed Air results");
                else for (int i = 0; i < resultMinimumJ.length; i++) {
                    double next = frame.minimumState().energyJ(i), difference = Math.abs(resultMinimumJ[i] - next);
                    maximumReplayEnergyDifferenceJ = Math.max(maximumReplayEnergyDifferenceJ, difference);
                    double capacity = frame.minimumState().layout().capacity(i);
                    if (capacity > 0) maximumReplayTemperatureDifferenceC = Math.max(maximumReplayTemperatureDifferenceC, difference / capacity);
                    check(sameEnergy(resultMinimumJ[i], next), "Reclaimed minimum input changed node=" + i
                            + ",beforeJ=" + resultMinimumJ[i] + ",afterJ=" + next + ",deltaJ=" + difference);
                }
                for (int i = 0; i < resultH.length; i++) check(sameEnergy(resultH[i], frame.materialEnthalpyJ(i))
                        && resultBranches[i] == frame.materialBranch(i), "Reclaimed input changed material state");
                check(deliveredJ == job.deliveredEnergyJ() && unacceptedJ == job.unacceptedEnergyJ(), "Replay changed source accounting");
                for (int i = 0; i < arenaBefore.length; i++) check(arenaBefore[i] == arena.enthalpyJ(job.region.scope.cells[i]),
                        "Unpublished work changed the arena");
            }
            if (cancellations < 2) { cancellations++; return false; }
        } catch (RuntimeException | AssertionError problem) {
            failure = problem.toString();
            com.teammoeg.frostedheart.FHMain.LOGGER.error("THERMAL_SOURCE_STEP_OBSERVATION_FAILURE,{}", summary());
        }
        return true;
    }
    public static synchronized void afterAccept(Object candidate, boolean accepted) {
        var job = (RegionJob) candidate;
        if (!accepted || job.input != input || completed || failure != null) return;
        try {
            publications++;
            check(cancellations == 2 && publications == 1 && job.region.sampleTick() == input.toTick
                    && job.region.consumedSequence() == input.sequence, "Input was not consumed exactly once");
            check(job.region.pending.frozen() != input && job.reservation == null, "Commit retained input or job scratch");
            check(job.region.accepted().field() == job.region.solver.acceptedSnapshot(), "Air/material publications disagree");
            completed = true;
        } catch (RuntimeException | AssertionError problem) { failure = problem.toString(); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    /** Streaming replay may sum identical world records in a different floating-point order. */
    private static boolean sameEnergy(double first, double second) {
        double scale = Math.max(Math.abs(energyJ), Math.max(Math.abs(first), Math.abs(second)));
        return first == second || Double.isFinite(first) && Double.isFinite(second)
                && Math.abs(first - second) <= 32 * Math.ulp(scale);
    }
    public static synchronized boolean completed() { return completed; }
    public static synchronized String failure() { return failure; }
    public static synchronized String summary() {
        return "sequence=" + (input == null ? -1 : input.sequence) + ",sourceJ=" + energyJ + ",deliveredJ=" + deliveredJ
                + ",unacceptedJ=" + unacceptedJ + ",workspaceCancellations=" + cancellations + ",publications=" + publications
                + ",minimum=" + (resultMinimumJ != null) + ",maxReplayEnergyDeltaJ=" + maximumReplayEnergyDifferenceJ
                + ",maxReplayTemperatureDeltaC=" + maximumReplayTemperatureDifferenceC + ",completed=" + completed + ",failure=" + failure;
    }
    public static synchronized void closed(ThermalCellArena cells) { if (arena == cells) end(); }
    public static synchronized void end() { fire = null; arena = null; input = null; before = null; resultC = resultH = resultMinimumJ = arenaBefore = null; resultBranches = null; }
}
