/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;

import java.util.Objects;

/** Executes one concrete source-and-air-sweep path over a bounded time plan. */
public final class ThermalStepExecutor {
    private ThermalStepExecutor() {
    }

    public enum Status {
        COMPLETED,
        INPUTS_PENDING,
        NUMERIC_DEGRADED
    }

    public record Report(
            Status status,
            ThermalStepPlan.Status timeStatus,
            int executedTransportSubsteps,
            long skippedTransportTicks,
            long skippedPhaseTicks,
            double sourceAppliedJ,
            InputWatermarks sealedWatermarks,
            InputWatermarks appliedWatermarks
    ) {
    }

    public static Report execute(
            ThermalStepPlan plan,
            long appliedDimensionGeneration,
            InputWatermarks appliedWatermarks,
            ThermalCellArena arena,
            ThermalSourceTimeline sources,
            ThermalSweep airSweep,
            double referenceTemperatureC
    ) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(airSweep, "airSweep");
        if (!sources.targets(arena) || !airSweep.targets(arena)) {
            throw new IllegalArgumentException(
                    "source timeline, sweep, and executor must share one arena");
        }
        if (!Double.isFinite(referenceTemperatureC)) {
            throw new IllegalArgumentException("referenceTemperatureC must be finite");
        }
        SolveEpoch epoch = plan.epoch();
        InputWatermarks actualApplied = actualAppliedWatermarks(
                appliedWatermarks, sources);
        if (!epoch.nonSourceInputsSatisfiedBy(
                appliedDimensionGeneration, actualApplied)) {
            return new Report(
                    Status.INPUTS_PENDING,
                    plan.status(),
                    0,
                    plan.skippedTransportTicks(),
                    plan.skippedPhaseTicks(),
                    0.0D,
                    epoch.sealedWatermarks(),
                    actualApplied
            );
        }
        if (!sources.isReady(epoch)) {
            return new Report(
                    Status.INPUTS_PENDING,
                    plan.status(),
                    0,
                    plan.skippedTransportTicks(),
                    plan.skippedPhaseTicks(),
                    0.0D,
                    epoch.sealedWatermarks(),
                    actualApplied
            );
        }

        ThermalSweep.Direction direction = ThermalSweep.Direction.forEpoch(epoch);
        try {
            airSweep.preflight(referenceTemperatureC, epoch, direction);
        } catch (RuntimeException failure) {
            throw ExecutionFailure.preflight(epoch.previousTick(), failure);
        }

        boolean sourcePreApplied = sources.isPreApplied(epoch);
        double sourceApplied = sourcePreApplied
                ? sources.preAppliedEnergyJ(epoch)
                : 0.0D;
        double compensation = 0.0D;
        int executedTransport = 0;
        boolean numericDegraded = false;
        long sourceCursor = sources.cursorTick();
        double cutEnergy = sourcePreApplied
                || sourceCursor != epoch.previousTick()
                ? 0.0D
                : sources.apply(epoch, sourceCursor, sourceCursor);
        if (!Double.isFinite(cutEnergy)) {
            return numericDegraded(
                    plan, 0, 0.0D, actualAppliedWatermarks(
                            appliedWatermarks, sources));
        }
        if (!sourcePreApplied) {
            sourceApplied = cutEnergy;
        }
        for (int index = 0; index < plan.substepCount(); index++) {
            long fromTick = plan.substepStartTick(index);
            long toTick = plan.substepEndTick(index);
            double sourceEnergy;
            if (sourcePreApplied || sourceCursor >= toTick) {
                sourceEnergy = 0.0D;
            } else if (sourceCursor == fromTick) {
                sourceEnergy = sources.apply(epoch, fromTick, toTick);
                sourceCursor = toTick;
            } else {
                throw new IllegalStateException(
                        "source cursor does not match the transport retry interval");
            }
            if (!Double.isFinite(sourceEnergy)) {
                return numericDegraded(
                        plan, executedTransport, sourceApplied,
                        actualAppliedWatermarks(appliedWatermarks, sources));
            }
            double adjusted = sourceEnergy - compensation;
            double next = sourceApplied + adjusted;
            if (!Double.isFinite(next)) {
                return numericDegraded(
                        plan, executedTransport, sourceApplied,
                        actualAppliedWatermarks(appliedWatermarks, sources));
            }
            compensation = (next - sourceApplied) - adjusted;
            sourceApplied = next;
            double dtSeconds = plan.substepDtSeconds(index);
            ThermalSweep.Result sweepResult;
            try {
                sweepResult = airSweep.applyAfterPreflight(
                        referenceTemperatureC,
                        epoch,
                        dtSeconds,
                        direction
                );
            } catch (RuntimeException failure) {
                throw ExecutionFailure.rolledBackSubstep(fromTick, failure);
            }
            numericDegraded |= sweepResult.numericDegradedOperations() != 0;
            executedTransport++;
        }

        if (sourceCursor < epoch.targetTick()) {
            double skippedIntervalSource = sources.apply(
                    epoch,
                    sourceCursor,
                    epoch.targetTick()
            );
            if (!Double.isFinite(skippedIntervalSource)) {
                return numericDegraded(
                        plan, executedTransport, sourceApplied,
                        actualAppliedWatermarks(appliedWatermarks, sources));
            }
            double adjusted = skippedIntervalSource - compensation;
            double next = sourceApplied + adjusted;
            if (!Double.isFinite(next)) {
                return numericDegraded(
                        plan, executedTransport, sourceApplied,
                        actualAppliedWatermarks(appliedWatermarks, sources));
            }
            sourceApplied = next;
        }

        return new Report(
                numericDegraded ? Status.NUMERIC_DEGRADED : Status.COMPLETED,
                plan.status(),
                executedTransport,
                plan.skippedTransportTicks(),
                plan.skippedPhaseTicks(),
                sourceApplied,
                epoch.sealedWatermarks(),
                actualAppliedWatermarks(appliedWatermarks, sources)
        );
    }

    private static Report numericDegraded(
            ThermalStepPlan plan,
            int executedTransport,
            double sourceApplied,
            InputWatermarks appliedWatermarks
    ) {
        return new Report(
                Status.NUMERIC_DEGRADED,
                plan.status(),
                executedTransport,
                plan.skippedTransportTicks(),
                plan.skippedPhaseTicks(),
                sourceApplied,
                plan.epoch().sealedWatermarks(),
                appliedWatermarks
        );
    }

    private static InputWatermarks actualAppliedWatermarks(
            InputWatermarks appliedWatermarks,
            ThermalSourceTimeline sources
    ) {
        return appliedWatermarks == null
                ? null
                : appliedWatermarks.withSource(sources.appliedWatermark());
    }

    /** Carries the exact safe continuation boundary after a transport failure. */
    public static final class ExecutionFailure extends RuntimeException {
        private final long retryFromTick;
        private final boolean failedBeforeMutation;

        private ExecutionFailure(
                long retryFromTick,
                boolean failedBeforeMutation,
                RuntimeException cause
        ) {
            super(failedBeforeMutation
                    ? "thermal transport preflight failed"
                    : "thermal transport substep failed and was rolled back",
                    cause);
            this.retryFromTick = retryFromTick;
            this.failedBeforeMutation = failedBeforeMutation;
        }

        private static ExecutionFailure preflight(
                long retryFromTick,
                RuntimeException cause
        ) {
            return new ExecutionFailure(retryFromTick, true, cause);
        }

        private static ExecutionFailure rolledBackSubstep(
                long retryFromTick,
                RuntimeException cause
        ) {
            return new ExecutionFailure(retryFromTick, false, cause);
        }

        public long retryFromTick() {
            return retryFromTick;
        }

        public boolean failedBeforeMutation() {
            return failedBeforeMutation;
        }
    }
}
