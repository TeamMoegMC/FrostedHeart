/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.phase0.reference;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalAcceptanceTest {
    @Test
    void typicalWorkloadRequiresZeroLossAndZeroSkippedPhysics() {
        ThermalAcceptance.Result passing = ThermalAcceptance.evaluate(
                criteria(ThermalAcceptance.Tier.TYPICAL),
                measurement(correctness(100.0, 100.0, 0.0))
        );
        assertTrue(passing.passed());
        assertTrue(passing.violations().isEmpty());

        ThermalAcceptance.CorrectnessMeasurement failingCorrectness =
                new ThermalAcceptance.CorrectnessMeasurement(
                        100.0,
                        90.0,
                        0.0,
                        2.0,
                        1L,
                        1L,
                        2L,
                        3L,
                        true,
                        false,
                        0L,
                        true,
                        true,
                        true,
                        true,
                        0L
                );
        ThermalAcceptance.Result failing = ThermalAcceptance.evaluate(
                criteria(ThermalAcceptance.Tier.TYPICAL),
                measurement(failingCorrectness)
        );
        Set<ThermalAcceptance.Check> violations = violationChecks(failing);

        assertFalse(failing.passed());
        assertTrue(violations.contains(ThermalAcceptance.Check.SOURCE_ENERGY_BALANCE));
        assertTrue(violations.contains(ThermalAcceptance.Check.SOURCE_RESYNC_LOSS));
        assertTrue(violations.contains(ThermalAcceptance.Check.SOURCE_EXTERNAL_LOSS_INTERVALS));
        assertTrue(violations.contains(ThermalAcceptance.Check.TIME_DEGRADED));
        assertTrue(violations.contains(ThermalAcceptance.Check.SKIPPED_TRANSPORT_TICKS));
        assertTrue(violations.contains(ThermalAcceptance.Check.SKIPPED_PHASE_TICKS));
    }

    @Test
    void stressWorkloadAllowsAttributedLossButRequiresBoundedRecovery() {
        ThermalAcceptance.CorrectnessMeasurement failingCorrectness =
                new ThermalAcceptance.CorrectnessMeasurement(
                        100.0,
                        80.0,
                        20.0,
                        5.0,
                        1L,
                        2L,
                        4L,
                        4L,
                        false,
                        true,
                        1L,
                        false,
                        false,
                        false,
                        false,
                        21L
                );
        ThermalAcceptance.Result result = ThermalAcceptance.evaluate(
                criteria(ThermalAcceptance.Tier.STRESS),
                measurement(failingCorrectness)
        );
        Set<ThermalAcceptance.Check> violations = violationChecks(result);

        assertFalse(result.passed());
        assertFalse(violations.contains(ThermalAcceptance.Check.SOURCE_RESYNC_LOSS));
        assertTrue(violations.contains(ThermalAcceptance.Check.NO_ACTIVE_CHUNK_LOAD));
        assertTrue(violations.contains(ThermalAcceptance.Check.NO_UNBOUNDED_BACKLOG));
        assertTrue(violations.contains(ThermalAcceptance.Check.HARD_CAPS_EFFECTIVE));
        assertTrue(violations.contains(ThermalAcceptance.Check.DEGRADATION_BOUNDED));
        assertTrue(violations.contains(ThermalAcceptance.Check.DEGRADATION_ATTRIBUTED));
        assertTrue(violations.contains(ThermalAcceptance.Check.DEGRADATION_OBSERVABLE));
        assertTrue(violations.contains(ThermalAcceptance.Check.STICKY_RECOVERY_CONVERGED));
        assertTrue(violations.contains(ThermalAcceptance.Check.RECOVERY_OLDEST_AGE));
    }

    @Test
    void performanceViolationsAreReportedIndividually() {
        ThermalAcceptance.PerformanceMeasurement performance =
                new ThermalAcceptance.PerformanceMeasurement(
                        new ThermalAcceptance.LatencyMeasurement(1_000_000L, 3_000_000L, 5_000_000L),
                        new ThermalAcceptance.LatencyMeasurement(1_000_000L, 4_000_000L, 6_000_000L),
                        18.0,
                        9_000_000L,
                        2_000.0,
                        0.02,
                        3L,
                        5L
                );
        ThermalAcceptance.Result result = ThermalAcceptance.evaluate(
                criteria(ThermalAcceptance.Tier.TYPICAL),
                new ThermalAcceptance.Measurement(
                        performance,
                        correctness(0.0, 0.0, 0.0)
                )
        );

        Set<ThermalAcceptance.Check> violations = violationChecks(result);
        assertTrue(violations.contains(ThermalAcceptance.Check.MAIN_THREAD_P95));
        assertTrue(violations.contains(ThermalAcceptance.Check.MAIN_THREAD_P99));
        assertTrue(violations.contains(ThermalAcceptance.Check.WORKER_P95));
        assertTrue(violations.contains(ThermalAcceptance.Check.WORKER_P99));
        assertTrue(violations.contains(ThermalAcceptance.Check.TPS));
        assertTrue(violations.contains(ThermalAcceptance.Check.RETAINED_BYTES));
        assertTrue(violations.contains(ThermalAcceptance.Check.ALLOCATIONS_PER_TICK));
        assertTrue(violations.contains(ThermalAcceptance.Check.FALLBACK_RATIO));
        assertTrue(violations.contains(ThermalAcceptance.Check.PUBLICATION_AGE));
        assertTrue(violations.contains(ThermalAcceptance.Check.QUEUE_AGE));
    }

    private static ThermalAcceptance.Criteria criteria(ThermalAcceptance.Tier tier) {
        return new ThermalAcceptance.Criteria(
                tier,
                new ThermalAcceptance.LatencyLimit(2_000_000L, 4_000_000L),
                new ThermalAcceptance.LatencyLimit(3_000_000L, 5_000_000L),
                19.5,
                8_000_000L,
                1_000.0,
                0.01,
                2L,
                4L,
                20L,
                1.0e-9
        );
    }

    private static ThermalAcceptance.Measurement measurement(
            ThermalAcceptance.CorrectnessMeasurement correctness
    ) {
        return new ThermalAcceptance.Measurement(
                new ThermalAcceptance.PerformanceMeasurement(
                        new ThermalAcceptance.LatencyMeasurement(500_000L, 1_000_000L, 2_000_000L),
                        new ThermalAcceptance.LatencyMeasurement(500_000L, 1_500_000L, 2_500_000L),
                        20.0,
                        7_000_000L,
                        500.0,
                        0.005,
                        1L,
                        2L
                ),
                correctness
        );
    }

    private static ThermalAcceptance.CorrectnessMeasurement correctness(
            double integratedSourceEnergyJ,
            double appliedSourceEnergyJ,
            double declaredSinkEnergyJ
    ) {
        return new ThermalAcceptance.CorrectnessMeasurement(
                integratedSourceEnergyJ,
                appliedSourceEnergyJ,
                declaredSinkEnergyJ,
                0.0,
                0L,
                0L,
                0L,
                0L,
                true,
                false,
                0L,
                true,
                true,
                true,
                true,
                0L
        );
    }

    private static Set<ThermalAcceptance.Check> violationChecks(ThermalAcceptance.Result result) {
        return result.violations().stream()
                .map(ThermalAcceptance.CheckResult::check)
                .collect(Collectors.toSet());
    }
}
