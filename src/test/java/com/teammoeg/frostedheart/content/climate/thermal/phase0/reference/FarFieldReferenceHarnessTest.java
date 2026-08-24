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

import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarFieldReferenceHarnessTest {
    private static final FarFieldReferenceHarness.FitRange FIT_RANGE =
            new FarFieldReferenceHarness.FitRange(1.0D, 1_000_000.0D);

    @Test
    void standardMatrixCoversRequiredTopologiesWindAndSourcePowers() {
        List<FarFieldReferenceHarness.Fixture> fixtures =
                FarFieldReferenceHarness.standardFixtures();
        Set<FarFieldProfileRegistry.TopologyClass> topologies = fixtures.stream()
                .map(fixture -> fixture.key().topologyClass())
                .collect(Collectors.toSet());
        Set<FarFieldProfileRegistry.WindBucket> winds = fixtures.stream()
                .map(fixture -> fixture.key().windBucket())
                .collect(Collectors.toSet());
        Set<Double> powers = fixtures.stream()
                .map(FarFieldReferenceHarness.Fixture::sourcePowerW)
                .collect(Collectors.toSet());
        Map<FarFieldProfileRegistry.Key, List<FarFieldReferenceHarness.Fixture>> byKey =
                fixtures.stream().collect(Collectors.groupingBy(
                        FarFieldReferenceHarness.Fixture::key));

        assertEquals(EnumSet.allOf(FarFieldProfileRegistry.TopologyClass.class), topologies);
        assertEquals(
                Set.of(
                        FarFieldProfileRegistry.WindBucket.CALM,
                        FarFieldProfileRegistry.WindBucket.WINDY),
                winds);
        assertEquals(Set.of(1_000.0D, 10_000.0D, 100_000.0D), powers);
        assertEquals(18, fixtures.size());
        for (List<FarFieldReferenceHarness.Fixture> bucket : byKey.values()) {
            assertEquals(3, bucket.size());
            assertEquals(2L, bucket.stream()
                    .filter(fixture -> fixture.split() == FarFieldReferenceHarness.Split.FIT)
                    .count());
            assertEquals(1L, bucket.stream()
                    .filter(fixture -> fixture.split() == FarFieldReferenceHarness.Split.HOLDOUT)
                    .count());
        }
    }

    @Test
    void looseTestToleranceApprovesAndStrictToleranceRejectsSameHoldout() {
        List<FarFieldReferenceHarness.Fixture> openCalm = fixtures(
                FarFieldProfileRegistry.TopologyClass.OPEN_SPACE,
                FarFieldProfileRegistry.WindBucket.CALM);
        FarFieldReferenceHarness.GateResult loose = FarFieldReferenceHarness.calibrate(
                openCalm,
                FIT_RANGE,
                new FarFieldReferenceHarness.Tolerances(
                        1_000_000.0D,
                        1_000_000.0D,
                        1.0e15D,
                        1.0e12D));
        FarFieldReferenceHarness.GateResult strict = FarFieldReferenceHarness.calibrate(
                openCalm,
                FIT_RANGE,
                new FarFieldReferenceHarness.Tolerances(0.0D, 0.0D, 0.0D, 0.0D));

        assertTrue(loose.approved());
        assertEquals(
                FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE,
                loose.profile().approval());
        assertFalse(strict.approved());
        assertEquals(FarFieldProfileRegistry.Approval.CANDIDATE, strict.profile().approval());
        assertEquals(1, loose.holdoutMetrics().caseCount());
        assertTrue(loose.profile().conductanceWPerK() >= FIT_RANGE.minimumConductanceWPerK());
        assertTrue(loose.profile().conductanceWPerK() <= FIT_RANGE.maximumConductanceWPerK());
    }

    @Test
    void holdoutChangesMetricsButCannotChangeFittedConductance() {
        FarFieldProfileRegistry.Key key = key();
        FarFieldReferenceHarness.Fixture fit = fixture(
                "fit", key, FarFieldReferenceHarness.Split.FIT,
                new double[]{20_000.0D}, new double[]{4_000.0D, 8_000.0D});
        FarFieldReferenceHarness.Fixture firstHoldout = fixture(
                "holdout-a", key, FarFieldReferenceHarness.Split.HOLDOUT,
                new double[]{20_000.0D}, new double[]{4_000.0D, 8_000.0D});
        FarFieldReferenceHarness.Fixture secondHoldout = fixture(
                "holdout-b", key, FarFieldReferenceHarness.Split.HOLDOUT,
                new double[]{2_000_000.0D}, new double[]{300.0D, 50.0D});
        FarFieldReferenceHarness.Tolerances limits =
                new FarFieldReferenceHarness.Tolerances(
                        1_000_000.0D, 1_000_000.0D, 1.0e15D, 1.0e12D);

        FarFieldReferenceHarness.GateResult first = FarFieldReferenceHarness.calibrate(
                List.of(fit, firstHoldout), FIT_RANGE, limits);
        FarFieldReferenceHarness.GateResult second = FarFieldReferenceHarness.calibrate(
                List.of(fit, secondHoldout), FIT_RANGE, limits);

        assertEquals(
                first.profile().conductanceWPerK(),
                second.profile().conductanceWPerK(),
                1.0e-9D);
        assertTrue(second.holdoutMetrics().maximumTemperatureErrorC()
                > first.holdoutMetrics().maximumTemperatureErrorC());
    }

    @Test
    void calibrationRejectsMixedKeysDuplicateIdsAndMissingSplit() {
        FarFieldReferenceHarness.Fixture fit = fixture(
                "same", key(), FarFieldReferenceHarness.Split.FIT,
                new double[]{20_000.0D}, new double[]{4_000.0D, 8_000.0D});
        FarFieldReferenceHarness.Fixture duplicate = fixture(
                "same", key(), FarFieldReferenceHarness.Split.HOLDOUT,
                new double[]{20_000.0D}, new double[]{4_000.0D, 8_000.0D});
        FarFieldReferenceHarness.Fixture foreign = fixture(
                "foreign",
                new FarFieldProfileRegistry.Key(
                        0,
                        FarFieldProfileRegistry.OpeningClass.HALF_OPEN,
                        2,
                        FarFieldProfileRegistry.Orientation.HORIZONTAL,
                        FarFieldProfileRegistry.WindBucket.CALM,
                        FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                        FarFieldProfileRegistry.TopologyClass.HALF_OPEN_SPACE),
                FarFieldReferenceHarness.Split.HOLDOUT,
                new double[]{20_000.0D},
                new double[]{4_000.0D, 8_000.0D});
        FarFieldReferenceHarness.Tolerances limits =
                new FarFieldReferenceHarness.Tolerances(1.0D, 1.0D, 1.0D, 1.0D);

        assertThrows(
                IllegalArgumentException.class,
                () -> FarFieldReferenceHarness.calibrate(
                        List.of(fit, duplicate), FIT_RANGE, limits));
        assertThrows(
                IllegalArgumentException.class,
                () -> FarFieldReferenceHarness.calibrate(
                        List.of(fit, foreign), FIT_RANGE, limits));
        assertThrows(
                IllegalArgumentException.class,
                () -> FarFieldReferenceHarness.calibrate(
                        List.of(fit), FIT_RANGE, limits));
    }

    private static List<FarFieldReferenceHarness.Fixture> fixtures(
            FarFieldProfileRegistry.TopologyClass topology,
            FarFieldProfileRegistry.WindBucket wind
    ) {
        return FarFieldReferenceHarness.standardFixtures().stream()
                .filter(fixture -> fixture.key().topologyClass() == topology)
                .filter(fixture -> fixture.key().windBucket() == wind)
                .toList();
    }

    private static FarFieldReferenceHarness.Fixture fixture(
            String id,
            FarFieldProfileRegistry.Key key,
            FarFieldReferenceHarness.Split split,
            double[] capacities,
            double[] conductances
    ) {
        return new FarFieldReferenceHarness.Fixture(
                id,
                key,
                split,
                76_800.0D,
                capacities,
                conductances,
                -20.0D,
                -20.0D,
                10_000.0D,
                600.0D,
                10.0D,
                -10.0D,
                -12.0D,
                25.0D);
    }

    private static FarFieldProfileRegistry.Key key() {
        return new FarFieldProfileRegistry.Key(
                0,
                FarFieldProfileRegistry.OpeningClass.MULTI_FACE,
                2,
                FarFieldProfileRegistry.Orientation.HORIZONTAL,
                FarFieldProfileRegistry.WindBucket.CALM,
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                FarFieldProfileRegistry.TopologyClass.OPEN_SPACE);
    }
}
