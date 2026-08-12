/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownAssignmentModelTest {
    @Test
    void fillsOnlyVacanciesAndUsesEncounterOrderForExactTies() {
        List<String> residents = List.of("first", "second");
        List<String> workplaces = List.of("mine", "hunt");
        Map<String, Integer> existing = Map.of("mine", 1, "hunt", 0);

        List<TownAssignmentModel.Assignment<String, String>> assignments =
                TownAssignmentModel.fillVacancies(
                        residents,
                        workplaces,
                        ignored -> 2,
                        existing::get,
                        (ignored, count) -> -count,
                        (workplace, resident) -> true,
                        (workplace, resident) -> 1.0);

        assertEquals("hunt", assignments.get(0).workplace());
        assertEquals("first", assignments.get(0).resident());
        assertEquals("mine", assignments.get(1).workplace());
        assertEquals("second", assignments.get(1).resident());
    }
}
