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

/** Immutable bounded transport/phase coverage for one epoch. */
public final class ThermalStepPlan {
    public enum Status {
        NORMAL,
        TIME_DEGRADED
    }

    private final SolveEpoch epoch;
    private final Status status;
    private final long maxSolveDeltaTicks;
    private final long[] substepStarts;
    private final long[] substepEnds;
    private final long skippedTransportTicks;
    private final long skippedPhaseTicks;

    ThermalStepPlan(
            SolveEpoch epoch,
            Status status,
            long maxSolveDeltaTicks,
            long[] substepStarts,
            long[] substepEnds,
            long skippedTransportTicks,
            long skippedPhaseTicks
    ) {
        this.epoch = Objects.requireNonNull(epoch, "epoch");
        this.status = Objects.requireNonNull(status, "status");
        this.maxSolveDeltaTicks = maxSolveDeltaTicks;
        this.substepStarts = substepStarts.clone();
        this.substepEnds = substepEnds.clone();
        this.skippedTransportTicks = skippedTransportTicks;
        this.skippedPhaseTicks = skippedPhaseTicks;
        validate();
    }

    public SolveEpoch epoch() {
        return epoch;
    }

    public Status status() {
        return status;
    }

    public int substepCount() {
        return substepStarts.length;
    }

    public long substepStartTick(int index) {
        return substepStarts[index];
    }

    public long substepEndTick(int index) {
        return substepEnds[index];
    }

    public long substepTicks(int index) {
        return substepEnds[index] - substepStarts[index];
    }

    public double substepDtSeconds(int index) {
        return substepTicks(index) / SolveEpoch.TICKS_PER_SECOND;
    }

    public long coveredTransportTicks() {
        long covered = 0L;
        for (int index = 0; index < substepStarts.length; index++) {
            covered += substepTicks(index);
        }
        return covered;
    }

    public long skippedTransportTicks() {
        return skippedTransportTicks;
    }

    public long skippedPhaseTicks() {
        return skippedPhaseTicks;
    }

    public long sourceCoverageTicks() {
        return epoch.durationTicks();
    }

    private void validate() {
        if (maxSolveDeltaTicks <= 0L) {
            throw new IllegalArgumentException("maxSolveDeltaTicks must be positive");
        }
        if (substepStarts.length != substepEnds.length) {
            throw new IllegalArgumentException("substep start/end arrays must match");
        }
        long expectedStart = epoch.previousTick();
        for (int index = 0; index < substepStarts.length; index++) {
            long start = substepStarts[index];
            long end = substepEnds[index];
            if (start != expectedStart || end <= start) {
                throw new IllegalArgumentException("thermal substeps must form a positive prefix");
            }
            if (end - start > maxSolveDeltaTicks) {
                throw new IllegalArgumentException("thermal substep exceeds maxSolveDeltaTicks");
            }
            expectedStart = end;
        }
        if (expectedStart > epoch.targetTick()) {
            throw new IllegalArgumentException("thermal substeps exceed the epoch target");
        }
        long uncovered = epoch.targetTick() - expectedStart;
        if (skippedTransportTicks != uncovered || skippedPhaseTicks != uncovered) {
            throw new IllegalArgumentException(
                    "skipped transport/phase ticks must equal the uncovered epoch suffix");
        }
        if (status == Status.NORMAL && uncovered != 0L) {
            throw new IllegalArgumentException("a NORMAL plan cannot skip thermal time");
        }
    }
}
