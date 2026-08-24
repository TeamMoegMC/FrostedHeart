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

import java.util.ArrayList;
import java.util.List;

/** Workload-specific correctness and performance acceptance contract. */
public final class ThermalAcceptance {
    private ThermalAcceptance() {
    }

    public enum Tier {
        TYPICAL,
        STRESS
    }

    public enum Check {
        MAIN_THREAD_P95,
        MAIN_THREAD_P99,
        WORKER_P95,
        WORKER_P99,
        TPS,
        RETAINED_BYTES,
        ALLOCATIONS_PER_TICK,
        FALLBACK_RATIO,
        PUBLICATION_AGE,
        QUEUE_AGE,
        SOURCE_ENERGY_BALANCE,
        NO_ACTIVE_CHUNK_LOAD,
        NO_UNBOUNDED_BACKLOG,
        SOURCE_RESYNC_LOSS,
        SOURCE_EXTERNAL_LOSS_INTERVALS,
        TIME_DEGRADED,
        SKIPPED_TRANSPORT_TICKS,
        SKIPPED_PHASE_TICKS,
        HARD_CAPS_EFFECTIVE,
        DEGRADATION_BOUNDED,
        DEGRADATION_ATTRIBUTED,
        DEGRADATION_OBSERVABLE,
        STICKY_RECOVERY_CONVERGED,
        RECOVERY_OLDEST_AGE
    }

    public record LatencyLimit(long p95Nanos, long p99Nanos) {
        public LatencyLimit {
            requireNonNegative("p95Nanos", p95Nanos);
            requireNonNegative("p99Nanos", p99Nanos);
            if (p99Nanos < p95Nanos) {
                throw new IllegalArgumentException("p99Nanos must be at least p95Nanos");
            }
        }
    }

    public record LatencyMeasurement(long p50Nanos, long p95Nanos, long p99Nanos) {
        public LatencyMeasurement {
            requireNonNegative("p50Nanos", p50Nanos);
            requireNonNegative("p95Nanos", p95Nanos);
            requireNonNegative("p99Nanos", p99Nanos);
            if (p50Nanos > p95Nanos || p95Nanos > p99Nanos) {
                throw new IllegalArgumentException("latency percentiles must be monotonic");
            }
        }
    }

    public record Criteria(
            Tier tier,
            LatencyLimit mainThread,
            LatencyLimit worker,
            double minimumTps,
            long maximumRetainedBytes,
            double maximumAllocationsPerTick,
            double maximumFallbackRatio,
            long maximumPublicationAgeTicks,
            long maximumQueueAgeTicks,
            long maximumRecoveryOldestAgeTicks,
            double sourceEnergyToleranceJ
    ) {
        public Criteria {
            if (tier == null || mainThread == null || worker == null) {
                throw new IllegalArgumentException("tier and latency limits are required");
            }
            ThermalUnits.requireNonNegative("minimumTps", minimumTps);
            requireNonNegative("maximumRetainedBytes", maximumRetainedBytes);
            ThermalUnits.requireNonNegative("maximumAllocationsPerTick", maximumAllocationsPerTick);
            requireRatio("maximumFallbackRatio", maximumFallbackRatio);
            requireNonNegative("maximumPublicationAgeTicks", maximumPublicationAgeTicks);
            requireNonNegative("maximumQueueAgeTicks", maximumQueueAgeTicks);
            requireNonNegative("maximumRecoveryOldestAgeTicks", maximumRecoveryOldestAgeTicks);
            ThermalUnits.requireNonNegative("sourceEnergyToleranceJ", sourceEnergyToleranceJ);
        }
    }

    public record PerformanceMeasurement(
            LatencyMeasurement mainThread,
            LatencyMeasurement worker,
            double tps,
            long retainedBytes,
            double allocationsPerTick,
            double fallbackRatio,
            long publicationAgeTicks,
            long queueAgeTicks
    ) {
        public PerformanceMeasurement {
            if (mainThread == null || worker == null) {
                throw new IllegalArgumentException("latency measurements are required");
            }
            ThermalUnits.requireNonNegative("tps", tps);
            requireNonNegative("retainedBytes", retainedBytes);
            ThermalUnits.requireNonNegative("allocationsPerTick", allocationsPerTick);
            requireRatio("fallbackRatio", fallbackRatio);
            requireNonNegative("publicationAgeTicks", publicationAgeTicks);
            requireNonNegative("queueAgeTicks", queueAgeTicks);
        }
    }

    public record CorrectnessMeasurement(
            double integratedSourceEnergyJ,
            double appliedSourceEnergyJ,
            double declaredSourceSinkEnergyJ,
            double sourceResyncLossJ,
            long sourceExternalLossIntervals,
            long timeDegradedCount,
            long skippedTransportTicks,
            long skippedPhaseTicks,
            boolean hardCapsEffective,
            boolean unboundedBacklogObserved,
            long activeChunkLoads,
            boolean degradationBounded,
            boolean degradationAttributed,
            boolean degradationObservable,
            boolean stickyRecoveryConverged,
            long recoveryOldestAgeTicks
    ) {
        public CorrectnessMeasurement {
            ThermalUnits.requireFinite("integratedSourceEnergyJ", integratedSourceEnergyJ);
            ThermalUnits.requireFinite("appliedSourceEnergyJ", appliedSourceEnergyJ);
            ThermalUnits.requireFinite("declaredSourceSinkEnergyJ", declaredSourceSinkEnergyJ);
            ThermalUnits.requireFinite("sourceResyncLossJ", sourceResyncLossJ);
            requireNonNegative("sourceExternalLossIntervals", sourceExternalLossIntervals);
            requireNonNegative("timeDegradedCount", timeDegradedCount);
            requireNonNegative("skippedTransportTicks", skippedTransportTicks);
            requireNonNegative("skippedPhaseTicks", skippedPhaseTicks);
            requireNonNegative("activeChunkLoads", activeChunkLoads);
            requireNonNegative("recoveryOldestAgeTicks", recoveryOldestAgeTicks);
        }
    }

    public record Measurement(
            PerformanceMeasurement performance,
            CorrectnessMeasurement correctness
    ) {
        public Measurement {
            if (performance == null || correctness == null) {
                throw new IllegalArgumentException("performance and correctness measurements are required");
            }
        }
    }

    public record CheckResult(Check check, boolean passed, String expected, String actual) {
        public CheckResult {
            if (check == null || expected == null || actual == null) {
                throw new IllegalArgumentException("check result fields are required");
            }
        }
    }

    public record Result(List<CheckResult> checks) {
        public Result {
            checks = List.copyOf(checks);
        }

        public boolean passed() {
            return checks.stream().allMatch(CheckResult::passed);
        }

        public List<CheckResult> violations() {
            return checks.stream().filter(check -> !check.passed()).toList();
        }
    }

    public static Result evaluate(Criteria criteria, Measurement measurement) {
        if (criteria == null || measurement == null) {
            throw new IllegalArgumentException("criteria and measurement are required");
        }
        PerformanceMeasurement performance = measurement.performance();
        CorrectnessMeasurement correctness = measurement.correctness();
        List<CheckResult> checks = new ArrayList<>();

        atMost(checks, Check.MAIN_THREAD_P95, performance.mainThread().p95Nanos(),
                criteria.mainThread().p95Nanos());
        atMost(checks, Check.MAIN_THREAD_P99, performance.mainThread().p99Nanos(),
                criteria.mainThread().p99Nanos());
        atMost(checks, Check.WORKER_P95, performance.worker().p95Nanos(),
                criteria.worker().p95Nanos());
        atMost(checks, Check.WORKER_P99, performance.worker().p99Nanos(),
                criteria.worker().p99Nanos());
        atLeast(checks, Check.TPS, performance.tps(), criteria.minimumTps());
        atMost(checks, Check.RETAINED_BYTES, performance.retainedBytes(),
                criteria.maximumRetainedBytes());
        atMost(checks, Check.ALLOCATIONS_PER_TICK, performance.allocationsPerTick(),
                criteria.maximumAllocationsPerTick());
        atMost(checks, Check.FALLBACK_RATIO, performance.fallbackRatio(),
                criteria.maximumFallbackRatio());
        atMost(checks, Check.PUBLICATION_AGE, performance.publicationAgeTicks(),
                criteria.maximumPublicationAgeTicks());
        atMost(checks, Check.QUEUE_AGE, performance.queueAgeTicks(),
                criteria.maximumQueueAgeTicks());

        double sourceResidualJ = correctness.integratedSourceEnergyJ()
                - correctness.appliedSourceEnergyJ()
                - correctness.declaredSourceSinkEnergyJ();
        absoluteAtMost(checks, Check.SOURCE_ENERGY_BALANCE, sourceResidualJ,
                criteria.sourceEnergyToleranceJ());
        exactlyZero(checks, Check.NO_ACTIVE_CHUNK_LOAD, correctness.activeChunkLoads());
        booleanCheck(checks, Check.NO_UNBOUNDED_BACKLOG,
                !correctness.unboundedBacklogObserved(), "no unbounded backlog");

        if (criteria.tier() == Tier.TYPICAL) {
            absoluteAtMost(checks, Check.SOURCE_RESYNC_LOSS, correctness.sourceResyncLossJ(),
                    criteria.sourceEnergyToleranceJ());
            exactlyZero(checks, Check.SOURCE_EXTERNAL_LOSS_INTERVALS,
                    correctness.sourceExternalLossIntervals());
            exactlyZero(checks, Check.TIME_DEGRADED, correctness.timeDegradedCount());
            exactlyZero(checks, Check.SKIPPED_TRANSPORT_TICKS,
                    correctness.skippedTransportTicks());
            exactlyZero(checks, Check.SKIPPED_PHASE_TICKS, correctness.skippedPhaseTicks());
        } else {
            booleanCheck(checks, Check.HARD_CAPS_EFFECTIVE,
                    correctness.hardCapsEffective(), "hard caps effective");
            booleanCheck(checks, Check.DEGRADATION_BOUNDED,
                    correctness.degradationBounded(), "all degradation bounded");
            booleanCheck(checks, Check.DEGRADATION_ATTRIBUTED,
                    correctness.degradationAttributed(), "all degradation attributed");
            booleanCheck(checks, Check.DEGRADATION_OBSERVABLE,
                    correctness.degradationObservable(), "all degradation observable");
            booleanCheck(checks, Check.STICKY_RECOVERY_CONVERGED,
                    correctness.stickyRecoveryConverged(), "sticky recovery converged");
            atMost(checks, Check.RECOVERY_OLDEST_AGE, correctness.recoveryOldestAgeTicks(),
                    criteria.maximumRecoveryOldestAgeTicks());
        }

        return new Result(checks);
    }

    private static void exactlyZero(List<CheckResult> checks, Check check, long actual) {
        checks.add(new CheckResult(check, actual == 0L, "= 0", Long.toString(actual)));
    }

    private static void absoluteAtMost(
            List<CheckResult> checks,
            Check check,
            double actual,
            double limit
    ) {
        checks.add(new CheckResult(
                check,
                Math.abs(actual) <= limit,
                "abs(value) <= " + limit,
                Double.toString(actual)
        ));
    }

    private static void atMost(List<CheckResult> checks, Check check, long actual, long limit) {
        checks.add(new CheckResult(check, actual <= limit, "<= " + limit, Long.toString(actual)));
    }

    private static void atMost(List<CheckResult> checks, Check check, double actual, double limit) {
        checks.add(new CheckResult(check, actual <= limit, "<= " + limit, Double.toString(actual)));
    }

    private static void atLeast(List<CheckResult> checks, Check check, double actual, double limit) {
        checks.add(new CheckResult(check, actual >= limit, ">= " + limit, Double.toString(actual)));
    }

    private static void booleanCheck(
            List<CheckResult> checks,
            Check check,
            boolean passed,
            String expectation
    ) {
        checks.add(new CheckResult(check, passed, expectation, Boolean.toString(passed)));
    }

    private static void requireRatio(String name, double value) {
        ThermalUnits.requireNonNegative(name, value);
        if (value > 1.0) {
            throw new IllegalArgumentException(name + " must be at most 1");
        }
    }

    private static void requireNonNegative(String name, long value) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
