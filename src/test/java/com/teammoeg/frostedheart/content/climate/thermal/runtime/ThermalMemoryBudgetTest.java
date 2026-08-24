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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(80L, dimension.usedBytes());
        assertEquals(80L, server.usedBytes());

        critical.close();
        optional.close();
        assertEquals(0L, dimension.usedBytes());
        assertEquals(0L, server.usedBytes());
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
        assertEquals(90L, server.optionalUsedBytes());

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
        assertFalse(reservation.released());

        reservation.close();
        reservation.close();

        assertTrue(reservation.released());
        assertEquals(0L, budget.usedBytes());
    }
}
