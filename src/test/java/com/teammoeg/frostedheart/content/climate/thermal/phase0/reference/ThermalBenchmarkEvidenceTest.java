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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalBenchmarkEvidenceTest {
    @Test
    void runtimeEnvironmentCapturesRequiredReproductionFields() {
        ThermalBenchmarkEvidence.FrozenEnvironment environment =
                ThermalBenchmarkEvidence.FrozenEnvironment.captureRuntime(
                        "phase0-host-a", "1.20.1", "47.3.0", "default-dev-mod-set",
                        12, 10, 3, "phase0-default");

        assertFalse(environment.javaVersion().isBlank());
        assertFalse(environment.vmName().isBlank());
        assertTrue(environment.availableProcessors() > 0);
        assertTrue(environment.maximumHeapBytes() > 0L);
        assertEquals(12, environment.viewDistanceChunks());
        assertEquals(10, environment.simulationDistanceChunks());
    }

    @Test
    void numericPassWithoutArtifactsRemainsIncompleteEvidence() {
        ThermalBenchmarkEvidence.Run run = run(
                "descriptor-only", "outdoor-players-1",
                ThermalBenchmarkEvidence.Candidate.SPARSE_THERMAL_RUNTIME_V1,
                criteria(ThermalAcceptance.Tier.TYPICAL, 10L),
                passingMeasurement(), Map.of());

        ThermalBenchmarkEvidence.Assessment assessment = ThermalBenchmarkEvidence.assess(run);

        assertEquals(ThermalBenchmarkEvidence.Status.INCOMPLETE_EVIDENCE, assessment.status());
        assertFalse(assessment.numericResult().isPresent());
        assertTrue(assessment.missingEvidence().contains(
                ThermalBenchmarkEvidence.EvidenceKind.JFR_RECORDING));
        assertTrue(assessment.missingEvidence().contains(
                ThermalBenchmarkEvidence.EvidenceKind.PRODUCTION_LIKE_SHADOW_REPORT));
    }

    @Test
    void completeRuntimeEvidenceCanPublishMeasuredVerdict() {
        ThermalBenchmarkEvidence.Run passing = run(
                "measured-pass", "outdoor-players-1",
                ThermalBenchmarkEvidence.Candidate.LEGACY,
                criteria(ThermalAcceptance.Tier.TYPICAL, 10L),
                passingMeasurement(), completeArtifacts(ThermalBenchmarkEvidence.Candidate.LEGACY));
        ThermalBenchmarkEvidence.Run failing = run(
                "measured-fail", "outdoor-players-1",
                ThermalBenchmarkEvidence.Candidate.LEGACY,
                criteria(ThermalAcceptance.Tier.TYPICAL, 1L),
                passingMeasurement(), completeArtifacts(ThermalBenchmarkEvidence.Candidate.LEGACY));

        assertEquals(ThermalBenchmarkEvidence.Status.MEASURED_PASS,
                ThermalBenchmarkEvidence.assess(passing).status());
        assertEquals(ThermalBenchmarkEvidence.Status.MEASURED_FAIL,
                ThermalBenchmarkEvidence.assess(failing).status());
    }

    @Test
    void offlineReferenceDoesNotClaimMinecraftIntegrationEvidence() {
        Map<ThermalBenchmarkEvidence.EvidenceKind, ThermalBenchmarkEvidence.Artifact> artifacts =
                completeArtifacts(ThermalBenchmarkEvidence.Candidate.REFERENCE_FINITE_VOLUME);
        ThermalBenchmarkEvidence.Run run = run(
                "offline-reference", "outdoor-players-1",
                ThermalBenchmarkEvidence.Candidate.REFERENCE_FINITE_VOLUME,
                criteria(ThermalAcceptance.Tier.TYPICAL, 10L),
                passingMeasurement(), artifacts);

        assertFalse(artifacts.containsKey(
                ThermalBenchmarkEvidence.EvidenceKind.FORGE_GAMETEST_REPORT));
        assertFalse(artifacts.containsKey(
                ThermalBenchmarkEvidence.EvidenceKind.PRODUCTION_LIKE_SHADOW_REPORT));
        assertTrue(ThermalBenchmarkEvidence.assess(run).evidenceComplete());
    }

    @Test
    void matrixRequiresEveryWorkloadCandidatePairAndFrozenConditions() {
        ThermalAcceptance.Criteria criteria = criteria(ThermalAcceptance.Tier.TYPICAL, 10L);
        ThermalBenchmarkEvidence.Run legacy = run(
                "legacy", "outdoor-players-1", ThermalBenchmarkEvidence.Candidate.LEGACY,
                criteria, passingMeasurement(),
                completeArtifacts(ThermalBenchmarkEvidence.Candidate.LEGACY));
        ThermalBenchmarkEvidence.Run cachedWithConflict = new ThermalBenchmarkEvidence.Run(
                "cached", "outdoor-players-1",
                ThermalBenchmarkEvidence.Candidate.CACHED_ANALYTIC_SURFACE,
                differentEnvironment(), criteria(ThermalAcceptance.Tier.TYPICAL, 11L),
                passingMeasurement(),
                completeArtifacts(ThermalBenchmarkEvidence.Candidate.CACHED_ANALYTIC_SURFACE));

        ThermalBenchmarkEvidence.MatrixCoverage coverage =
                ThermalBenchmarkEvidence.matrixCoverage(List.of(legacy, cachedWithConflict));

        assertEquals(70, ThermalBenchmarkEvidence.expectedRunKeys().size());
        assertEquals(68, coverage.missingRuns().size());
        assertEquals(Set.of("outdoor-players-1"), coverage.criteriaConflicts());
        assertEquals(Set.of("outdoor-players-1"), coverage.environmentConflicts());
        assertFalse(coverage.evidenceComplete());
    }

    @Test
    void runRejectsUnknownWorkloadAndTierMismatch() {
        assertThrows(IllegalArgumentException.class, () -> run(
                "unknown", "not-a-workload", ThermalBenchmarkEvidence.Candidate.LEGACY,
                criteria(ThermalAcceptance.Tier.TYPICAL, 10L), passingMeasurement(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> run(
                "wrong-tier", "outdoor-players-1", ThermalBenchmarkEvidence.Candidate.LEGACY,
                criteria(ThermalAcceptance.Tier.STRESS, 10L), passingMeasurement(), Map.of()));
    }

    private static ThermalBenchmarkEvidence.Run run(
            String runId,
            String workloadId,
            ThermalBenchmarkEvidence.Candidate candidate,
            ThermalAcceptance.Criteria criteria,
            ThermalAcceptance.Measurement measurement,
            Map<ThermalBenchmarkEvidence.EvidenceKind, ThermalBenchmarkEvidence.Artifact> artifacts
    ) {
        return new ThermalBenchmarkEvidence.Run(
                runId, workloadId, candidate, environment(), criteria, measurement, artifacts);
    }

    private static ThermalBenchmarkEvidence.FrozenEnvironment environment() {
        return new ThermalBenchmarkEvidence.FrozenEnvironment(
                "phase0-host-a", "OpenJDK", "17", "Server VM",
                "test-os", "1", "amd64", 8, 4_000_000_000L,
                "1.20.1", "47.3.0", "default-dev-mod-set", 12, 10, 3, "phase0-default");
    }

    private static ThermalBenchmarkEvidence.FrozenEnvironment differentEnvironment() {
        return new ThermalBenchmarkEvidence.FrozenEnvironment(
                "phase0-host-b", "OpenJDK", "17", "Server VM",
                "test-os", "1", "amd64", 8, 4_000_000_000L,
                "1.20.1", "47.3.0", "default-dev-mod-set", 12, 10, 3, "phase0-default");
    }

    private static ThermalAcceptance.Criteria criteria(ThermalAcceptance.Tier tier, long latencyLimit) {
        return new ThermalAcceptance.Criteria(
                tier,
                new ThermalAcceptance.LatencyLimit(latencyLimit, latencyLimit),
                new ThermalAcceptance.LatencyLimit(latencyLimit, latencyLimit),
                19.0D,
                1_000L,
                1.0D,
                0.1D,
                2L,
                2L,
                20L,
                1.0e-9D
        );
    }

    private static ThermalAcceptance.Measurement passingMeasurement() {
        return new ThermalAcceptance.Measurement(
                new ThermalAcceptance.PerformanceMeasurement(
                        new ThermalAcceptance.LatencyMeasurement(1L, 2L, 3L),
                        new ThermalAcceptance.LatencyMeasurement(1L, 2L, 3L),
                        20.0D,
                        500L,
                        0.5D,
                        0.05D,
                        1L,
                        1L
                ),
                new ThermalAcceptance.CorrectnessMeasurement(
                        10.0D,
                        9.0D,
                        1.0D,
                        0.0D,
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
                )
        );
    }

    private static Map<ThermalBenchmarkEvidence.EvidenceKind, ThermalBenchmarkEvidence.Artifact>
            completeArtifacts(ThermalBenchmarkEvidence.Candidate candidate) {
        EnumMap<ThermalBenchmarkEvidence.EvidenceKind, ThermalBenchmarkEvidence.Artifact> artifacts =
                new EnumMap<>(ThermalBenchmarkEvidence.EvidenceKind.class);
        for (ThermalBenchmarkEvidence.EvidenceKind kind :
                ThermalBenchmarkEvidence.requiredEvidence(candidate)) {
            artifacts.put(kind, new ThermalBenchmarkEvidence.Artifact(kind, "artifact:" + kind.name()));
        }
        return Map.copyOf(artifacts);
    }
}
