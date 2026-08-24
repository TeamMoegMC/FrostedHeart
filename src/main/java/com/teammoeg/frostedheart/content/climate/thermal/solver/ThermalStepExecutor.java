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

        boolean sourcePreApplied = sources.isPreApplied(epoch);
        double sourceApplied = sourcePreApplied
                ? sources.preAppliedEnergyJ(epoch)
                : 0.0D;
        double compensation = 0.0D;
        int executedTransport = 0;
        boolean numericDegraded = false;
        long sourceCursor = sourcePreApplied
                ? epoch.targetTick()
                : epoch.previousTick();
        double cutEnergy = sourcePreApplied
                ? 0.0D
                : sources.apply(
                        epoch,
                        epoch.previousTick(),
                        epoch.previousTick());
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
            double sourceEnergy = sourcePreApplied
                    ? 0.0D
                    : sources.apply(epoch, fromTick, toTick);
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
            ThermalSweep.Result sweepResult = airSweep.apply(
                    referenceTemperatureC,
                    epoch,
                    dtSeconds,
                    ThermalSweep.Direction.forEpoch(epoch)
            );
            numericDegraded |= sweepResult.numericDegradedOperations() != 0;
            executedTransport++;
            if (!sourcePreApplied) {
                sourceCursor = toTick;
            }
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
}
