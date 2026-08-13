/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.observation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownObservationModelTest {
    private static final TownObservationModel.ResidentRules RULES =
            new TownObservationModel.ResidentRules(10.0, 5.0, 5.0,
                    2, 10.0, 5.0, true);

    @Test
    void lowTailAndMeansRemainDistinct() {
        TownObservationModel.ResidentSnapshot snapshot =
                TownObservationModel.observeResidents(List.of(
                        resident("weak", 5.0, 5.0, true),
                        resident("a", 100.0, 100.0, true),
                        resident("b", 100.0, 100.0, true),
                        resident("c", 100.0, 100.0, true),
                        resident("d", 100.0, 100.0, true)), RULES);

        assertEquals(81.0, snapshot.averageHealth(), 1.0e-9);
        assertEquals(43.0, snapshot.p10Health(), 1.0e-9);
        assertEquals(5.0, snapshot.minimumHealth(), 1.0e-9);
        assertEquals(List.of("weak"), snapshot.exitRiskResidentIds());
        assertEquals(List.of("weak"), snapshot.unableToWorkResidentIds());
    }

    @Test
    void exitRiskIncludesNextMorningHomelessPenalty() {
        TownObservationModel.ResidentSnapshot snapshot =
                TownObservationModel.observeResidents(List.of(
                        resident("homeless", 14.0, 50.0, false),
                        resident("housed", 14.0, 50.0, true)), RULES);

        assertEquals(List.of("homeless"), snapshot.exitRiskResidentIds());
        assertEquals(List.of("homeless"), snapshot.unableToWorkResidentIds());
    }

    @Test
    void reserveTrendAndPointProcessMetricsHaveExplicitUnits() {
        TownObservationModel.ReserveSignal reserve =
                TownObservationModel.observeReserve(6.0, 8.0);
        assertEquals(-2.0, reserve.dailyTrend(), 1.0e-9);
        assertEquals(3.0, reserve.timeToEmptyDays(), 1.0e-9);
        assertTrue(TownObservationModel.observeReserve(8.0, 6.0)
                .timeToEmptyDays() == Double.POSITIVE_INFINITY);

        assertEquals(3.0, TownObservationModel.fanoFactor(new int[]{0, 0, 3, 0}), 1.0e-9);
        assertEquals(Math.sqrt(2.0) / 3.0,
                TownObservationModel.intervalCoefficientOfVariation(List.of(1, 3, 7)), 1.0e-9);
    }

    private static TownObservationModel.ResidentStatus resident(
            String id,
            double health,
            double mental,
            boolean housed
    ) {
        return new TownObservationModel.ResidentStatus(id, 2, health, mental, housed);
    }
}
