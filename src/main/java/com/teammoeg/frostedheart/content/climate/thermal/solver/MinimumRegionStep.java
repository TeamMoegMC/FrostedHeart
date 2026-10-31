/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import java.util.function.ToIntFunction;

/** Compacts one immutable input prefix before advancing the bounded network. */
final class MinimumRegionStep {
    private final RegionalQuerySnapshot before;
    private final RegionPending.Input input;
    private final MinimumThermalModel.InputPlan inputPlan;
    private final long topology;
    private final ToIntFunction<SpatialHeatInput.Target> materialSlot;
    private final int segments;
    private final double[] energyJ, naturalC;
    private MinimumThermalState.Step step;
    private RegionalQuerySnapshot result;
    private double unavailableJ;
    private boolean sealed;

    private static long backingBytes(int pairs) {
        return 2048L + AirBackingMeter.arrayPayloadBytes(MinimumThermalState.TIME_SEGMENTS * MinimumThermalState.MAXIMUM_NODES, 8)
                + 2 * AirBackingMeter.arrayPayloadBytes(pairs, 8)
                + 3 * AirBackingMeter.arrayPayloadBytes(MinimumThermalState.MAXIMUM_NODES, 8)
                + 4 * AirBackingMeter.arrayPayloadBytes(MinimumThermalState.PHASE_BODIES, 1);
    }

    /** Pair-factor arrays follow the actual immutable graph, not its worst case. */
    static long workspaceBytes(RegionalQuerySnapshot frame) {
        var minimum = frame.minimumQuality() ? frame.minimumState() : frame.minimumSummary();
        long bytes = backingBytes(minimum.layout().pairCount()) + 32768L;
        return (bytes + 32767L) & ~32767L;
    }

    MinimumRegionStep(RegionalQuerySnapshot before, RegionPending.Input input, long topology,
            double[] naturalC, ToIntFunction<SpatialHeatInput.Target> materialSlot, MinimumThermalModel.InputPlan inputPlan) {
        this.before = before; this.input = input; this.topology = topology;
        this.naturalC = naturalC; this.materialSlot = materialSlot; this.inputPlan = inputPlan;
        if (inputPlan != null) inputPlan.begin(before);
        segments = (int) Math.max(1, Math.min(MinimumThermalState.TIME_SEGMENTS, (input.toTick - input.fromTick + 19) / 20));
        energyJ = new double[segments * MinimumThermalState.MAXIMUM_NODES];
    }
    void sealInput() { sealed = true; }

    boolean run(long deadline) {
        if (result != null) return true;
        if (!sealed) return false;
        if (step == null) step = before.minimumState().new Step(input.fromTick, input.toTick, segments, energyJ, naturalC);
        if (!step.run(deadline)) return false;
        result = before.advanceMinimum(step.result(), input.toTick, topology);
        return true;
    }

    void accept(SpatialHeatInput.Target target, long fromTick, long toTick, double joules) {
        var mapping = inputPlan == null ? MinimumThermalModel.InputPlan.resolve(target, before, materialSlot)
                : inputPlan.get(target, before, materialSlot);
        // Waiting is dynamic material state, never part of the cached address.
        if (mapping.count == 0 || mapping.body >= 0 && before.materialWaiting(mapping.body)) {
            unavailableJ += joules; return;
        }
        double assigned = 0;
        for (int segment = 0; segment < segments; segment++) {
            long begin = input.fromTick + (input.toTick - input.fromTick) * segment / segments;
            long end = input.fromTick + (input.toTick - input.fromTick) * (segment + 1) / segments;
            double amount;
            if (fromTick == toTick) {
                long tick = fromTick;
                boolean contains = tick >= begin && (tick < end || segment == segments - 1 && tick == end);
                if (!contains) continue;
                amount = joules;
            } else {
                long from = Math.max(begin, fromTick), to = Math.min(end, toTick);
                if (from >= to) continue;
                amount = to == toTick ? joules - assigned : joules * ((double) (to - from) / (toTick - fromTick));
            }
            assigned += amount;
            mapping.add(amount, energyJ, segment * MinimumThermalState.MAXIMUM_NODES);
        }
    }

    RegionalQuerySnapshot result() { return result; }
    double deliveredEnergyJ() { return step == null ? 0 : step.deliveredEnergyJ(); }
    double unacceptedEnergyJ() { return unavailableJ + (step == null ? 0 : step.unacceptedEnergyJ()); }
}
