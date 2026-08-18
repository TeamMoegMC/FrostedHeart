/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownFoodAllocationModelTest {
    @Test
    void guaranteesFollowHousingOrderBeforeTownWideEqualShare() {
        var households = List.of(
                new TownFoodAllocationModel.Household<>("high", List.of("a", "b"), 1),
                new TownFoodAllocationModel.Household<>("low", List.of("c", "d"), 1));

        TownFoodAllocationModel.Plan<String> plan = TownFoodAllocationModel.plan(
                households, 3.0, 1.0, List::copyOf);

        assertEquals(1.0, plan.allocations().get("a"), 1.0e-12);
        assertEquals(1.0, plan.allocations().get("c"), 1.0e-12);
        assertEquals(0.5, plan.allocations().get("b"), 1.0e-12);
        assertEquals(0.5, plan.allocations().get("d"), 1.0e-12);
        assertEquals(3.0, plan.allocatedFood(), 1.0e-12);
    }

    @Test
    void earlierGuaranteeCanConsumeTheLastFood() {
        var households = List.of(
                new TownFoodAllocationModel.Household<>("high", List.of("a"), 1),
                new TownFoodAllocationModel.Household<>("low", List.of("b"), 1));

        TownFoodAllocationModel.Plan<String> plan = TownFoodAllocationModel.plan(
                households, 0.75, 1.0, residents -> residents);

        assertEquals(0.75, plan.allocations().get("a"), 1.0e-12);
        assertEquals(0.0, plan.allocations().get("b"), 1.0e-12);
        assertTrue(plan.guaranteedResidents().containsAll(List.of("a", "b")));
    }
}
