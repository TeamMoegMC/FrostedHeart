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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;

/**
 * 城镇每日岗位调度的纯 Java 实现。
 * <p>
 * The shared, Forge-independent daily town staffing planner. Workplaces are
 * supplied in player-visible queue order. The first pass satisfies configured
 * targets strictly in that order; a second pass assigns surplus labour to the
 * least-filled workplace by {@code assigned / capacity}.
 */
public final class TownAssignmentModel {
    private TownAssignmentModel() {
    }

    public static <R, W> Plan<R, W> plan(
            List<R> residents,
            List<Workplace<W>> queuedWorkplaces,
            Function<R, W> previousWorkplace,
            BiPredicate<W, R> canWork,
            ToDoubleBiFunction<W, R> residentScore,
            Comparator<R> stableResidentOrder
    ) {
        Objects.requireNonNull(residents, "residents");
        Objects.requireNonNull(queuedWorkplaces, "queuedWorkplaces");
        Objects.requireNonNull(previousWorkplace, "previousWorkplace");
        Objects.requireNonNull(canWork, "canWork");
        Objects.requireNonNull(residentScore, "residentScore");
        Objects.requireNonNull(stableResidentOrder, "stableResidentOrder");

        List<R> remaining = new ArrayList<>(residents);
        remaining.sort(stableResidentOrder);
        List<Workplace<W>> workplaces = List.copyOf(queuedWorkplaces);
        Map<W, MutableStatus> status = new LinkedHashMap<>();
        for (Workplace<W> workplace : workplaces) {
            if (status.putIfAbsent(workplace.value(), new MutableStatus(workplace)) != null) {
                throw new IllegalArgumentException("Duplicate workplace in staffing queue: "
                        + workplace.value());
            }
        }
        List<Assignment<R, W>> assignments = new ArrayList<>();

        // Guaranteed-target pass: a later workplace cannot consume a resident
        // while an earlier workplace still has a target vacancy that resident
        // can fill.
        for (Workplace<W> workplace : workplaces) {
            MutableStatus workplaceStatus = status.get(workplace.value());
            while (workplaceStatus.assigned < workplaceStatus.effectiveTarget) {
                int residentIndex = bestResidentIndex(
                        remaining, workplace.value(), previousWorkplace,
                        canWork, residentScore, stableResidentOrder);
                if (residentIndex < 0) break;
                assign(remaining, residentIndex, workplace.value(), Phase.TARGET,
                        residentScore, assignments, workplaceStatus);
            }
        }

        // Surplus pass: discrete max-min fairness by current capacity fill.
        while (!remaining.isEmpty()) {
            Workplace<W> selected = null;
            int selectedResidentIndex = -1;
            double selectedFill = Double.POSITIVE_INFINITY;
            for (Workplace<W> workplace : workplaces) {
                MutableStatus workplaceStatus = status.get(workplace.value());
                if (workplaceStatus.assigned >= workplaceStatus.capacity) continue;
                int candidate = bestResidentIndex(
                        remaining, workplace.value(), previousWorkplace,
                        canWork, residentScore, stableResidentOrder);
                if (candidate < 0) continue;
                double fill = (double) workplaceStatus.assigned / workplaceStatus.capacity;
                // Strictly-less preserves queue order as the exact-tie breaker.
                if (fill < selectedFill) {
                    selected = workplace;
                    selectedResidentIndex = candidate;
                    selectedFill = fill;
                }
            }
            if (selected == null) break;
            assign(remaining, selectedResidentIndex, selected.value(), Phase.SURPLUS,
                    residentScore, assignments, status.get(selected.value()));
        }

        Map<W, WorkplaceStatus> immutableStatus = new LinkedHashMap<>();
        status.forEach((workplace, value) -> immutableStatus.put(
                workplace, value.toImmutable(workplace)));
        return new Plan<>(assignments, remaining, immutableStatus);
    }

    private static <R, W> int bestResidentIndex(
            List<R> residents,
            W workplace,
            Function<R, W> previousWorkplace,
            BiPredicate<W, R> canWork,
            ToDoubleBiFunction<W, R> residentScore,
            Comparator<R> stableResidentOrder
    ) {
        int bestIndex = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        boolean bestWasPrevious = false;
        for (int index = 0; index < residents.size(); index++) {
            R resident = residents.get(index);
            if (!canWork.test(workplace, resident)) continue;
            double score = residentScore.applyAsDouble(workplace, resident);
            if (!Double.isFinite(score)) continue;
            boolean wasPrevious = Objects.equals(previousWorkplace.apply(resident), workplace);
            if (bestIndex < 0
                    || score > bestScore
                    || Double.compare(score, bestScore) == 0 && wasPrevious && !bestWasPrevious
                    || Double.compare(score, bestScore) == 0 && wasPrevious == bestWasPrevious
                    && stableResidentOrder.compare(resident, residents.get(bestIndex)) < 0) {
                bestIndex = index;
                bestScore = score;
                bestWasPrevious = wasPrevious;
            }
        }
        return bestIndex;
    }

    private static <R, W> void assign(
            List<R> remaining,
            int residentIndex,
            W workplace,
            Phase phase,
            ToDoubleBiFunction<W, R> residentScore,
            List<Assignment<R, W>> assignments,
            MutableStatus status
    ) {
        R resident = remaining.remove(residentIndex);
        double score = residentScore.applyAsDouble(workplace, resident);
        assignments.add(new Assignment<>(resident, workplace, score, phase));
        status.assigned++;
    }

    /** One queued workplace and its current physical/player inputs. */
    public record Workplace<W>(W value, int capacity, int target, boolean workable) {
        public Workplace {
            Objects.requireNonNull(value, "value");
            capacity = Math.max(0, capacity);
            target = Math.max(0, target);
        }
    }

    public enum Phase {
        TARGET,
        SURPLUS
    }

    public record Assignment<R, W>(
            R resident,
            W workplace,
            double residentScore,
            Phase phase
    ) {
    }

    public record WorkplaceStatus(
            int capacity,
            int configuredTarget,
            int effectiveTarget,
            int assigned,
            int targetShortfall
    ) {
    }

    public record Plan<R, W>(
            List<Assignment<R, W>> assignments,
            List<R> unassignedResidents,
            Map<W, WorkplaceStatus> workplaces
    ) {
        public Plan {
            assignments = List.copyOf(assignments);
            unassignedResidents = List.copyOf(unassignedResidents);
            workplaces = Collections.unmodifiableMap(new LinkedHashMap<>(workplaces));
        }
    }

    private static final class MutableStatus {
        private final int capacity;
        private final int configuredTarget;
        private final int effectiveTarget;
        private int assigned;

        private <W> MutableStatus(Workplace<W> workplace) {
            this.capacity = workplace.workable() ? workplace.capacity() : 0;
            this.configuredTarget = workplace.target();
            this.effectiveTarget = Math.min(configuredTarget, capacity);
        }

        private <W> WorkplaceStatus toImmutable(W ignored) {
            return new WorkplaceStatus(capacity, configuredTarget, effectiveTarget,
                    assigned, Math.max(0, effectiveTarget - assigned));
        }
    }
}
