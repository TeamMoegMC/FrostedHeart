/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Forge-independent kernels for the observables shared by gameplay and simulations. */
public final class TownObservationModel {
    private TownObservationModel() {
    }

    /**
     * Observes the low tail that can be hidden by the population mean.
     * P10 is the linearly interpolated tenth percentile of current residents.
     * Exit risk means the resident would cross a removal threshold at the next
     * morning settlement, including the configured homeless-health penalty.
     */
    public static ResidentSnapshot observeResidents(
            Collection<ResidentStatus> residents,
            ResidentRules rules
    ) {
        if (residents.isEmpty()) return ResidentSnapshot.empty();
        double[] health = new double[residents.size()];
        double[] mental = new double[residents.size()];
        Set<String> unableToWork = new LinkedHashSet<>();
        Set<String> exitRisk = new LinkedHashSet<>();
        int index = 0;
        double healthSum = 0.0;
        double mentalSum = 0.0;
        for (ResidentStatus resident : residents) {
            double residentHealth = finiteOrZero(resident.health());
            double residentMental = finiteOrZero(resident.mental());
            health[index] = residentHealth;
            mental[index] = residentMental;
            healthSum += residentHealth;
            mentalSum += residentMental;
            index++;
            if (!ResidentDailyModel.canWork(
                    resident.age(), residentHealth, residentMental, resident.hasHousing(),
                    rules.minimumWorkingAge(), rules.minimumWorkingHealthExclusive(),
                    rules.minimumWorkingMentalExclusive(), rules.workRequiresHousing())) {
                unableToWork.add(resident.id());
            }
            ResidentDailyModel.MorningResult morning = ResidentDailyModel.settleMorning(
                    residentHealth, residentMental, resident.hasHousing(),
                    rules.homelessHealthLossPerDay(), rules.removalHealthThreshold(),
                    rules.removalMentalThreshold());
            if (morning.removed()) exitRisk.add(resident.id());
        }
        Arrays.sort(health);
        Arrays.sort(mental);
        return new ResidentSnapshot(
                residents.size(), healthSum / residents.size(), percentile(health, 0.10), health[0],
                mentalSum / residents.size(), percentile(mental, 0.10), mental[0],
                List.copyOf(unableToWork), List.copyOf(exitRisk));
    }

    /** One-day reserve slope and its direct time-to-empty extrapolation. */
    public static ReserveSignal observeReserve(double currentDays, double previousDays) {
        double current = nonNegative(currentDays);
        double previous = nonNegative(previousDays);
        double trend = current - previous;
        double timeToEmpty = trend < 0.0 ? current / -trend : Double.POSITIVE_INFINITY;
        return new ReserveSignal(current, trend, timeToEmpty);
    }

    /** Variance-to-mean ratio of daily event counts; values above one are super-Poisson. */
    public static double fanoFactor(int[] dailyCounts) {
        if (dailyCounts.length == 0) return 0.0;
        double mean = Arrays.stream(dailyCounts).average().orElse(0.0);
        if (mean <= 0.0) return 0.0;
        double squared = 0.0;
        for (int count : dailyCounts) squared += (count - mean) * (count - mean);
        double variance = dailyCounts.length > 1
                ? squared / (dailyCounts.length - 1.0) : 0.0;
        return variance / mean;
    }

    /** Coefficient of variation of positive intervals between event days. */
    public static double intervalCoefficientOfVariation(List<Integer> eventDays) {
        if (eventDays.size() < 3) return 0.0;
        List<Integer> intervals = new ArrayList<>(eventDays.size() - 1);
        for (int index = 1; index < eventDays.size(); index++) {
            int interval = eventDays.get(index) - eventDays.get(index - 1);
            if (interval > 0) intervals.add(interval);
        }
        if (intervals.size() < 2) return 0.0;
        double mean = intervals.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        if (mean <= 0.0) return 0.0;
        double squared = intervals.stream()
                .mapToDouble(value -> (value - mean) * (value - mean)).sum();
        return Math.sqrt(squared / (intervals.size() - 1.0)) / mean;
    }

    private static double percentile(double[] sorted, double probability) {
        if (sorted.length == 0) return 0.0;
        double position = probability * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) return sorted[lower];
        double weight = position - lower;
        return sorted[lower] * (1.0 - weight) + sorted[upper] * weight;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finiteOrZero(value));
    }

    public record ResidentStatus(
            String id,
            int age,
            double health,
            double mental,
            boolean hasHousing
    ) {
    }

    public record ResidentRules(
            double homelessHealthLossPerDay,
            double removalHealthThreshold,
            double removalMentalThreshold,
            int minimumWorkingAge,
            double minimumWorkingHealthExclusive,
            double minimumWorkingMentalExclusive,
            boolean workRequiresHousing
    ) {
    }

    public record ResidentSnapshot(
            int population,
            double averageHealth,
            double p10Health,
            double minimumHealth,
            double averageMental,
            double p10Mental,
            double minimumMental,
            List<String> unableToWorkResidentIds,
            List<String> exitRiskResidentIds
    ) {
        public ResidentSnapshot {
            unableToWorkResidentIds = List.copyOf(unableToWorkResidentIds);
            exitRiskResidentIds = List.copyOf(exitRiskResidentIds);
        }

        public static ResidentSnapshot empty() {
            return new ResidentSnapshot(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    List.of(), List.of());
        }

        public int unableToWorkCount() {
            return unableToWorkResidentIds.size();
        }

        public int exitRiskCount() {
            return exitRiskResidentIds.size();
        }
    }

    public record ReserveSignal(double reserveDays, double dailyTrend, double timeToEmptyDays) {
    }
}
