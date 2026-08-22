/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportTransferBudgetTest {
    @Test
    void integerRatesConvergeWithoutLowRateStarvation() {
        for (int rate : new int[]{1, 7, 19, 20, 64, 128, 1280}) {
            TransportTransferBudget budget = new TransportTransferBudget();
            int moved = 0;
            for (int tick = 0; tick < 2000; tick++) {
                moved += budget.beginTick(rate, true);
            }
            assertEquals(rate * 100, moved, "rate=" + rate);
            assertTrue(budget.getTransferRemainder() >= 0.0);
            assertTrue(budget.getTransferRemainder() < 1.0);
        }
    }

    @Test
    void fractionalShortageRateKeepsLessThanOneItemOfDiscretizationError() {
        TransportTransferBudget budget = new TransportTransferBudget();
        double rate = 17.25;
        int moved = 0;
        int ticks = 20_000;
        for (int tick = 0; tick < ticks; tick++) {
            moved += budget.beginTick(rate, true);
        }
        double exact = rate * ticks / TransportTransferBudget.SERVER_TICKS_PER_SECOND;
        assertTrue(exact - moved >= 0.0 && exact - moved < 1.0);
    }

    @Test
    void idleTicksAndUnusedWholeBudgetsDoNotCreateBursts() {
        TransportTransferBudget budget = new TransportTransferBudget();
        for (int tick = 0; tick < 1000; tick++) {
            assertEquals(0, budget.beginTick(1280.0, false));
        }
        assertEquals(64, budget.beginTick(1280.0, true));
        assertEquals(64, budget.beginTick(1280.0, true));

        budget.reset();
        assertEquals(0.0, budget.getTransferRemainder());
        assertEquals(0, budget.beginTick(1.0, true));
    }
}
