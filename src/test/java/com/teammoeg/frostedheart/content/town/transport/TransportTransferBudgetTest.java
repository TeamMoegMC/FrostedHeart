/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportTransferBudgetTest {
    @Test
    void configuredRateDefinesBaseIntervalBoundaries() {
        assertEquals(20, TransportTransferBudget.baseIntervalTicksFor(1));
        assertEquals(20, TransportTransferBudget.baseIntervalTicksFor(32));
        assertEquals(10, TransportTransferBudget.baseIntervalTicksFor(33));
        assertEquals(10, TransportTransferBudget.baseIntervalTicksFor(64));
        assertEquals(5, TransportTransferBudget.baseIntervalTicksFor(65));
        assertEquals(5, TransportTransferBudget.baseIntervalTicksFor(128));
        assertEquals(2, TransportTransferBudget.baseIntervalTicksFor(129));
        assertEquals(2, TransportTransferBudget.baseIntervalTicksFor(640));
        assertEquals(1, TransportTransferBudget.baseIntervalTicksFor(641));
        assertEquals(1, TransportTransferBudget.baseIntervalTicksFor(1280));
    }

    @Test
    void firstAttemptWaitsForConfiguredBaseInterval() {
        TransportTransferBudget slow = budget(32);
        assertEquals(0, slow.beginAttempt(0, 32.0));
        for (long time = 1; time < 20; time++) {
            assertEquals(0, slow.beginAttempt(time, 32.0), "time=" + time);
        }
        assertEquals(32, slow.beginAttempt(20, 32.0));

        TransportTransferBudget fast = budget(1280);
        assertEquals(0, fast.beginAttempt(0, 1280.0));
        assertEquals(64, fast.beginAttempt(1, 1280.0));
    }

    @Test
    void failureDoublesIntervalAndCapsAtEightyTicks() {
        TransportTransferBudget budget = budget(64);
        assertEquals(10, budget.baseIntervalTicks());

        long time = failOnce(budget, 64, 0);
        assertEquals(20, budget.currentIntervalTicks());

        time = failOnce(budget, 64, time + 1);
        assertEquals(40, budget.currentIntervalTicks());

        time = failOnce(budget, 64, time + 1);
        assertEquals(80, budget.currentIntervalTicks());

        time = failOnce(budget, 64, time + 1);
        assertEquals(80, budget.currentIntervalTicks());
    }

    @Test
    void successHalvesIntervalUntilBaseInterval() {
        TransportTransferBudget budget = budget(64);
        long time = failOnce(budget, 64, 0);
        assertEquals(20, budget.currentIntervalTicks());

        time = succeedOnce(budget, 64, time + 1, 1);
        assertEquals(10, budget.currentIntervalTicks());

        budget = budget(64);
        time = 0;
        for (int i = 0; i < 3; i++) {
            time = failOnce(budget, 64, time + 1);
        }
        assertEquals(80, budget.currentIntervalTicks());

        time = succeedOnce(budget, 64, time + 1, 1);
        assertEquals(40, budget.currentIntervalTicks());

        time = succeedOnce(budget, 64, time + 1, 1);
        assertEquals(20, budget.currentIntervalTicks());

        time = succeedOnce(budget, 64, time + 1, 1);
        assertEquals(10, budget.currentIntervalTicks());
    }

    @Test
    void zeroEffectiveRateAndInsufficientTokensAreNotFailures() {
        TransportTransferBudget budget = budget(1);
        assertEquals(0, budget.beginAttempt(0, 0.0));
        assertEquals(20, budget.currentIntervalTicks());

        assertEquals(0, budget.beginAttempt(1, 0.5));
        assertEquals(20, budget.currentIntervalTicks());
        assertEquals(0, budget.beginAttempt(2, 0.5));
        assertEquals(20, budget.currentIntervalTicks());
    }

    @Test
    void nonFiniteEffectiveRateIsTreatedAsZero() {
        TransportTransferBudget budget = budget(1);
        assertEquals(0, budget.beginAttempt(0, Double.NaN));
        assertEquals(0, budget.beginAttempt(1, Double.POSITIVE_INFINITY));
        assertEquals(20, budget.currentIntervalTicks());
    }

    @Test
    void tokenCapacityCapsAtSixtyFourAndDropsOverflow() {
        TransportTransferBudget budget = budget(1280);
        assertEquals(0, budget.beginAttempt(0, 1280.0));
        assertEquals(64, budget.beginAttempt(1000, 1280.0));
        assertEquals(64.0, budget.getTokens(), 1.0e-9);
    }

    @Test
    void fractionalRateCarriesRemainderAcrossSeconds() {
        TransportTransferBudget budget = budget(20);
        assertEquals(0, budget.beginAttempt(0, 17.25));
        int[] expected = {17, 17, 17, 18};
        for (int second = 1; second <= expected.length; second++) {
            long time = second * 20L;
            int batch = budget.beginAttempt(time, 17.25);
            assertEquals(expected[second - 1], batch, "second=" + second);
            budget.recordSuccess(batch, time);
        }
        assertEquals(0.0, budget.getTokens(), 1.0e-9);
    }

    @Test
    void firstSuccessAfterBackoffClearsRemainingTokens() {
        TransportTransferBudget budget = budget(64);
        long time = failOnce(budget, 64, 0);
        assertEquals(20, budget.currentIntervalTicks());

        time = nextBudgetTime(budget, 64, time + 1);
        int batch = budget.beginAttempt(time, 64.0);
        assertTrue(batch > 0);
        budget.recordSuccess(1, time);

        assertEquals(0.0, budget.getTokens(), 1.0e-9);
        assertEquals(10, budget.currentIntervalTicks());
    }

    @Test
    void failureDoesNotConsumeTokens() {
        TransportTransferBudget budget = budget(64);
        long time = nextBudgetTime(budget, 64, 0);
        double before = budget.getTokens();
        budget.recordFailure(time);
        assertEquals(before, budget.getTokens(), 1.0e-9);
    }

    @Test
    void independentInstancesDoNotShareState() {
        TransportTransferBudget first = budget(64);
        TransportTransferBudget second = budget(64);
        assertEquals(0, first.beginAttempt(0, 64.0));
        assertEquals(0, second.beginAttempt(0, 64.0));
        assertEquals(64, first.beginAttempt(20, 64.0));
        assertEquals(64, second.beginAttempt(20, 64.0));
    }

    @Test
    void resetPreventsOfflineAllowanceAccumulation() {
        TransportTransferBudget budget = budget(64);
        budget.beginAttempt(0, 64.0);
        budget.reset();
        assertEquals(0, budget.beginAttempt(1000, 64.0));
        assertEquals(0.0, budget.getTokens(), 1.0e-9);
    }

    @Test
    void changingConfiguredRateRebuildsIntervalAndClearsTokens() {
        TransportTransferBudget budget = budget(64);
        budget.beginAttempt(0, 64.0);
        budget.beginAttempt(20, 64.0);
        assertEquals(64.0, budget.getTokens(), 1.0e-9);

        budget.configure(1);
        assertEquals(20, budget.baseIntervalTicks());
        assertEquals(20, budget.currentIntervalTicks());
        assertEquals(0.0, budget.getTokens(), 1.0e-9);
        assertEquals(0, budget.beginAttempt(21, 1.0));
    }

    @Test
    void pauseClearsTokensWithoutClearingBackoff() {
        TransportTransferBudget budget = budget(64);
        long time = failOnce(budget, 64, 0);
        assertEquals(20, budget.currentIntervalTicks());
        budget.pause(time + 100);
        assertEquals(0.0, budget.getTokens(), 1.0e-9);
        assertEquals(20, budget.currentIntervalTicks());
    }

    @Test
    void deferSchedulesNextProbeAtCurrentIntervalWithoutClearingBackoff() {
        TransportTransferBudget budget = budget(64);
        assertEquals(0, budget.beginAttempt(0, 64.0));
        assertEquals(32, budget.beginAttempt(10, 64.0));
        budget.recordFailure(10);
        assertEquals(20, budget.currentIntervalTicks());

        assertEquals(64, budget.beginAttempt(30, 64.0));
        budget.defer(30);
        assertEquals(0.0, budget.getTokens(), 1.0e-9);
        assertEquals(20, budget.currentIntervalTicks());
        for (long time = 31; time < 50; time++) {
            assertEquals(0, budget.beginAttempt(time, 64.0), "time=" + time);
        }
        assertEquals(64, budget.beginAttempt(50, 64.0));
    }

    @Test
    void wakeMakesFutureAttemptEligibleWithoutClearingBackoff() {
        TransportTransferBudget budget = budget(64);
        long time = failOnce(budget, 64, 0);
        assertEquals(20, budget.currentIntervalTicks());
        long wakeTime = time + 5;
        budget.wake(wakeTime);
        assertEquals(64, budget.beginAttempt(wakeTime, 1280.0));
        assertEquals(20, budget.currentIntervalTicks());
    }

    @Test
    void longBlockedRecoveryMovesAtMostSixtyFourWithoutCatchUp() {
        TransportTransferBudget budget = budget(1280);
        long time = 0;
        for (int i = 0; i < 10; i++) {
            time = failOnce(budget, 1280, time + 1);
        }
        assertEquals(80, budget.currentIntervalTicks());

        time = nextBudgetTime(budget, 1280, time + 1);
        int batch = budget.beginAttempt(time, 1280.0);
        assertEquals(64, batch);
        budget.recordSuccess(batch, time);
        assertEquals(0.0, budget.getTokens(), 1.0e-9);
    }

    @Test
    void highConfiguredRateWithLowEffectiveRateWaitsForWholeItem() {
        TransportTransferBudget budget = budget(1280);
        assertEquals(1, budget.baseIntervalTicks());
        assertEquals(0, budget.beginAttempt(0, 0.5));
        assertEquals(0, budget.beginAttempt(1, 0.5));
        assertEquals(0, budget.beginAttempt(39, 0.5));
        assertEquals(1, budget.beginAttempt(40, 0.5));
    }

    @Test
    void tokenCapacityRoundsUpToOneSecondButNeverAboveSixtyFour() {
        assertEquals(1, TransportTransferBudget.tokenCapacity(0.5));
        assertEquals(18, TransportTransferBudget.tokenCapacity(17.25));
        assertEquals(64, TransportTransferBudget.tokenCapacity(64.0));
        assertEquals(64, TransportTransferBudget.tokenCapacity(1280.0));
        assertEquals(0, TransportTransferBudget.tokenCapacity(0.0));
    }

    private static TransportTransferBudget budget(int configuredRate) {
        TransportTransferBudget budget = new TransportTransferBudget();
        budget.configure(configuredRate);
        return budget;
    }

    private static long failOnce(TransportTransferBudget budget, double effectiveRate, long startTime) {
        long time = nextBudgetTime(budget, effectiveRate, startTime);
        budget.recordFailure(time);
        return time;
    }

    private static long succeedOnce(
            TransportTransferBudget budget,
            double effectiveRate,
            long startTime,
            int moved
    ) {
        long time = nextBudgetTime(budget, effectiveRate, startTime);
        budget.recordSuccess(moved, time);
        return time;
    }

    private static long nextBudgetTime(
            TransportTransferBudget budget,
            double effectiveRate,
            long startTime
    ) {
        long time = startTime;
        while (budget.beginAttempt(time, effectiveRate) <= 0) {
            time++;
        }
        return time;
    }
}
