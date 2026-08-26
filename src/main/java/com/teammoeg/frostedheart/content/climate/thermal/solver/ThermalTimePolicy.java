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

import java.util.Objects;

/** Uniform cadence and hard bounds for dimension-wide thermal intervals. */
public record ThermalTimePolicy(
        long uniformStepTicks,
        long maxSolveDeltaTicks,
        int maxDegradedSubsteps
) {
    public ThermalTimePolicy {
        if (uniformStepTicks <= 0L) {
            throw new IllegalArgumentException("uniformStepTicks must be positive");
        }
        if (maxSolveDeltaTicks < uniformStepTicks) {
            throw new IllegalArgumentException(
                    "maxSolveDeltaTicks must be at least uniformStepTicks");
        }
        if (maxDegradedSubsteps <= 0) {
            throw new IllegalArgumentException("maxDegradedSubsteps must be positive");
        }
    }

    public ThermalStepPlan plan(SolveEpoch epoch) {
        Objects.requireNonNull(epoch, "epoch");
        long duration = epoch.durationTicks();
        if (duration == 0L) {
            return new ThermalStepPlan(
                    epoch,
                    ThermalStepPlan.Status.NORMAL,
                    maxSolveDeltaTicks,
                    new long[0],
                    new long[0],
                    0L,
                    0L
            );
        }
        if (duration <= maxSolveDeltaTicks) {
            return new ThermalStepPlan(
                    epoch,
                    ThermalStepPlan.Status.NORMAL,
                    maxSolveDeltaTicks,
                    new long[]{epoch.previousTick()},
                    new long[]{epoch.targetTick()},
                    0L,
                    0L
            );
        }

        long maximumCovered = saturatedMultiply(maxSolveDeltaTicks, maxDegradedSubsteps);
        long coveredTicks = Math.min(duration, maximumCovered);
        int substepCount = (int) Math.min(
                maxDegradedSubsteps,
                divideRoundingUp(coveredTicks, maxSolveDeltaTicks)
        );
        long[] starts = new long[substepCount];
        long[] ends = new long[substepCount];
        long cursor = epoch.previousTick();
        long remainingCovered = coveredTicks;
        for (int index = 0; index < substepCount; index++) {
            long stepTicks = Math.min(maxSolveDeltaTicks, remainingCovered);
            starts[index] = cursor;
            cursor += stepTicks;
            ends[index] = cursor;
            remainingCovered -= stepTicks;
        }
        long skipped = duration - coveredTicks;
        return new ThermalStepPlan(
                epoch,
                ThermalStepPlan.Status.TIME_DEGRADED,
                maxSolveDeltaTicks,
                starts,
                ends,
                skipped,
                skipped
        );
    }

    private static long saturatedMultiply(long value, int multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }

    private static long divideRoundingUp(long numerator, long denominator) {
        return 1L + (numerator - 1L) / denominator;
    }
}
