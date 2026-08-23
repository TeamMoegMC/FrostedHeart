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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Canonical Phase 0 workload identities and the evidence each run must report. */
public final class ThermalWorkloadCatalog {
    private ThermalWorkloadCatalog() {
    }

    public enum Scenario {
        OUTDOOR_PLAYERS,
        SHARED_BASES,
        ISOLATED_BASES,
        HIGH_SPEED_EXPLORATION,
        DYNAMIC_BASE,
        STABLE_SOURCES,
        CHANGING_SOURCES,
        CROPS,
        DENSE_RADIATION,
        MULTI_DIMENSION_FAIRNESS
    }

    public enum RequiredOutput {
        MAIN_THREAD_P50_P95_P99,
        WORKER_P50_P95_P99,
        TPS,
        RETAINED_BYTES,
        ALLOCATIONS_AND_GC,
        WORLD_READS,
        FALLBACK_RATIO,
        PUBLICATION_AGE,
        QUEUE_AGE,
        ENERGY_LEDGERS,
        NO_ACTIVE_CHUNK_LOAD,
        PAGE_CELL_BRICK_SHARING,
        SOURCE_ACCUMULATORS,
        QUERY_FRAME_REUSE,
        TOWN_PROJECTIONS,
        ADMISSION_REFUSALS,
        HARD_CAP_BEHAVIOR,
        RESOLVER_READS,
        WATERMARK_LAG,
        TIME_DEGRADED,
        SKIPPED_TRANSPORT_AND_PHASE,
        GEOMETRY_DELTAS,
        DEPENDENCY_CLOSURE,
        BRICK_REBUILDS,
        DYNAMIC_EXCLUSION,
        SEQLOCK_RETRIES,
        PUBLISHED_BYTES,
        SOURCE_REGISTRY_COST,
        SOURCE_TOPOLOGY_WORK,
        SOURCE_INTEGRAL_ERROR,
        SOURCE_SEGMENT_REPLAY,
        RADIATION_CANDIDATES,
        RADIATION_RAYS,
        RADIATION_CACHE_RETRACE,
        PASSIVE_QUERY_CALLS,
        PASSIVE_LOOKUP_MISSES,
        UNIQUE_QUERY_REGIONS,
        CROP_OWNED_STATE,
        MULTI_DIMENSION_FAIRNESS,
        GENERATION_SAFE_UNLOAD,
        STICKY_RECOVERY
    }

    public record Workload(
            String id,
            Scenario scenario,
            ThermalAcceptance.Tier tier,
            int players,
            int bases,
            int sources,
            int crops,
            int activeDimensions,
            Set<RequiredOutput> requiredOutputs
    ) {
        public Workload {
            if (id == null || id.isBlank() || scenario == null || tier == null) {
                throw new IllegalArgumentException("id, scenario and tier are required");
            }
            requireNonNegative("players", players);
            requireNonNegative("bases", bases);
            requireNonNegative("sources", sources);
            requireNonNegative("crops", crops);
            if (activeDimensions <= 0) {
                throw new IllegalArgumentException("activeDimensions must be positive");
            }
            if (requiredOutputs == null || requiredOutputs.isEmpty()) {
                throw new IllegalArgumentException("requiredOutputs must not be empty");
            }
            requiredOutputs = Set.copyOf(requiredOutputs);
        }
    }

    private static final List<Workload> ALL = List.of(
            outdoor(1),
            outdoor(10),
            outdoor(50),
            outdoor(100),
            workload("shared-bases-100p-10b", Scenario.SHARED_BASES,
                    ThermalAcceptance.Tier.TYPICAL, 100, 10, 0, 0, 1,
                    RequiredOutput.PAGE_CELL_BRICK_SHARING,
                    RequiredOutput.SOURCE_ACCUMULATORS,
                    RequiredOutput.QUERY_FRAME_REUSE,
                    RequiredOutput.TOWN_PROJECTIONS),
            workload("isolated-bases-100p-100b", Scenario.ISOLATED_BASES,
                    ThermalAcceptance.Tier.STRESS, 100, 100, 0, 0, 1,
                    RequiredOutput.ADMISSION_REFUSALS,
                    RequiredOutput.HARD_CAP_BEHAVIOR,
                    RequiredOutput.STICKY_RECOVERY),
            workload("high-speed-exploration-100p", Scenario.HIGH_SPEED_EXPLORATION,
                    ThermalAcceptance.Tier.STRESS, 100, 0, 0, 0, 3,
                    RequiredOutput.RESOLVER_READS,
                    RequiredOutput.WATERMARK_LAG,
                    RequiredOutput.TIME_DEGRADED,
                    RequiredOutput.SKIPPED_TRANSPORT_AND_PHASE,
                    RequiredOutput.STICKY_RECOVERY),
            workload("dynamic-door-piston-fluid-base", Scenario.DYNAMIC_BASE,
                    ThermalAcceptance.Tier.STRESS, 1, 1, 0, 0, 1,
                    RequiredOutput.GEOMETRY_DELTAS,
                    RequiredOutput.DEPENDENCY_CLOSURE,
                    RequiredOutput.BRICK_REBUILDS,
                    RequiredOutput.DYNAMIC_EXCLUSION,
                    RequiredOutput.SEQLOCK_RETRIES,
                    RequiredOutput.PUBLISHED_BYTES),
            workload("stable-sources-100", Scenario.STABLE_SOURCES,
                    ThermalAcceptance.Tier.TYPICAL, 0, 0, 100, 0, 1,
                    RequiredOutput.SOURCE_REGISTRY_COST,
                    RequiredOutput.SOURCE_TOPOLOGY_WORK,
                    RequiredOutput.SOURCE_INTEGRAL_ERROR),
            workload("changing-sources-100", Scenario.CHANGING_SOURCES,
                    ThermalAcceptance.Tier.STRESS, 0, 0, 100, 0, 1,
                    RequiredOutput.SOURCE_REGISTRY_COST,
                    RequiredOutput.SOURCE_INTEGRAL_ERROR,
                    RequiredOutput.SOURCE_SEGMENT_REPLAY,
                    RequiredOutput.STICKY_RECOVERY),
            crops(10_000, ThermalAcceptance.Tier.TYPICAL),
            crops(50_000, ThermalAcceptance.Tier.STRESS),
            workload("dense-radiation-100p", Scenario.DENSE_RADIATION,
                    ThermalAcceptance.Tier.STRESS, 100, 0, 0, 0, 1,
                    RequiredOutput.RADIATION_CANDIDATES,
                    RequiredOutput.RADIATION_RAYS,
                    RequiredOutput.RADIATION_CACHE_RETRACE),
            workload("three-dimension-fairness-100p", Scenario.MULTI_DIMENSION_FAIRNESS,
                    ThermalAcceptance.Tier.TYPICAL, 100, 0, 0, 0, 3,
                    RequiredOutput.MULTI_DIMENSION_FAIRNESS,
                    RequiredOutput.GENERATION_SAFE_UNLOAD,
                    RequiredOutput.STICKY_RECOVERY)
    );

    public static List<Workload> all() {
        return ALL;
    }

    public static Workload byId(String id) {
        return ALL.stream()
                .filter(workload -> workload.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown workload: " + id));
    }

    private static Workload outdoor(int players) {
        return workload(
                "outdoor-players-" + players,
                Scenario.OUTDOOR_PLAYERS,
                ThermalAcceptance.Tier.TYPICAL,
                players,
                0,
                0,
                0,
                1
        );
    }

    private static Workload crops(int count, ThermalAcceptance.Tier tier) {
        return workload(
                "crops-" + count,
                Scenario.CROPS,
                tier,
                0,
                0,
                0,
                count,
                1,
                RequiredOutput.PASSIVE_QUERY_CALLS,
                RequiredOutput.PASSIVE_LOOKUP_MISSES,
                RequiredOutput.UNIQUE_QUERY_REGIONS,
                RequiredOutput.CROP_OWNED_STATE
        );
    }

    private static Workload workload(
            String id,
            Scenario scenario,
            ThermalAcceptance.Tier tier,
            int players,
            int bases,
            int sources,
            int crops,
            int activeDimensions,
            RequiredOutput... scenarioOutputs
    ) {
        EnumSet<RequiredOutput> outputs = EnumSet.of(
                RequiredOutput.MAIN_THREAD_P50_P95_P99,
                RequiredOutput.WORKER_P50_P95_P99,
                RequiredOutput.TPS,
                RequiredOutput.RETAINED_BYTES,
                RequiredOutput.ALLOCATIONS_AND_GC,
                RequiredOutput.WORLD_READS,
                RequiredOutput.FALLBACK_RATIO,
                RequiredOutput.PUBLICATION_AGE,
                RequiredOutput.QUEUE_AGE,
                RequiredOutput.ENERGY_LEDGERS,
                RequiredOutput.NO_ACTIVE_CHUNK_LOAD
        );
        for (RequiredOutput scenarioOutput : scenarioOutputs) {
            outputs.add(scenarioOutput);
        }
        return new Workload(
                id,
                scenario,
                tier,
                players,
                bases,
                sources,
                crops,
                activeDimensions,
                outputs
        );
    }

    private static void requireNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
