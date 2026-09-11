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
    void dimensionsCompeteForTheSameServerGlobalCap() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(100L);
        ThermalMemoryBudget first = server.createDimensionBudget(100L);
        ThermalMemoryBudget second = server.createDimensionBudget(100L);

        ThermalMemoryBudget.Reservation firstUse = first.tryReserve(70L);
        assertNotNull(firstUse);
        assertNull(second.tryReserve(31L));
        ThermalMemoryBudget.Reservation secondUse = second.tryReserve(30L);
        assertNotNull(secondUse);
        firstUse.close();
        secondUse.close();
    }

    @Test
    void replacementMustBeAdmittedWhileOldBackingIsStillCharged() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(80L);
        ThermalMemoryBudget dimension = server.createDimensionBudget(80L);
        ThermalMemoryBudget.Reservation oldBacking = dimension.tryReserve(40L);
        assertNotNull(oldBacking);

        assertNull(dimension.tryReserve(50L));
        oldBacking.close();
        ThermalMemoryBudget.Reservation replacement = dimension.tryReserve(50L);
        assertNotNull(replacement);
        replacement.close();
    }

    @Test
    void reservationReleaseIsIdempotent() {
        ThermalMemoryBudget budget = new ThermalMemoryBudget(32L);
        ThermalMemoryBudget.Reservation reservation = budget.tryReserve(8L);
        assertNotNull(reservation);
        reservation.close();
        reservation.close();
        assertNull(budget.tryReserve(40L));
        ThermalMemoryBudget.Reservation fullBudget = budget.tryReserve(32L);
        assertNotNull(fullBudget);
        fullBudget.close();
    }
}
