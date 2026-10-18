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
 *
 */

package com.teammoeg.chorda.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CGuiHelperItemBatchPlannerTest {
    @Test
    void preservesContiguousLightingSegmentsAndResetsForTheNextBatch() {
        CGuiHelper.ItemBatchPlanner planner = new CGuiHelper.ItemBatchPlanner();

        assertEquals(CGuiHelper.ItemBatchPlanner.Transition.START, planner.accept(true));
        assertEquals(CGuiHelper.ItemBatchPlanner.Transition.CONTINUE, planner.accept(true));
        assertEquals(CGuiHelper.ItemBatchPlanner.Transition.SWITCH, planner.accept(false));
        assertEquals(CGuiHelper.ItemBatchPlanner.Transition.CONTINUE, planner.accept(false));
        assertEquals(CGuiHelper.ItemBatchPlanner.Transition.SWITCH, planner.accept(true));

        planner.reset();
        assertEquals(CGuiHelper.ItemBatchPlanner.Transition.START, planner.accept(false));
    }
}
