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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Evidence envelope for Phase 0b measurements. Numeric acceptance alone is
 * deliberately insufficient: a run is measured only when every required
 * artifact and its frozen environment are present.
 */
public final class ThermalBenchmarkEvidence {
    private ThermalBenchmarkEvidence() {
    }

    public enum Candidate {
        LEGACY,
        CACHED_ANALYTIC_SURFACE,
        SPARSE_THERMAL_RUNTIME_V1,
        PER_BLOCK_SPARSE_GRAPH,
        REFERENCE_FINITE_VOLUME
    }

    public enum EvidenceKind {
        ENVIRONMENT_MANIFEST,
        JFR_RECORDING,
        JMH_RESULT,
        RETAINED_HEAP_REPORT,
        FORGE_GAMETEST_REPORT,
        PRODUCTION_LIKE_SHADOW_REPORT,
        CORRECTNESS_REPORT
    }

    public enum Status {
        INCOMPLETE_EVIDENCE,
        MEASURED_PASS,
        MEASURED_FAIL
    }

    public record FrozenEnvironment(
            String hardwareProfile,
            String javaVendor,
            String javaVersion,
            String vmName,
            String osName,
            String osVersion,
            String osArchitecture,
            int availableProcessors,
            long maximumHeapBytes,
            String minecraftVersion,
            String forgeVersion,
            String modSet,
            int viewDistanceChunks,
            int simulationDistanceChunks,
            int randomTickSpeed,
            String configProfile
    ) {
        public FrozenEnvironment {
            requireText("hardwareProfile", hardwareProfile);
            requireText("javaVendor", javaVendor);
            requireText("javaVersion", javaVersion);
            requireText("vmName", vmName);
            requireText("osName", osName);
            requireText("osVersion", osVersion);
            requireText("osArchitecture", osArchitecture);
            requireText("minecraftVersion", minecraftVersion);
            requireText("forgeVersion", forgeVersion);
            requireText("modSet", modSet);
            requireText("configProfile", configProfile);
            if (availableProcessors <= 0) {
                throw new IllegalArgumentException("availableProcessors must be positive");
            }
            if (maximumHeapBytes <= 0L) {
                throw new IllegalArgumentException("maximumHeapBytes must be positive");
            }
            if (viewDistanceChunks <= 0 || simulationDistanceChunks <= 0) {
                throw new IllegalArgumentException("view and simulation distance must be positive");
            }
            if (randomTickSpeed < 0) {
                throw new IllegalArgumentException("randomTickSpeed must be non-negative");
            }
        }

        public static FrozenEnvironment captureRuntime(
                String hardwareProfile,
                String minecraftVersion,
                String forgeVersion,
                String modSet,
                int viewDistanceChunks,
                int simulationDistanceChunks,
                int randomTickSpeed,
                String configProfile
        ) {
            Runtime runtime = Runtime.getRuntime();
            return new FrozenEnvironment(
                    hardwareProfile,
                    systemProperty("java.vendor"),
                    systemProperty("java.version"),
                    systemProperty("java.vm.name"),
                    systemProperty("os.name"),
                    systemProperty("os.version"),
                    systemProperty("os.arch"),
                    runtime.availableProcessors(),
                    runtime.maxMemory(),
                    minecraftVersion,
                    forgeVersion,
                    modSet,
                    viewDistanceChunks,
                    simulationDistanceChunks,
                    randomTickSpeed,
                    configProfile
            );
        }
    }

    public record Artifact(EvidenceKind kind, String locator) {
        public Artifact {
            if (kind == null) {
                throw new IllegalArgumentException("artifact kind is required");
            }
            requireText("artifact locator", locator);
        }
    }

    public record Run(
            String runId,
            String workloadId,
            Candidate candidate,
            FrozenEnvironment environment,
            ThermalAcceptance.Criteria criteria,
            ThermalAcceptance.Measurement measurement,
            Map<EvidenceKind, Artifact> artifacts
    ) {
        public Run {
            requireText("runId", runId);
            requireText("workloadId", workloadId);
            if (candidate == null || environment == null || criteria == null || measurement == null) {
                throw new IllegalArgumentException(
                        "candidate, environment, criteria and measurement are required");
            }
            ThermalWorkloadCatalog.Workload workload = ThermalWorkloadCatalog.byId(workloadId);
            if (criteria.tier() != workload.tier()) {
                throw new IllegalArgumentException("criteria tier must match workload tier");
            }
            if (artifacts == null) {
                throw new IllegalArgumentException("artifacts are required");
            }
            Map<EvidenceKind, Artifact> copy = new HashMap<>();
            for (Map.Entry<EvidenceKind, Artifact> entry : artifacts.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null
                        || entry.getKey() != entry.getValue().kind()) {
                    throw new IllegalArgumentException("artifact map key must match artifact kind");
                }
                copy.put(entry.getKey(), entry.getValue());
            }
            artifacts = Map.copyOf(copy);
        }

        public RunKey key() {
            return new RunKey(workloadId, candidate);
        }
    }

    public record RunKey(String workloadId, Candidate candidate) {
        public RunKey {
            requireText("workloadId", workloadId);
            if (candidate == null) {
                throw new IllegalArgumentException("candidate is required");
            }
        }
    }

    public record Assessment(
            Status status,
            Set<EvidenceKind> missingEvidence,
            Optional<ThermalAcceptance.Result> numericResult
    ) {
        public Assessment {
            if (status == null || missingEvidence == null || numericResult == null) {
                throw new IllegalArgumentException("assessment fields are required");
            }
            missingEvidence = Set.copyOf(missingEvidence);
            if (status == Status.INCOMPLETE_EVIDENCE && numericResult.isPresent()) {
                throw new IllegalArgumentException("incomplete evidence cannot publish a numeric verdict");
            }
            if (status != Status.INCOMPLETE_EVIDENCE && numericResult.isEmpty()) {
                throw new IllegalArgumentException("measured evidence requires a numeric verdict");
            }
        }

        public boolean evidenceComplete() {
            return status != Status.INCOMPLETE_EVIDENCE;
        }
    }

    public record MatrixCoverage(
            Set<RunKey> missingRuns,
            Set<String> criteriaConflicts,
            Set<String> environmentConflicts
    ) {
        public MatrixCoverage {
            missingRuns = Set.copyOf(missingRuns);
            criteriaConflicts = Set.copyOf(criteriaConflicts);
            environmentConflicts = Set.copyOf(environmentConflicts);
        }

        public boolean evidenceComplete() {
            return missingRuns.isEmpty()
                    && criteriaConflicts.isEmpty()
                    && environmentConflicts.isEmpty();
        }
    }

    public static Set<EvidenceKind> requiredEvidence(Candidate candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate is required");
        }
        EnumSet<EvidenceKind> required = EnumSet.of(
                EvidenceKind.ENVIRONMENT_MANIFEST,
                EvidenceKind.JFR_RECORDING,
                EvidenceKind.JMH_RESULT,
                EvidenceKind.RETAINED_HEAP_REPORT,
                EvidenceKind.CORRECTNESS_REPORT
        );
        if (candidate != Candidate.REFERENCE_FINITE_VOLUME) {
            required.add(EvidenceKind.FORGE_GAMETEST_REPORT);
            required.add(EvidenceKind.PRODUCTION_LIKE_SHADOW_REPORT);
        }
        return Set.copyOf(required);
    }

    public static Assessment assess(Run run) {
        if (run == null) {
            throw new IllegalArgumentException("run is required");
        }
        EnumSet<EvidenceKind> missing = EnumSet.copyOf(requiredEvidence(run.candidate()));
        missing.removeAll(run.artifacts().keySet());
        if (!missing.isEmpty()) {
            return new Assessment(Status.INCOMPLETE_EVIDENCE, missing, Optional.empty());
        }
        ThermalAcceptance.Result result = ThermalAcceptance.evaluate(run.criteria(), run.measurement());
        return new Assessment(
                result.passed() ? Status.MEASURED_PASS : Status.MEASURED_FAIL,
                Set.of(),
                Optional.of(result)
        );
    }

    public static MatrixCoverage matrixCoverage(List<Run> runs) {
        if (runs == null) {
            throw new IllegalArgumentException("runs are required");
        }
        Set<RunKey> expected = new LinkedHashSet<>();
        for (ThermalWorkloadCatalog.Workload workload : ThermalWorkloadCatalog.all()) {
            for (Candidate candidate : Candidate.values()) {
                expected.add(new RunKey(workload.id(), candidate));
            }
        }

        Map<RunKey, Run> completeRuns = new HashMap<>();
        Map<String, ThermalAcceptance.Criteria> criteriaByWorkload = new HashMap<>();
        Map<String, FrozenEnvironment> environmentByWorkload = new HashMap<>();
        Set<String> criteriaConflicts = new LinkedHashSet<>();
        Set<String> environmentConflicts = new LinkedHashSet<>();
        for (Run run : runs) {
            if (run == null) {
                throw new IllegalArgumentException("runs must not contain null");
            }
            if (assess(run).evidenceComplete()) {
                Run duplicate = completeRuns.putIfAbsent(run.key(), run);
                if (duplicate != null) {
                    throw new IllegalArgumentException("duplicate complete run: " + run.key());
                }
                ThermalAcceptance.Criteria previousCriteria =
                        criteriaByWorkload.putIfAbsent(run.workloadId(), run.criteria());
                if (previousCriteria != null && !previousCriteria.equals(run.criteria())) {
                    criteriaConflicts.add(run.workloadId());
                }
                FrozenEnvironment previousEnvironment =
                        environmentByWorkload.putIfAbsent(run.workloadId(), run.environment());
                if (previousEnvironment != null && !previousEnvironment.equals(run.environment())) {
                    environmentConflicts.add(run.workloadId());
                }
            }
        }
        expected.removeAll(completeRuns.keySet());
        return new MatrixCoverage(expected, criteriaConflicts, environmentConflicts);
    }

    public static List<RunKey> expectedRunKeys() {
        List<RunKey> keys = new ArrayList<>();
        for (ThermalWorkloadCatalog.Workload workload : ThermalWorkloadCatalog.all()) {
            for (Candidate candidate : Candidate.values()) {
                keys.add(new RunKey(workload.id(), candidate));
            }
        }
        return List.copyOf(keys);
    }

    private static String systemProperty(String name) {
        return System.getProperty(name, "unknown");
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
