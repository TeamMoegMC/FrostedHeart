/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure shared-menu planner used for one house or one simulated recipient group.
 *
 * <p>The planner preserves the existing absolute food-tier priority. Within the highest
 * available tier, it repeatedly chooses the candidate that reduces the group's aggregate
 * nutrition deficit most per food resource unit. The resulting menu composition is shared by
 * all recipients in that group; each recipient receives it scaled only by that resident's
 * allocated share. Gameplay uses {@link #planInPriorityOrder(List, int, List, Parameters)} so
 * earlier houses reserve food before later houses are considered.</p>
 */
public final class ResidentPublicMenuModel {
    private static final double EPSILON = 1.0e-9;

    private ResidentPublicMenuModel() {
    }

    /**
     * Converts one item profile to resident nutrition points using vanilla hunger only.
     *
     * <p>Each channel is {@code profilePercent * hunger}. Saturation remains part of food resource
     * allocation but is deliberately absent from resident nutrition points.</p>
     *
     * @param profile canonical item percentage profile; {@code null} is zero
     * @param hunger vanilla hunger supplied by one item
     * @return four-channel points contributed by one item
     */
    public static ResidentNutrition.NutritionIntake nutritionPoints(
            FoodNutritionProfile profile,
            int hunger
    ) {
        FoodNutritionProfile safe = profile == null ? FoodNutritionProfile.ZERO : profile;
        double h = Math.max(0, hunger);
        return new ResidentNutrition.NutritionIntake(
                safe.fat() * h,
                safe.carbohydrate() * h,
                safe.protein() * h,
                safe.vegetable() * h);
    }

    /**
     * Selects one shared menu for a recipient group.
     *
     * <p>The requested food amount is divided into {@code selectionChunks}. For each fragment,
     * candidates in lower tiers are ignored while a higher-tier candidate remains available.
     * Candidates within the active tier are scored by simulating their proportional distribution
     * to all recipients and measuring reduction of deficit relative to the healthy reserve.
     * Stable keys resolve equal scores deterministically.</p>
     *
     * @param requestedFoodUnits total food resource units requested after resident allocation
     * @param selectionChunks number of menu fragments; values below one become one
     * @param candidates available food stacks and their resource/nutrition facts
     * @param recipients resident starting states and proportional menu shares
     * @param parameters resident meal-settlement and healthy-line parameters
     * @param <K> key used by the caller to identify an inventory candidate
     * @return selected item amounts, consumed food resource units, and aggregate nutrition points
     */
    public static <K> Plan<K> plan(
            double requestedFoodUnits,
            int selectionChunks,
            List<Candidate<K>> candidates,
            List<Recipient> recipients,
            Parameters parameters
    ) {
        double remaining = nonNegative(requestedFoodUnits);
        if (remaining <= EPSILON || candidates == null || candidates.isEmpty()) {
            return Plan.empty();
        }
        Parameters safeParameters = parameters == null ? Parameters.DEFAULT : parameters;
        List<CandidateState<K>> available = new ArrayList<>();
        for (Candidate<K> candidate : candidates) {
            if (candidate != null) available.add(new CandidateState<>(candidate));
        }
        int chunks = Math.max(1, selectionChunks);
        double chunkSize = remaining / chunks;
        double consumed = 0.0;
        ResidentNutrition.NutritionIntake intake = ResidentNutrition.NutritionIntake.ZERO;
        Map<K, Double> used = new LinkedHashMap<>();
        for (int chunk = 0; chunk < chunks && remaining > EPSILON; chunk++) {
            double chunkRemaining = Math.min(remaining, chunkSize);
            while (chunkRemaining > EPSILON) {
                int highestTier = available.stream().filter(CandidateState::available)
                        .mapToInt(candidate -> candidate.source.tier()).max().orElse(-1);
                if (highestTier < 0) break;
                double request = chunkRemaining;
                ResidentNutrition.NutritionIntake before = intake;
                CandidateState<K> selected = available.stream()
                        .filter(CandidateState::available)
                        .filter(candidate -> candidate.source.tier() == highestTier)
                        .max(Comparator.comparingDouble((CandidateState<K> candidate) ->
                                        utility(candidate, request, before, recipients, safeParameters))
                                .thenComparing(candidate -> candidate.source.stableKey(),
                                        Comparator.reverseOrder()))
                        .orElse(null);
                if (selected == null) break;
                double items = Math.min(selected.items,
                        request / selected.source.foodUnitsPerItem());
                if (items <= EPSILON) {
                    selected.items = 0.0;
                    continue;
                }
                selected.items -= items;
                used.merge(selected.source.key(), items, Double::sum);
                double food = items * selected.source.foodUnitsPerItem();
                consumed += food;
                remaining -= food;
                chunkRemaining -= food;
                intake = intake.plus(selected.source.nutritionPoints().scale(items));
            }
        }
        return new Plan<>(Map.copyOf(used), consumed, intake);
    }

    /**
     * Plans independent shared menus in strict group-priority order.
     *
     * <p>Every selected item amount is removed from the planning inventory before the next
     * group is evaluated. This gives an earlier house first access to high-tier food while
     * retaining one common composition inside each house. This method only plans numeric
     * amounts; the caller remains responsible for authoritative inventory mutation and must use
     * the actually modified amounts for settlement and reporting.</p>
     *
     * @param groups ordered recipient groups; list order is the food-quality priority
     * @param selectionChunks menu fragments used independently for each group
     * @param candidates inventory visible before the first group is planned
     * @param parameters resident meal projection parameters
     * @param <G> caller's group identity, normally a house
     * @param <K> caller's inventory item identity
     * @return one plan for every input group, in the same order
     */
    public static <G, K> List<GroupPlan<G, K>> planInPriorityOrder(
            List<Group<G>> groups,
            int selectionChunks,
            List<Candidate<K>> candidates,
            Parameters parameters
    ) {
        if (groups == null || groups.isEmpty()) return List.of();
        List<Candidate<K>> safeCandidates = candidates == null ? List.of() : candidates;
        Map<K, Double> remainingByKey = new LinkedHashMap<>();
        for (Candidate<K> candidate : safeCandidates) {
            if (candidate != null) {
                remainingByKey.merge(
                        candidate.key(), candidate.availableItems(), Math::max);
            }
        }

        List<GroupPlan<G, K>> result = new ArrayList<>(groups.size());
        for (Group<G> group : groups) {
            if (group == null) continue;
            List<Candidate<K>> remainingCandidates = safeCandidates.stream()
                    .filter(candidate -> candidate != null)
                    .map(candidate -> new Candidate<>(
                            candidate.key(), candidate.stableKey(), candidate.tier(),
                            Math.min(candidate.availableItems(),
                                    remainingByKey.getOrDefault(candidate.key(), 0.0)),
                            candidate.foodUnitsPerItem(), candidate.nutritionPoints()))
                    .toList();
            Plan<K> groupPlan = plan(
                    group.requestedFoodUnits(), selectionChunks, remainingCandidates,
                    group.recipients(), parameters);
            for (Map.Entry<K, Double> use : groupPlan.itemAmounts().entrySet()) {
                remainingByKey.computeIfPresent(use.getKey(),
                        (key, remaining) -> Math.max(0.0, remaining - nonNegative(use.getValue())));
            }
            result.add(new GroupPlan<>(group.key(), groupPlan));
        }
        return List.copyOf(result);
    }

    private static <K> double utility(
            CandidateState<K> candidate,
            double requestedFood,
            ResidentNutrition.NutritionIntake before,
            List<Recipient> recipients,
            Parameters parameters
    ) {
        double food = Math.min(requestedFood,
                candidate.items * candidate.source.foodUnitsPerItem());
        if (food <= EPSILON) return 0.0;
        double items = food / candidate.source.foodUnitsPerItem();
        ResidentNutrition.NutritionIntake after = before.plus(
                candidate.source.nutritionPoints().scale(items));
        return (aggregateDeficit(recipients, before, parameters)
                - aggregateDeficit(recipients, after, parameters)) / food;
    }

    /**
     * Simulates a menu against all recipients and sums their four-channel shortages.
     *
     * <p>Each recipient receives {@code menu * share}; the resulting reserves are compared with
     * {@link Parameters#healthyReserve()}. This is the objective used for candidate scoring.</p>
     *
     * @param recipients resident nutrition states and proportional shares
     * @param menu aggregate nutrition points in the proposed menu
     * @param parameters meal conversion and healthy reserve settings
     * @return non-negative aggregate shortage across every resident and channel
     */
    public static double aggregateDeficit(
            List<Recipient> recipients,
            ResidentNutrition.NutritionIntake menu,
            Parameters parameters
    ) {
        if (recipients == null || recipients.isEmpty()) return 0.0;
        Parameters safe = parameters == null ? Parameters.DEFAULT : parameters;
        ResidentNutrition.NutritionIntake safeMenu = menu == null
                ? ResidentNutrition.NutritionIntake.ZERO : menu;
        double deficit = 0.0;
        for (Recipient recipient : recipients) {
            if (recipient == null || recipient.share() <= 0.0) continue;
            ResidentNutrition projected = recipient.nutrition().withMeal(
                    safeMenu.scale(recipient.share()),
                    safe.referencePoints(), safe.gainAtReference(),
                    safe.maximumCoverage(), safe.maximumReserve());
            deficit += Math.max(0.0, safe.healthyReserve() - projected.fat())
                    + Math.max(0.0, safe.healthyReserve() - projected.carbohydrate())
                    + Math.max(0.0, safe.healthyReserve() - projected.protein())
                    + Math.max(0.0, safe.healthyReserve() - projected.vegetable());
        }
        return deficit;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    /** Inventory candidate considered by the shared-menu optimizer. */
    public record Candidate<K>(
            K key,
            String stableKey,
            int tier,
            double availableItems,
            double foodUnitsPerItem,
            ResidentNutrition.NutritionIntake nutritionPoints
    ) {
        public Candidate {
            stableKey = stableKey == null ? "" : stableKey;
            availableItems = nonNegative(availableItems);
            foodUnitsPerItem = nonNegative(foodUnitsPerItem);
            nutritionPoints = nutritionPoints == null
                    ? ResidentNutrition.NutritionIntake.ZERO : nutritionPoints;
        }
    }

    /** Resident starting state and fraction of the aggregate menu allocated to that resident. */
    public record Recipient(ResidentNutrition nutrition, double share) {
        public Recipient {
            nutrition = nutrition == null ? ResidentNutrition.DEFAULT_VALUE : nutrition;
            share = nonNegative(share);
        }
    }

    /** Numeric settings used while projecting resident meal gains and healthy-line deficits. */
    public record Parameters(
            double referencePoints,
            double gainAtReference,
            double maximumCoverage,
            double maximumReserve,
            double healthyReserve
    ) {
        public static final Parameters DEFAULT = new Parameters(200, 2, 2, 100, 70);

        public Parameters {
            referencePoints = Math.max(EPSILON, nonNegative(referencePoints));
            gainAtReference = nonNegative(gainAtReference);
            maximumCoverage = nonNegative(maximumCoverage);
            maximumReserve = nonNegative(maximumReserve);
            healthyReserve = nonNegative(healthyReserve);
        }
    }

    /** Ordered recipient group passed to the priority planner. */
    public record Group<G>(
            G key,
            double requestedFoodUnits,
            List<Recipient> recipients
    ) {
        public Group {
            requestedFoodUnits = nonNegative(requestedFoodUnits);
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }
    }

    /** Menu selected for one ordered group. */
    public record GroupPlan<G, K>(G key, Plan<K> plan) {
        public GroupPlan {
            plan = plan == null ? Plan.empty() : plan;
        }
    }

    /** Immutable result of one shared-menu planning pass. */
    public record Plan<K>(
            Map<K, Double> itemAmounts,
            double foodUnits,
            ResidentNutrition.NutritionIntake nutrition
    ) {
        public Plan {
            itemAmounts = itemAmounts == null ? Map.of() : Map.copyOf(itemAmounts);
            foodUnits = nonNegative(foodUnits);
            nutrition = nutrition == null
                    ? ResidentNutrition.NutritionIntake.ZERO : nutrition;
        }

        public static <K> Plan<K> empty() {
            return new Plan<>(Map.of(), 0.0, ResidentNutrition.NutritionIntake.ZERO);
        }
    }

    private static final class CandidateState<K> {
        private final Candidate<K> source;
        private double items;

        private CandidateState(Candidate<K> source) {
            this.source = source;
            this.items = source.availableItems();
        }

        private boolean available() {
            return items > EPSILON && source.foodUnitsPerItem() > EPSILON;
        }
    }
}
