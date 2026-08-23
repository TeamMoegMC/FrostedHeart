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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalReferenceModelTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void ticksAndEnthalpyRoundTripUseTheFrozenUnits() {
        assertEquals(1.0, ThermalUnits.ticksToSeconds(20L), EPSILON);
        assertEquals(0.05, ThermalUnits.ticksToSeconds(1L), EPSILON);

        double enthalpyJ = ThermalUnits.enthalpyFromTemperature(25.0, -20.0, 2_000.0);
        assertEquals(90_000.0, enthalpyJ, EPSILON);
        assertEquals(
                25.0,
                ThermalUnits.temperatureFromEnthalpy(enthalpyJ, -20.0, 2_000.0),
                EPSILON
        );
    }

    @Test
    void constantAndPiecewisePowerIntegrateEventDurationsExactly() {
        ThermalReferenceModel.SourceIntegration constant =
                ThermalReferenceModel.integrateConstantPower(40.0, 125.0, 2.0);
        assertEquals(250.0, constant.integratedEnergyJ(), EPSILON);
        assertEquals(290.0, constant.enthalpyJ(), EPSILON);

        ThermalReferenceModel.SourceIntegration piecewise =
                ThermalReferenceModel.integratePiecewisePower(
                        40.0,
                        List.of(
                                new ThermalReferenceModel.PowerSegment(0.5, 100.0),
                                new ThermalReferenceModel.PowerSegment(1.5, 100.0),
                                new ThermalReferenceModel.PowerSegment(4.0, -50.0)
                        )
                );
        assertEquals(0.0, piecewise.integratedEnergyJ(), EPSILON);
        assertEquals(40.0, piecewise.enthalpyJ(), EPSILON);
    }

    @Test
    void isolatedPairMatchesTheAnalyticSolutionAndConservesEnthalpy() {
        double capacityA = 1_000.0;
        double capacityB = 2_000.0;
        double initialA = 0.0;
        double initialB = 200_000.0;
        double conductance = 10.0;
        double dtSeconds = 5.0;

        ThermalReferenceModel.PairExchange result = ThermalReferenceModel.exchangePair(
                initialA,
                capacityA,
                initialB,
                capacityB,
                conductance,
                dtSeconds
        );
        double expectedTransfer = 100.0 * (capacityA * capacityB / (capacityA + capacityB))
                * -Math.expm1(-conductance * dtSeconds * (1.0 / capacityA + 1.0 / capacityB));

        assertEquals(expectedTransfer, result.transferredToAJ(), EPSILON);
        assertEquals(initialA + initialB, result.totalEnthalpyJ(), EPSILON);
        assertTrue(result.enthalpyAJ() / capacityA < result.enthalpyBJ() / capacityB);
    }

    @Test
    void pairExchangeIsSymmetricWhenEndpointsAreSwapped() {
        ThermalReferenceModel.PairExchange forward = ThermalReferenceModel.exchangePair(
                -2_000.0, 500.0, 9_000.0, 750.0, 30.0, 0.25
        );
        ThermalReferenceModel.PairExchange reverse = ThermalReferenceModel.exchangePair(
                9_000.0, 750.0, -2_000.0, 500.0, 30.0, 0.25
        );

        assertEquals(forward.enthalpyAJ(), reverse.enthalpyBJ(), EPSILON);
        assertEquals(forward.enthalpyBJ(), reverse.enthalpyAJ(), EPSILON);
        assertEquals(forward.transferredToAJ(), -reverse.transferredToAJ(), EPSILON);
    }

    @Test
    void fixedBoundaryUsesStableExponentialExchange() {
        double initialH = ThermalUnits.enthalpyFromTemperature(0.0, -10.0, 1_000.0);
        ThermalReferenceModel.BoundaryExchange result =
                ThermalReferenceModel.exchangeFixedBoundary(
                        initialH,
                        1_000.0,
                        -10.0,
                        100.0,
                        20.0,
                        5.0
                );
        double expectedTransfer = 1_000.0 * 100.0 * -Math.expm1(-0.1);

        assertEquals(expectedTransfer, result.energyFromBoundaryJ(), EPSILON);
        assertEquals(initialH + expectedTransfer, result.enthalpyJ(), EPSILON);
    }

    @Test
    void extremeExchangeExponentConvergesWithoutNaN() {
        ThermalReferenceModel.PairExchange result = ThermalReferenceModel.exchangePair(
                0.0,
                Double.MIN_NORMAL,
                1.0,
                1.0,
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        assertTrue(Double.isFinite(result.enthalpyAJ()));
        assertTrue(Double.isFinite(result.enthalpyBJ()));
        assertEquals(1.0, result.totalEnthalpyJ(), EPSILON);
    }

    @Test
    void illegalNumericalFixturesAreRejectedExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> ThermalUnits.ticksToSeconds(-1L));
        assertThrows(
                IllegalArgumentException.class,
                () -> ThermalUnits.enthalpyFromTemperature(0.0, 0.0, 0.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ThermalReferenceModel.integrateConstantPower(0.0, Double.NaN, 1.0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ThermalReferenceModel.exchangePair(0.0, 1.0, 0.0, 1.0, -1.0, 1.0)
        );
    }
}
