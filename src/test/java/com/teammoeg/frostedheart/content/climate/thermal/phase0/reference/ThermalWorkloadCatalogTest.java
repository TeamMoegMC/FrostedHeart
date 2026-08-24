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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalWorkloadCatalogTest {
    @Test
    void catalogContainsEveryFrozenPhaseZeroScenarioScale() {
        List<ThermalWorkloadCatalog.Workload> workloads = ThermalWorkloadCatalog.all();

        assertEquals(14, workloads.size());
        assertEquals(workloads.size(), workloads.stream().map(ThermalWorkloadCatalog.Workload::id)
                .collect(java.util.stream.Collectors.toSet()).size());
        assertEquals(100, ThermalWorkloadCatalog.byId("outdoor-players-100").players());
        assertEquals(10, ThermalWorkloadCatalog.byId("shared-bases-100p-10b").bases());
        assertEquals(100, ThermalWorkloadCatalog.byId("isolated-bases-100p-100b").bases());
        assertEquals(10_000, ThermalWorkloadCatalog.byId("crops-10000").crops());
        assertEquals(50_000, ThermalWorkloadCatalog.byId("crops-50000").crops());
        assertEquals(3, ThermalWorkloadCatalog.byId("three-dimension-fairness-100p")
                .activeDimensions());
    }

    @Test
    void everyWorkloadRequiresComparableBaselineEvidence() {
        Set<ThermalWorkloadCatalog.RequiredOutput> common = Set.of(
                ThermalWorkloadCatalog.RequiredOutput.MAIN_THREAD_P50_P95_P99,
                ThermalWorkloadCatalog.RequiredOutput.WORKER_P50_P95_P99,
                ThermalWorkloadCatalog.RequiredOutput.TPS,
                ThermalWorkloadCatalog.RequiredOutput.RETAINED_BYTES,
                ThermalWorkloadCatalog.RequiredOutput.ALLOCATIONS_AND_GC,
                ThermalWorkloadCatalog.RequiredOutput.WORLD_READS,
                ThermalWorkloadCatalog.RequiredOutput.FALLBACK_RATIO,
                ThermalWorkloadCatalog.RequiredOutput.PUBLICATION_AGE,
                ThermalWorkloadCatalog.RequiredOutput.QUEUE_AGE,
                ThermalWorkloadCatalog.RequiredOutput.ENERGY_LEDGERS,
                ThermalWorkloadCatalog.RequiredOutput.NO_ACTIVE_CHUNK_LOAD
        );

        for (ThermalWorkloadCatalog.Workload workload : ThermalWorkloadCatalog.all()) {
            assertTrue(workload.requiredOutputs().containsAll(common), workload.id());
        }
    }

    @Test
    void scenarioSpecificEvidenceCannotBeLostThroughMutableViews() {
        ThermalWorkloadCatalog.Workload changingSources =
                ThermalWorkloadCatalog.byId("changing-sources-100");
        assertTrue(changingSources.requiredOutputs().contains(
                ThermalWorkloadCatalog.RequiredOutput.SOURCE_SEGMENT_REPLAY
        ));
        assertTrue(ThermalWorkloadCatalog.byId("crops-50000").requiredOutputs().contains(
                ThermalWorkloadCatalog.RequiredOutput.CROP_OWNED_STATE
        ));
        assertTrue(ThermalWorkloadCatalog.byId("dense-radiation-100p").requiredOutputs().contains(
                ThermalWorkloadCatalog.RequiredOutput.RADIATION_RAYS
        ));

        assertThrows(
                UnsupportedOperationException.class,
                () -> ThermalWorkloadCatalog.all().add(changingSources)
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> changingSources.requiredOutputs().add(
                        ThermalWorkloadCatalog.RequiredOutput.BRICK_REBUILDS
                )
        );
    }

    @Test
    void workloadIdsAreUniqueWithoutDependingOnIterationOrder() {
        Set<String> ids = new HashSet<>();
        for (ThermalWorkloadCatalog.Workload workload : ThermalWorkloadCatalog.all()) {
            assertTrue(ids.add(workload.id()), workload.id());
        }
    }
}
