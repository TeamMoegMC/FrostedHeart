/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.model;

import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/** Forge-independent residential triage and stability rules. */
public final class TownResidentCareModel {
    public static final double DEFAULT_SCORE_BAND = 0.05;

    private TownResidentCareModel() {
    }

    public static Need assess(
            UUID id,
            int age,
            double health,
            double mental,
            ResidentNutrition nutrition,
            int minimumWorkingAge,
            double minimumWorkingHealthExclusive,
            double minimumWorkingMentalExclusive,
            double removalHealthThreshold,
            double removalMentalThreshold
    ) {
        return assess(id, age, health, mental, nutrition, minimumWorkingAge,
                minimumWorkingHealthExclusive, minimumWorkingMentalExclusive,
                removalHealthThreshold, removalMentalThreshold,
                ResidentNutrition.HEALTHY, ResidentNutrition.SEVERE);
    }

    public static Need assess(
            UUID id,
            int age,
            double health,
            double mental,
            ResidentNutrition nutrition,
            int minimumWorkingAge,
            double minimumWorkingHealthExclusive,
            double minimumWorkingMentalExclusive,
            double removalHealthThreshold,
            double removalMentalThreshold,
            double healthyNutritionReserve,
            double severeNutritionReserve
    ) {
        Objects.requireNonNull(id, "id");
        ResidentNutrition safeNutrition = nutrition == null
                ? ResidentNutrition.DEFAULT_VALUE : nutrition;
        double healthRisk = normalizedRisk(health, removalHealthThreshold);
        double mentalRisk = normalizedRisk(mental, removalMentalThreshold);
        double nutritionRisk = safeNutrition.nutritionRisk(healthyNutritionReserve);
        double primary = Math.max(healthRisk, Math.max(mentalRisk, nutritionRisk));
        double mean = (healthRisk + mentalRisk + nutritionRisk) / 3.0;
        boolean labourCapable = age >= minimumWorkingAge
                && health > minimumWorkingHealthExclusive
                && mental > minimumWorkingMentalExclusive;
        boolean critical = health <= minimumWorkingHealthExclusive
                || mental <= minimumWorkingMentalExclusive
                || safeNutrition.minimum() < severeNutritionReserve;
        return new Need(id, labourCapable, critical, primary, mean,
                safeNutrition.severeChannelCount(severeNutritionReserve));
    }

    public static Comparator<Need> comparator(TownCareLaw law) {
        return comparator(law, DEFAULT_SCORE_BAND);
    }

    public static Comparator<Need> comparator(TownCareLaw law, double scoreBand) {
        TownCareLaw safeLaw = law == null ? TownCareLaw.CLINICAL_TRIAGE : law;
        return (first, second) -> {
            int policy = comparePolicyGroup(first, second, safeLaw);
            if (policy != 0) return policy;
            int critical = Boolean.compare(second.critical(), first.critical());
            if (critical != 0) return critical;
            int severe = Integer.compare(second.severeNutritionChannels(),
                    first.severeNutritionChannels());
            if (severe != 0) return severe;
            int primary = Integer.compare(scoreBand(second.primaryRisk(), scoreBand),
                    scoreBand(first.primaryRisk(), scoreBand));
            if (primary != 0) return primary;
            int mean = Double.compare(second.meanRisk(), first.meanRisk());
            return mean != 0 ? mean : first.id().compareTo(second.id());
        };
    }

    /** House-specific tie breaker; stable residents win only within one risk band. */
    public static Comparator<Need> comparatorForHouse(
            TownCareLaw law,
            java.util.function.Predicate<UUID> livedHere
    ) {
        return comparatorForHouse(law, livedHere, DEFAULT_SCORE_BAND);
    }

    public static Comparator<Need> comparatorForHouse(
            TownCareLaw law,
            java.util.function.Predicate<UUID> livedHere,
            double scoreBand
    ) {
        Comparator<Need> base = comparator(law, scoreBand);
        return (first, second) -> {
            int firstPolicy = policyGroup(first, law);
            int secondPolicy = policyGroup(second, law);
            if (firstPolicy != secondPolicy) return Integer.compare(firstPolicy, secondPolicy);
            int critical = Boolean.compare(second.critical(), first.critical());
            if (critical != 0) return critical;
            int severe = Integer.compare(second.severeNutritionChannels(),
                    first.severeNutritionChannels());
            if (severe != 0) return severe;
            int firstBand = scoreBand(first.primaryRisk(), scoreBand);
            int secondBand = scoreBand(second.primaryRisk(), scoreBand);
            if (firstBand != secondBand) return Integer.compare(secondBand, firstBand);
            boolean firstStayed = livedHere.test(first.id());
            boolean secondStayed = livedHere.test(second.id());
            if (firstStayed != secondStayed) return firstStayed ? -1 : 1;
            return base.compare(first, second);
        };
    }

    private static int comparePolicyGroup(Need first, Need second, TownCareLaw law) {
        return Integer.compare(policyGroup(first, law), policyGroup(second, law));
    }

    private static int policyGroup(Need need, TownCareLaw law) {
        TownCareLaw safe = law == null ? TownCareLaw.CLINICAL_TRIAGE : law;
        return switch (safe) {
            case CLINICAL_TRIAGE -> 0;
            case DEPENDENT_FIRST -> need.labourCapable() ? 1 : 0;
            case WORKFORCE_FIRST -> need.labourCapable() ? 0 : 1;
        };
    }

    private static int scoreBand(double score) {
        return scoreBand(score, DEFAULT_SCORE_BAND);
    }

    private static int scoreBand(double score, double width) {
        double safeWidth = Double.isFinite(width) && width > 0.0 ? width : DEFAULT_SCORE_BAND;
        return (int) Math.floor(Math.max(0.0, Math.min(1.0, score)) / safeWidth);
    }

    private static double normalizedRisk(double value, double removalThreshold) {
        double safeValue = Double.isFinite(value) ? value : 0.0;
        double floor = Double.isFinite(removalThreshold) ? removalThreshold : 0.0;
        double range = Math.max(1.0, 100.0 - floor);
        return Math.max(0.0, Math.min(1.0, 1.0 - (safeValue - floor) / range));
    }

    public record Need(
            UUID id,
            boolean labourCapable,
            boolean critical,
            double primaryRisk,
            double meanRisk,
            int severeNutritionChannels
    ) {
    }
}
