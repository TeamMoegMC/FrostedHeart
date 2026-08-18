/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Pure two-pass calorie allocation: ordered guarantees, then town-wide equality. */
public final class TownFoodAllocationModel {
    private TownFoodAllocationModel() {
    }

    public static <R, H> Plan<R> plan(
            List<Household<R, H>> households,
            double availableFood,
            double foodPerResident,
            Function<List<R>, List<R>> careOrder
    ) {
        Objects.requireNonNull(households, "households");
        Objects.requireNonNull(careOrder, "careOrder");
        double remaining = nonNegative(availableFood);
        double ration = nonNegative(foodPerResident);
        Map<R, Double> allocations = new LinkedHashMap<>();
        Set<R> guaranteed = new LinkedHashSet<>();
        List<R> allResidents = new ArrayList<>();

        for (Household<R, H> household : households) {
            List<R> ordered = List.copyOf(careOrder.apply(household.residents()));
            allResidents.addAll(ordered);
            int target = Math.min(Math.max(0, household.guaranteedResidents()), ordered.size());
            for (int index = 0; index < target; index++) {
                R resident = ordered.get(index);
                guaranteed.add(resident);
                double supplied = Math.min(ration, remaining);
                allocations.put(resident, supplied);
                remaining -= supplied;
            }
        }

        List<R> unguaranteed = allResidents.stream()
                .filter(resident -> !guaranteed.contains(resident)).toList();
        if (!unguaranteed.isEmpty() && remaining > 0.0 && ration > 0.0) {
            double equal = Math.min(ration, remaining / unguaranteed.size());
            for (R resident : unguaranteed) allocations.put(resident, equal);
            remaining -= equal * unguaranteed.size();
        }
        for (R resident : allResidents) allocations.putIfAbsent(resident, 0.0);

        return new Plan<>(allocations, guaranteed,
                nonNegative(availableFood) - Math.max(0.0, remaining));
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    public record Household<R, H>(
            H house,
            List<R> residents,
            int guaranteedResidents
    ) {
        public Household {
            residents = residents == null ? List.of() : List.copyOf(residents);
            guaranteedResidents = Math.max(0, guaranteedResidents);
        }
    }

    public record Plan<R>(
            Map<R, Double> allocations,
            Set<R> guaranteedResidents,
            double allocatedFood
    ) {
        public Plan {
            allocations = Collections.unmodifiableMap(new LinkedHashMap<>(allocations));
            guaranteedResidents = Collections.unmodifiableSet(
                    new LinkedHashSet<>(guaranteedResidents));
        }
    }
}
