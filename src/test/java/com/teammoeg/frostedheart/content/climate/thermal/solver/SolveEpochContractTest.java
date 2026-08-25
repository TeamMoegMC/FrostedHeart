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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolveEpochContractTest {
    private static final ThermalTimePolicy POLICY = new ThermalTimePolicy(5L, 20L, 1);

    @Test
    void watermarkCoverageAndGenerationGateSolveInput() {
        InputWatermarks sealed = new InputWatermarks(4L, 8L, 2L, 6L, 3L);
        InputWatermarks behind = new InputWatermarks(4L, 7L, 2L, 6L, 3L);
        InputWatermarks ahead = new InputWatermarks(5L, 9L, 2L, 7L, 4L);
        SolveEpoch epoch = new SolveEpoch(100L, 105L, 9L, 12L, sealed);

        assertFalse(behind.covers(sealed));
        assertTrue(ahead.covers(sealed));
        assertFalse(epoch.inputsSatisfiedBy(11L, ahead));
        assertFalse(epoch.inputsSatisfiedBy(12L, behind));
        assertTrue(epoch.inputsSatisfiedBy(12L, ahead));
        assertEquals(0.25D, epoch.dtSeconds());
    }

    @Test
    void schedulerCoalescesCadenceTargetsAndAllowsOnlyOneInFlightEpoch() {
        LatestSolveEpochScheduler scheduler = new LatestSolveEpochScheduler(
                2L, 0L, InputWatermarks.ZERO, POLICY);
        SealedInputFrame tick5 = frame(5L, 1L);
        SealedInputFrame tick10 = frame(10L, 2L);
        SealedInputFrame tick15 = frame(15L, 3L);

        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                scheduler.sealLatest(tick5));
        SolveEpoch first = scheduler.tryStartLatest().orElseThrow();
        assertEquals(0L, first.previousTick());
        assertEquals(5L, first.targetTick());
        assertEquals(0, scheduler.pendingTargetCount());

        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                scheduler.sealLatest(tick10));
        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                scheduler.sealLatest(tick15));
        assertEquals(1, scheduler.pendingTargetCount());
        assertTrue(scheduler.tryStartLatest().isEmpty());

        assertEquals(LatestSolveEpochScheduler.CompletionResult.COMPLETED,
                scheduler.complete(first, 2L, tick5.watermarks()));
        SolveEpoch latest = scheduler.tryStartLatest().orElseThrow();
        assertEquals(5L, latest.previousTick());
        assertEquals(15L, latest.targetTick());
        assertEquals(2L, latest.epochId());
    }

    @Test
    void completionWaitsForAllSealedWatermarksWithoutDroppingInFlightState() {
        LatestSolveEpochScheduler scheduler = new LatestSolveEpochScheduler(
                4L, 20L, InputWatermarks.ZERO, POLICY);
        SealedInputFrame frame = new SealedInputFrame(
                25L,
                4L,
                new InputWatermarks(4L, 7L, 2L, 1L, 3L)
        );
        scheduler.sealLatest(frame);
        SolveEpoch epoch = scheduler.tryStartLatest().orElseThrow();

        assertEquals(LatestSolveEpochScheduler.CompletionResult.INPUTS_PENDING,
                scheduler.complete(
                        epoch,
                        4L,
                        new InputWatermarks(4L, 6L, 2L, 1L, 3L)
                ));
        assertEquals(Optional.of(epoch), scheduler.inFlight());
        assertEquals(20L, scheduler.lastCompletedTargetTick());

        assertEquals(LatestSolveEpochScheduler.CompletionResult.COMPLETED,
                scheduler.complete(epoch, 4L, frame.watermarks()));
        assertTrue(scheduler.inFlight().isEmpty());
        assertEquals(25L, scheduler.lastCompletedTargetTick());
    }

    @Test
    void cadenceDoesNotStartEarlyAndGenerationOrWatermarkRegressionIsRejected() {
        LatestSolveEpochScheduler scheduler = new LatestSolveEpochScheduler(
                7L, 100L, new InputWatermarks(5L, 5L, 5L, 5L, 5L), POLICY);

        assertEquals(LatestSolveEpochScheduler.SealResult.GENERATION_MISMATCH,
                scheduler.sealLatest(new SealedInputFrame(
                        105L, 8L, new InputWatermarks(6L, 6L, 6L, 6L, 6L))));
        assertEquals(LatestSolveEpochScheduler.SealResult.WATERMARK_REGRESSION,
                scheduler.sealLatest(new SealedInputFrame(
                        105L, 7L, new InputWatermarks(6L, 4L, 6L, 6L, 6L))));
        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                scheduler.sealLatest(new SealedInputFrame(
                        103L, 7L, new InputWatermarks(6L, 6L, 6L, 6L, 6L))));
        assertTrue(scheduler.tryStartLatest().isEmpty());
        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                scheduler.sealLatest(new SealedInputFrame(
                        105L, 7L, new InputWatermarks(7L, 7L, 7L, 7L, 7L))));
        assertEquals(105L, scheduler.tryStartLatest().orElseThrow().targetTick());
    }

    @Test
    void urgentInputFrameBypassesSteadyCadence() {
        LatestSolveEpochScheduler scheduler = new LatestSolveEpochScheduler(
                7L, 100L, InputWatermarks.ZERO, POLICY);
        SealedInputFrame urgent = new SealedInputFrame(
                101L, 7L, new InputWatermarks(1L, 0L, 0L, 0L, 0L));

        assertEquals(LatestSolveEpochScheduler.SealResult.ACCEPTED,
                scheduler.sealLatest(urgent, true));
        SolveEpoch epoch = scheduler.tryStartLatest().orElseThrow();
        assertEquals(100L, epoch.previousTick());
        assertEquals(101L, epoch.targetTick());
    }

    @Test
    void invalidEpochAndWatermarkDomainsAreRejectedAtSealConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new InputWatermarks(-1L, 0L, 0L, 0L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new SealedInputFrame(-1L, 0L, InputWatermarks.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new SolveEpoch(10L, 9L, 1L, 0L, InputWatermarks.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new SolveEpoch(0L, 1L, 0L, 0L, InputWatermarks.ZERO));
    }

    private static SealedInputFrame frame(long tick, long watermark) {
        return new SealedInputFrame(
                tick,
                2L,
                new InputWatermarks(
                        watermark, watermark, watermark, watermark, watermark)
        );
    }
}
