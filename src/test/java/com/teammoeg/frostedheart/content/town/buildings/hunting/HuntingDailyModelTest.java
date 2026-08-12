/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuntingDailyModelTest {
    @Test
    void standardHunterSettlesOneRollAndCarriesOneSixth() {
        HuntingDailyModel.RollPlan plan = HuntingDailyModel.planRolls(
                1.0, 7.0 / 6.0, 0.0, 0.0, true, 100.0);

        assertEquals(7.0 / 6.0, plan.workerExpectedRolls(), 1.0e-12);
        assertEquals(1, plan.plannedRolls());
        assertEquals(1, plan.executedRolls());
        assertEquals(1.0 / 6.0, plan.nextCarry(), 1.0e-12);
        assertTrue(plan.hasWorkerOpportunity());
    }

    @Test
    void terrainCapsRollsWithoutBackloggingWholeRolls() {
        HuntingDailyModel.RollPlan plan = HuntingDailyModel.planRolls(
                2.0, 7.0 / 6.0, 0.0, 0.5, true, 1.9);

        assertEquals(2, plan.plannedRolls());
        assertEquals(1, plan.executedRolls());
        assertEquals(5.0 / 6.0, plan.nextCarry(), 1.0e-12);
    }
}
