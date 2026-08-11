/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resident;

/** Forge-independent resident survival and work-eligibility formulas. */
public final class ResidentDailyModel {
    private ResidentDailyModel() {
    }

    public static boolean canWork(
            int age,
            double health,
            double mental,
            boolean hasHousing,
            int minimumWorkingAge,
            double minimumWorkingHealthExclusive,
            double minimumWorkingMentalExclusive,
            boolean workRequiresHousing
    ) {
        return age >= minimumWorkingAge
                && health > minimumWorkingHealthExclusive
                && mental > minimumWorkingMentalExclusive
                && (!workRequiresHousing || hasHousing);
    }

    /**
     * Applies the morning homeless-health penalty first, then evaluates the
     * same inclusive removal thresholds used by the town settlement loop.
     */
    public static MorningResult settleMorning(
            double health,
            double mental,
            boolean hasHousing,
            double homelessHealthLossPerDay,
            double removalHealthThreshold,
            double removalMentalThreshold
    ) {
        double settledHealth = finiteOrZero(health);
        double settledMental = finiteOrZero(mental);
        if (!hasHousing) {
            settledHealth = Math.max(0.0, settledHealth - nonNegative(homelessHealthLossPerDay));
        }
        return new MorningResult(
                settledHealth,
                settledHealth <= finiteOrZero(removalHealthThreshold),
                settledMental <= finiteOrZero(removalMentalThreshold));
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finiteOrZero(value));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    public record MorningResult(
            double healthAfterHomelessPenalty,
            boolean removedForHealth,
            boolean removedForMental
    ) {
        public boolean removed() {
            return removedForHealth || removedForMental;
        }
    }
}
