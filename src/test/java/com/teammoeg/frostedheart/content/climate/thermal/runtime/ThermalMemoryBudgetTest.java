/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ThermalMemoryBudgetTest {
    @Test
    void optionalStorageCannotConsumeDimensionOrServerCriticalReserve() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(100L, 30L);
        ThermalMemoryBudget dimension = server.createDimensionBudget(80L, 20L);

        ThermalMemoryBudget.Reservation critical = dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, 20L);
        assertNotNull(critical);
        ThermalMemoryBudget.Reservation optional = dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 60L);
        assertNotNull(optional);
        assertNull(dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 1L));
        assertNull(dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, 1L));
        critical.close();
        optional.close();
        ThermalMemoryBudget.Reservation criticalAgain = dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, 20L);
        ThermalMemoryBudget.Reservation optionalAgain = dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 60L);
        assertNotNull(criticalAgain);
        assertNotNull(optionalAgain);
        criticalAgain.close();
        optionalAgain.close();
    }

    @Test
    void dimensionsCompeteForTheSameServerGlobalCap() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(100L, 10L);
        ThermalMemoryBudget first = server.createDimensionBudget(100L, 0L);
        ThermalMemoryBudget second = server.createDimensionBudget(100L, 0L);

        ThermalMemoryBudget.Reservation firstUse = first.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 70L);
        assertNotNull(firstUse);
        assertNull(second.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 21L));
        ThermalMemoryBudget.Reservation secondUse = second.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 20L);
        assertNotNull(secondUse);
        firstUse.close();
        secondUse.close();
    }

    @Test
    void replacementMustBeAdmittedWhileOldBackingIsStillCharged() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(80L, 0L);
        ThermalMemoryBudget dimension = server.createDimensionBudget(80L, 0L);
        ThermalMemoryBudget.Reservation oldBacking = dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 40L);
        assertNotNull(oldBacking);

        assertNull(dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 50L));
        oldBacking.close();
        ThermalMemoryBudget.Reservation replacement = dimension.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL, 50L);
        assertNotNull(replacement);
        replacement.close();
    }

    @Test
    void reservationReleaseIsIdempotent() {
        ThermalMemoryBudget budget = new ThermalMemoryBudget(32L, 8L);
        ThermalMemoryBudget.Reservation reservation = budget.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, 8L);
        assertNotNull(reservation);
        reservation.close();
        reservation.close();
        assertNull(budget.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, 40L));
        ThermalMemoryBudget.Reservation fullBudget = budget.tryReserve(
                ThermalMemoryBudget.AllocationClass.CRITICAL, 32L);
        assertNotNull(fullBudget);
        fullBudget.close();
    }
}
