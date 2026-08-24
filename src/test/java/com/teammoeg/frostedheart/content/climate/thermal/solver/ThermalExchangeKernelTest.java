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

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalExchangeKernelTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void isolatedPairMatchesAnalyticSolutionAndConservesEnthalpy() {
        double enthalpyA = 0.0D;
        double capacityA = 1_000.0D;
        double enthalpyB = 200_000.0D;
        double capacityB = 2_000.0D;
        double conductance = 10.0D;
        double dtSeconds = 5.0D;

        ThermalExchangeKernel.PairResult result = ThermalExchangeKernel.exchangePair(
                enthalpyA,
                capacityA,
                enthalpyB,
                capacityB,
                conductance,
                dtSeconds
        );
        double reducedCapacity = capacityA * capacityB / (capacityA + capacityB);
        double expectedFromAToB = reducedCapacity
                * -Math.expm1(-conductance * dtSeconds
                * (1.0D / capacityA + 1.0D / capacityB))
                * (enthalpyA / capacityA - enthalpyB / capacityB);

        assertEquals(ThermalExchangeKernel.Status.APPLIED, result.status());
        assertEquals(expectedFromAToB, result.energyFromAToBJ(), EPSILON);
        assertEquals(enthalpyA + enthalpyB,
                result.enthalpyAJ() + result.enthalpyBJ(), EPSILON);
    }

    @Test
    void fixedBoundaryMatchesAnalyticTrajectory() {
        double referenceTemperatureC = -10.0D;
        double capacity = 1_000.0D;
        double initialTemperatureC = 0.0D;
        double enthalpy = capacity * (initialTemperatureC - referenceTemperatureC);

        ThermalExchangeKernel.BoundaryResult result =
                ThermalExchangeKernel.exchangeFixedBoundary(
                        enthalpy,
                        capacity,
                        referenceTemperatureC,
                        100.0D,
                        20.0D,
                        5.0D
                );
        double expectedTemperature = 100.0D
                + (initialTemperatureC - 100.0D) * Math.exp(-0.1D);

        assertEquals(ThermalExchangeKernel.Status.APPLIED, result.status());
        assertEquals(
                expectedTemperature,
                referenceTemperatureC + result.enthalpyJ() / capacity,
                EPSILON
        );
        assertEquals(enthalpy + result.energyFromBoundaryJ(), result.enthalpyJ(), EPSILON);
    }

    @Test
    void randomizedPairsRemainConservativeAndSymmetric() {
        SplittableRandom random = new SplittableRandom(0x5EEDC0DEL);
        for (int iteration = 0; iteration < 10_000; iteration++) {
            double capacityA = Math.pow(10.0D, random.nextDouble(0.0D, 8.0D));
            double capacityB = Math.pow(10.0D, random.nextDouble(0.0D, 8.0D));
            double temperatureA = random.nextDouble(-300.0D, 800.0D);
            double temperatureB = random.nextDouble(-300.0D, 800.0D);
            double enthalpyA = capacityA * temperatureA;
            double enthalpyB = capacityB * temperatureB;
            double conductance = Math.pow(10.0D, random.nextDouble(-4.0D, 6.0D));
            double dtSeconds = random.nextDouble(0.0D, 10.0D);

            ThermalExchangeKernel.PairResult forward = ThermalExchangeKernel.exchangePair(
                    enthalpyA, capacityA, enthalpyB, capacityB, conductance, dtSeconds);
            ThermalExchangeKernel.PairResult swapped = ThermalExchangeKernel.exchangePair(
                    enthalpyB, capacityB, enthalpyA, capacityA, conductance, dtSeconds);

            assertEquals(ThermalExchangeKernel.Status.APPLIED, forward.status());
            assertEquals(ThermalExchangeKernel.Status.APPLIED, swapped.status());
            double scale = Math.max(1.0D, Math.abs(enthalpyA) + Math.abs(enthalpyB));
            assertEquals(enthalpyA + enthalpyB,
                    forward.enthalpyAJ() + forward.enthalpyBJ(), scale * 2.0e-15D);
            assertEquals(forward.enthalpyAJ(), swapped.enthalpyBJ(), scale * 2.0e-15D);
            assertEquals(forward.enthalpyBJ(), swapped.enthalpyAJ(), scale * 2.0e-15D);
            assertEquals(forward.energyFromAToBJ(),
                    -swapped.energyFromAToBJ(), scale * 2.0e-15D);
        }
    }

    @Test
    void differentEpochsNeverExchange() {
        SolveEpoch epochA = epoch(0L, 5L, 1L);
        SolveEpoch epochB = epoch(0L, 5L, 2L);

        ThermalExchangeKernel.PairResult result = ThermalExchangeKernel.exchangePair(
                10_000.0D,
                1_000.0D,
                epochA,
                -5_000.0D,
                500.0D,
                epochB,
                100.0D,
                0.25D
        );

        assertEquals(ThermalExchangeKernel.Status.EPOCH_MISMATCH, result.status());
        assertFalse(result.applied());
        assertEquals(10_000.0D, result.enthalpyAJ());
        assertEquals(-5_000.0D, result.enthalpyBJ());
        assertEquals(0.0D, result.energyFromAToBJ());
    }

    @Test
    void extremeExponentSaturatesWithoutOvershootOrNan() {
        ThermalExchangeKernel.PairResult pair = ThermalExchangeKernel.exchangePair(
                0.0D,
                Double.MIN_NORMAL,
                1.0D,
                1.0D,
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );
        ThermalExchangeKernel.BoundaryResult boundary =
                ThermalExchangeKernel.exchangeFixedBoundary(
                        0.0D,
                        1.0D,
                        0.0D,
                        100.0D,
                        Double.MAX_VALUE,
                        Double.MAX_VALUE
                );

        assertEquals(ThermalExchangeKernel.Status.APPLIED, pair.status());
        assertTrue(Double.isFinite(pair.enthalpyAJ()));
        assertTrue(Double.isFinite(pair.enthalpyBJ()));
        assertEquals(1.0D, pair.enthalpyAJ() + pair.enthalpyBJ(), EPSILON);
        assertEquals(ThermalExchangeKernel.Status.APPLIED, boundary.status());
        assertEquals(100.0D, boundary.enthalpyJ(), EPSILON);
    }

    @Test
    void invalidNumericDomainsAreObservableNoOps() {
        assertDegradedPair(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        assertDegradedPair(0.0D, 1.0D, 0.0D, -1.0D, 1.0D, 1.0D);
        assertDegradedPair(0.0D, 1.0D, 0.0D, 1.0D, -1.0D, 1.0D);
        assertDegradedPair(Double.NaN, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        assertDegradedPair(0.0D, 1.0D, 0.0D, 1.0D, 1.0D, Double.NaN);

        ThermalExchangeKernel.BoundaryResult boundary =
                ThermalExchangeKernel.exchangeFixedBoundary(
                        12.0D, 0.0D, 0.0D, 10.0D, 1.0D, 1.0D);
        assertEquals(ThermalExchangeKernel.Status.NUMERIC_DEGRADED, boundary.status());
        assertEquals(12.0D, boundary.enthalpyJ());
        assertEquals(0.0D, boundary.energyFromBoundaryJ());
    }

    private static void assertDegradedPair(
            double enthalpyA,
            double capacityA,
            double enthalpyB,
            double capacityB,
            double conductance,
            double dtSeconds
    ) {
        ThermalExchangeKernel.PairResult result = ThermalExchangeKernel.exchangePair(
                enthalpyA,
                capacityA,
                enthalpyB,
                capacityB,
                conductance,
                dtSeconds
        );
        assertEquals(ThermalExchangeKernel.Status.NUMERIC_DEGRADED, result.status());
        assertFalse(result.applied());
        assertEquals(0.0D, result.energyFromAToBJ());
    }

    private static SolveEpoch epoch(long previousTick, long targetTick, long id) {
        return new SolveEpoch(previousTick, targetTick, id, 3L, InputWatermarks.ZERO);
    }
}
