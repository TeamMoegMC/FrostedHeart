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
