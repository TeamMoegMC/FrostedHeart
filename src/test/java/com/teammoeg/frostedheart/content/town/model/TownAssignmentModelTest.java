/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.model;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownAssignmentModelTest {
    private static final Comparator<String> RESIDENT_ORDER = Comparator.naturalOrder();

    @Test
    void targetPassStrictlyFillsQueueBeforeLaterWorkplace() {
        TownAssignmentModel.Plan<String, String> plan = plan(
                List.of("a", "b", "c"),
                List.of(workplace("mine", 5, 2), workplace("hunt", 5, 2)),
                ignored -> null, (workplace, resident) -> 1.0);

        assertEquals(List.of("mine", "mine", "hunt"), plan.assignments().stream()
                .map(TownAssignmentModel.Assignment::workplace).toList());
        assertEquals(0, plan.workplaces().get("mine").targetShortfall());
        assertEquals(1, plan.workplaces().get("hunt").targetShortfall());
    }

    @Test
    void unavailableCandidateDoesNotBlockLaterWorkplace() {
        TownAssignmentModel.Plan<String, String> plan = TownAssignmentModel.plan(
                List.of("hunter"),
                List.of(workplace("mine", 2, 2), workplace("hunt", 2, 1)),
                ignored -> null,
                (workplace, resident) -> !"mine".equals(workplace),
                (workplace, resident) -> 1.0,
                RESIDENT_ORDER);

        assertEquals("hunt", plan.assignments().get(0).workplace());
        assertEquals(2, plan.workplaces().get("mine").targetShortfall());
    }

    @Test
    void surplusUsesLowestCapacityFillAndQueueForExactTies() {
        TownAssignmentModel.Plan<String, String> plan = plan(
                List.of("a", "b", "c", "d"),
                List.of(workplace("small", 2, 0), workplace("large", 4, 0)),
                ignored -> null, (workplace, resident) -> 1.0);

        assertEquals(List.of("small", "large", "large", "small"),
                plan.assignments().stream()
                        .map(TownAssignmentModel.Assignment::workplace).toList());
        assertTrue(plan.assignments().stream()
                .allMatch(value -> value.phase() == TownAssignmentModel.Phase.SURPLUS));
    }

    @Test
    void residentScoreThenPreviousWorkplaceThenStableIdBreakTies() {
        Map<String, String> previous = Map.of(
                "alpha", "hunt", "beta", "mine", "gamma", "mine");
        TownAssignmentModel.Plan<String, String> plan = plan(
                List.of("gamma", "alpha", "beta"),
                List.of(workplace("mine", 2, 2), workplace("hunt", 1, 1)),
                previous::get,
                (workplace, resident) -> "alpha".equals(resident) ? 2.0 : 1.0);

        // Score outranks continuity, then beta wins the equal-score continuity
        // tie by stable resident id.
        assertEquals(List.of("alpha", "beta"), plan.assignments().stream()
                .filter(value -> "mine".equals(value.workplace()))
                .map(TownAssignmentModel.Assignment::resident).toList());
        assertEquals("gamma", plan.assignments().stream()
                .filter(value -> "hunt".equals(value.workplace()))
                .findFirst().orElseThrow().resident());
    }

    @Test
    void unworkableAndOverTargetInputsAreClampedWithoutDuplicateResidents() {
        TownAssignmentModel.Plan<String, String> plan = plan(
                List.of("a", "b"),
                List.of(
                        new TownAssignmentModel.Workplace<>("cold", 10, 10, false),
                        workplace("open", 1, 9)),
                ignored -> null, (workplace, resident) -> 1.0);

        assertEquals(0, plan.workplaces().get("cold").capacity());
        assertEquals(1, plan.workplaces().get("open").effectiveTarget());
        assertEquals(1, plan.assignments().size());
        assertEquals(List.of("b"), plan.unassignedResidents());
    }

    private static TownAssignmentModel.Workplace<String> workplace(
            String id, int capacity, int target
    ) {
        return new TownAssignmentModel.Workplace<>(id, capacity, target, true);
    }

    private static TownAssignmentModel.Plan<String, String> plan(
            List<String> residents,
            List<TownAssignmentModel.Workplace<String>> workplaces,
            java.util.function.Function<String, String> previous,
            java.util.function.ToDoubleBiFunction<String, String> score
    ) {
        return TownAssignmentModel.plan(
                residents, workplaces, previous,
                (workplace, resident) -> true, score, RESIDENT_ORDER);
    }
}
