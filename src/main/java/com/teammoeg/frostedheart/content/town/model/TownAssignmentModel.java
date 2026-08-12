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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Pure sticky-workplace fill algorithm. Existing assignments are supplied as
 * counts and are never reconsidered; only currently unassigned residents fill
 * vacancies. Encounter order is the deterministic tie breaker.
 */
public final class TownAssignmentModel {
    private TownAssignmentModel() {
    }

    public static <R, W> List<Assignment<R, W>> fillVacancies(
            List<R> availableResidents,
            List<W> availableWorkplaces,
            ToIntFunction<W> capacity,
            ToIntFunction<W> existingWorkers,
            ToDoubleBiFunction<W, Integer> dynamicPriority,
            BiPredicate<W, R> canWork,
            ToDoubleBiFunction<W, R> residentScore
    ) {
        List<R> residents = new ArrayList<>(availableResidents);
        List<W> workplaces = List.copyOf(availableWorkplaces);
        Map<W, Integer> counts = new IdentityHashMap<>();
        workplaces.forEach(workplace -> counts.put(
                workplace, Math.max(0, existingWorkers.applyAsInt(workplace))));
        List<Assignment<R, W>> result = new ArrayList<>();

        while (!residents.isEmpty()) {
            W selectedWorkplace = null;
            double selectedPriority = Double.NEGATIVE_INFINITY;
            for (W workplace : workplaces) {
                int count = counts.get(workplace);
                if (count >= Math.max(0, capacity.applyAsInt(workplace))) continue;
                double priority = dynamicPriority.applyAsDouble(workplace, count);
                if (priority > selectedPriority) {
                    selectedPriority = priority;
                    selectedWorkplace = workplace;
                }
            }
            if (selectedWorkplace == null || selectedPriority == Double.NEGATIVE_INFINITY) break;

            int bestIndex = -1;
            double bestScore = 0.0;
            for (int index = 0; index < residents.size(); index++) {
                R resident = residents.get(index);
                if (!canWork.test(selectedWorkplace, resident)) continue;
                double score = residentScore.applyAsDouble(selectedWorkplace, resident);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = index;
                }
            }
            if (bestIndex < 0) {
                counts.put(selectedWorkplace, Math.max(0, capacity.applyAsInt(selectedWorkplace)));
                continue;
            }
            R resident = residents.remove(bestIndex);
            result.add(new Assignment<>(resident, selectedWorkplace, bestScore, selectedPriority));
            counts.put(selectedWorkplace, counts.get(selectedWorkplace) + 1);
        }
        return List.copyOf(result);
    }

    public record Assignment<R, W>(
            R resident,
            W workplace,
            double residentScore,
            double workplacePriority
    ) {
    }
}
