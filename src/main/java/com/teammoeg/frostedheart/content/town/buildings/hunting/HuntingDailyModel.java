/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.frostedheart.content.town.TownMathFunctions;

/** Forge-independent integer-roll settlement used by the game and simulator. */
public final class HuntingDailyModel {
    private HuntingDailyModel() {
    }

    public static RollPlan planRolls(
            double totalHuntingSwe,
            double rollsPerSweDay,
            double passiveRollsPerBaseDay,
            double previousCarry,
            boolean useFractionalCarry,
            double availableHuntUnits
    ) {
        double workerExpectedRolls = nonNegative(totalHuntingSwe) * nonNegative(rollsPerSweDay);
        double newExpectedRolls = nonNegative(passiveRollsPerBaseDay) + workerExpectedRolls;
        int plannedRolls;
        double nextCarry;
        if (useFractionalCarry) {
            TownMathFunctions.FractionalSettlement settlement =
                    TownMathFunctions.settleFractionalAmount(previousCarry, newExpectedRolls);
            plannedRolls = saturatingInt(settlement.wholeAmount());
            nextCarry = settlement.carry();
        } else {
            plannedRolls = saturatingInt((long) Math.floor(newExpectedRolls));
            nextCarry = 0.0;
        }
        int availableRolls = availableHuntUnits == Double.POSITIVE_INFINITY
                ? Integer.MAX_VALUE
                : saturatingInt((long) Math.floor(nonNegative(availableHuntUnits)));
        int executedRolls = executedRolls(plannedRolls, availableHuntUnits);
        boolean hasWorkerOpportunity = workerExpectedRolls > 0.0 && availableRolls > 0;
        return new RollPlan(
                workerExpectedRolls,
                newExpectedRolls,
                plannedRolls,
                executedRolls,
                nextCarry,
                hasWorkerOpportunity);
    }

    public static int executedRolls(int plannedRolls, double availableHuntUnits) {
        int safePlanned = Math.max(0, plannedRolls);
        if (availableHuntUnits == Double.POSITIVE_INFINITY) return safePlanned;
        int availableRolls = saturatingInt((long) Math.floor(nonNegative(availableHuntUnits)));
        return Math.min(safePlanned, availableRolls);
    }

    /** Current dynamic vacancy priority for one hunting base. */
    public static double assignmentPriority(
            int currentWorkers,
            int maximumWorkers,
            double rating,
            double basePriority,
            double penaltyPerWorker,
            double fillRatioBonus,
            double ratingMultiplier
    ) {
        int maximum = Math.max(0, maximumWorkers);
        int current = Math.max(0, currentWorkers);
        if (maximum == 0 || current >= maximum) return Double.NEGATIVE_INFINITY;
        return finiteOrZero(basePriority)
                - nonNegative(penaltyPerWorker) * current
                + finiteOrZero(fillRatioBonus) * current / maximum
                + finiteOrZero(ratingMultiplier) * finiteOrZero(rating);
    }

    private static int saturatingInt(long value) {
        if (value <= 0L) return 0;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    public record RollPlan(
            double workerExpectedRolls,
            double newExpectedRolls,
            int plannedRolls,
            int executedRolls,
            double nextCarry,
            boolean hasWorkerOpportunity
    ) {
    }
}
