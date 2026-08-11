/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.town.buildings.house;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseBuildingDailySettlementTest {
    @Test
    void coldHouseStillRunsResidentSettlement() {
        assertFalse(HouseDailyModel.isBuildingWorkable(true, 4, 8, false, 4, 8));
        assertTrue(HouseDailyModel.shouldRunDailySettlement(true, 4, 8, 4, 8));
    }

    @Test
    void structurallyInvalidHouseDoesNotRunSettlement() {
        assertFalse(HouseDailyModel.isBuildingWorkable(false, 4, 8, false, 4, 8));
        assertFalse(HouseDailyModel.shouldRunDailySettlement(false, 4, 8, 4, 8));
    }
}
