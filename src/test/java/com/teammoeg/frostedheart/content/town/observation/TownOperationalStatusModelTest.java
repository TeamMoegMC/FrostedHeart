/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TownOperationalStatusModelTest {
    @AfterEach
    void clearClientCache() {
        TownOperationalStatusClientCache.reset();
    }

    @Test
    void foodReserveUsesStoredResourceUnitsAndPopulationDemand() {
        TownOperationalStatus.Metric reserve = TownOperationalStatusModel.foodReserveDays(
                650.0, 10, 6.5);
        assertTrue(reserve.available());
        assertEquals(10.0, reserve.value(), 1.0e-12);
        assertFalse(TownOperationalStatusModel.foodReserveDays(650.0, 0, 6.5).available());
    }

    @Test
    void fuelCountsOnlyWholeRecipeApplicationsAndLoadedBalance() {
        long total = TownOperationalStatusModel.totalProcessTicks(100, List.of(
                new TownOperationalStatusModel.FuelStock(5.9, 2, 700),
                new TownOperationalStatusModel.FuelStock(3.0, 1, 1400)));
        assertEquals(5700L, total);
        assertEquals(2L, TownOperationalStatusModel.wholeRecipeApplications(5.9, 2));
    }

    @Test
    void normalAndOverdriveUseCurrentPerTickDemand() {
        TownOperationalStatus.Metric normal = TownOperationalStatusModel.t1FuelReserveDays(
                240000, 1, 1, false, 24000);
        TownOperationalStatus.Metric overdrive = TownOperationalStatusModel.t1FuelReserveDays(
                240000, 1, 1, true, 24000);
        assertEquals(10.0, normal.value(), 1.0e-12);
        assertEquals(5.0, overdrive.value(), 1.0e-12);
    }

    @Test
    void reserveCrossingChoosesMostSevereAndRequiresFullRecovery() {
        assertEquals(TownOperationalStatusModel.ReserveTransition.CRITICAL,
                transition(8.0, 2.0));
        assertEquals(TownOperationalStatusModel.ReserveTransition.WARNING,
                transition(8.0, 6.0));
        assertEquals(TownOperationalStatusModel.ReserveTransition.NONE,
                transition(2.0, 6.0));
        assertEquals(TownOperationalStatusModel.ReserveTransition.RECOVERED,
                transition(2.0, 7.0));
    }

    @Test
    void staleClientResponseIsDiscarded() {
        assertTrue(TownOperationalStatusClientCache.accept(TownOperationalStatus.empty(100)));
        assertFalse(TownOperationalStatusClientCache.accept(TownOperationalStatus.empty(99)));
        assertEquals(100L, TownOperationalStatusClientCache.get().serverGameTime());
    }

    private static TownOperationalStatusModel.ReserveTransition transition(double before, double after) {
        return TownOperationalStatusModel.reserveTransition(
                TownOperationalStatus.Metric.available(before),
                TownOperationalStatus.Metric.available(after), 7.0, 3.0);
    }
}
