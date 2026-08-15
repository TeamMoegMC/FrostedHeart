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

import com.teammoeg.frostedheart.content.town.observation.TownObservationModel;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stateful event/episode observer layered on the exact stage-4 day transition. */
final class TownStageFourObserver {
    static final double SOFT_RESERVE_WARNING_DAYS =
            TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS;
    static final double RECOVERED_RESERVE_DAYS =
            TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_WARNING_DAYS;
    private static final double EPSILON = 1.0e-9;

    private final int simulatedDays;
    private final int initialPopulation;
    private final TownObservationModel.ResidentRules rules;
    private final List<TownSignalEvent> events = new ArrayList<>();
    private final List<DailyObservation> observations = new ArrayList<>();
    private final int[] dailyExits;
    private final int[] dailyAdverseEventCounts;
    private Set<String> previousUnable;
    private Set<String> previousExitRisk;
    private double previousFoodReserve;
    private double previousFuelReserve;
    private boolean previousFoodLow;
    private boolean previousFuelLow;
    private boolean previousFoodShortage;
    private boolean previousFuelShortage;
    private boolean previousHouseUnsafe;
    private boolean previousHuntingStopped;
    private boolean previousClimateCold;
    private boolean crisisActive;
    private long currentEpisodeId;
    private int episodeStartDay;
    private final Set<String> currentEpisodeAffectedResidentIds = new HashSet<>();
    private int crisisEpisodes;
    private int recoveredEpisodes;
    private int maximumEpisodeAffectedResidents;
    private final List<Integer> recoveryDurations = new ArrayList<>();
    private Integer firstExitWarningLeadDays;
    private double minimumAverageHealth = Double.POSITIVE_INFINITY;
    private double minimumP10Health = Double.POSITIVE_INFINITY;
    private double minimumAverageMental = Double.POSITIVE_INFINITY;
    private double minimumP10Mental = Double.POSITIVE_INFINITY;
    private double minimumFoodReserve = Double.POSITIVE_INFINITY;
    private double minimumFuelReserve = Double.POSITIVE_INFINITY;
    private double maximumFoodDrawdown;
    private double maximumFuelDrawdown;
    private int maximumUnableToWork;
    private int maximumExitRisk;

    TownStageFourObserver(
            int simulatedDays,
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters
    ) {
        this.simulatedDays = simulatedDays;
        this.initialPopulation = state.residents().size();
        this.rules = residentRules(parameters);
        this.dailyExits = new int[simulatedDays];
        this.dailyAdverseEventCounts = new int[simulatedDays];
        TownObservationModel.ResidentSnapshot initial = observe(state.residents(), rules);
        this.previousUnable = Set.copyOf(initial.unableToWorkResidentIds());
        this.previousExitRisk = Set.copyOf(initial.exitRiskResidentIds());
        this.previousFoodReserve = TownStageThreeModel.foodReserveDays(
                state, data, parameters, initialPopulation);
        this.previousFuelReserve = TownStageThreeModel.fuelReserveDays(
                state, scenario, data, parameters);
        this.previousFoodLow = previousFoodReserve < SOFT_RESERVE_WARNING_DAYS;
        this.previousFuelLow = previousFuelReserve < SOFT_RESERVE_WARNING_DAYS;
    }

    static List<TownObservationModel.ResidentStatus> copyStatuses(TownStageThreeState state) {
        return state.residents().stream().map(TownStageFourObserver::status).toList();
    }

    void observeDay(
            int day,
            List<TownObservationModel.ResidentStatus> beforeSettlement,
            TownStageThreeState state,
            TownStageThreeModel.DayResult result,
            TownStageThreeModel.DailyEnvironment environment,
            boolean climateBelowFullCoverageLimit
    ) {
        TownObservationModel.ResidentSnapshot current = observe(state.residents(), rules);
        Set<String> afterIds = new HashSet<>();
        for (TownStageThreeState.ResidentState resident : state.residents()) afterIds.add(resident.id());
        List<TownObservationModel.ResidentStatus> exited = beforeSettlement.stream()
                .filter(status -> !afterIds.contains(status.id())).toList();

        Set<String> currentUnable = Set.copyOf(current.unableToWorkResidentIds());
        Set<String> currentExitRisk = Set.copyOf(current.exitRiskResidentIds());
        Set<String> newlyUnableIds = difference(currentUnable, previousUnable);
        int newlyUnable = newlyUnableIds.size();
        int newlyAtExitRisk = differenceCount(currentExitRisk, previousExitRisk);
        int recoveredWork = differenceCount(previousUnable, currentUnable);
        int recoveredExitRisk = differenceCount(previousExitRisk, currentExitRisk);
        boolean foodLow = result.foodReserveDays() < SOFT_RESERVE_WARNING_DAYS;
        boolean fuelLow = result.fuelReserveDays() < SOFT_RESERVE_WARNING_DAYS;
        boolean foodShortage = result.foodSatisfaction() < 1.0 - EPSILON;
        boolean fuelShortage = result.towerServiceFraction() < 1.0 - EPSILON;
        boolean houseUnsafe = !environment.houseAcceptsNewResidents();
        boolean huntingStopped = !environment.huntingWorkable();

        boolean adverseState = foodLow || fuelLow || foodShortage || fuelShortage
                || houseUnsafe || huntingStopped || !exited.isEmpty()
                || current.exitRiskCount() > 0;
        if (!crisisActive && adverseState) {
            crisisActive = true;
            currentEpisodeId = ++crisisEpisodes;
            episodeStartDay = day;
            currentEpisodeAffectedResidentIds.clear();
        }
        long episodeId = crisisActive ? currentEpisodeId : 0L;
        int eventStart = events.size();

        addCrossing(day, foodLow, previousFoodLow,
                TownSignalEvent.Type.FOOD_RESERVE_WARNING,
                TownSignalEvent.Severity.WARNING, 1, episodeId, "food reserve below 3 days");
        addCrossing(day, fuelLow, previousFuelLow,
                TownSignalEvent.Type.FUEL_RESERVE_WARNING,
                TownSignalEvent.Severity.WARNING, 1, episodeId, "T1 fuel reserve below 3 days");
        addCrossing(day, foodShortage, previousFoodShortage,
                TownSignalEvent.Type.FOOD_SHORTAGE,
                TownSignalEvent.Severity.CRITICAL, Math.max(1, result.population()), episodeId,
                "resident food satisfaction below 1");
        addCrossing(day, fuelShortage, previousFuelShortage,
                TownSignalEvent.Type.FUEL_SHORTAGE,
                TownSignalEvent.Severity.CRITICAL, 1, episodeId, "T1 service below 1");
        addTwoWayCrossing(day, houseUnsafe, previousHouseUnsafe,
                TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE,
                TownSignalEvent.Type.HOUSE_TEMPERATURE_RECOVERED,
                result.population(), episodeId, "house morning temperature outside 0-40 C");
        addTwoWayCrossing(day, huntingStopped, previousHuntingStopped,
                TownSignalEvent.Type.HUNTING_TEMPERATURE_STOP,
                TownSignalEvent.Type.HUNTING_TEMPERATURE_RECOVERED,
                Math.max(1, result.assignedHunters()), episodeId,
                "hunting morning temperature below 0 C");
        addTwoWayCrossing(day, climateBelowFullCoverageLimit, previousClimateCold,
                TownSignalEvent.Type.CLIMATE_COLD_WARNING,
                TownSignalEvent.Type.CLIMATE_COLD_ENDED,
                1, episodeId, "climate below fully covered T1 housing limit");
        if (newlyUnable > 0) add(day, TownSignalEvent.Type.WORK_CAPACITY_LOST,
                TownSignalEvent.Severity.CRITICAL, newlyUnable, episodeId,
                "resident crossed current work-eligibility threshold");
        if (recoveredWork > 0) add(day, TownSignalEvent.Type.WORK_CAPACITY_RECOVERED,
                TownSignalEvent.Severity.INFORMATION, recoveredWork, episodeId,
                "resident returned above current work-eligibility threshold");
        if (newlyAtExitRisk > 0) add(day, TownSignalEvent.Type.EXIT_RISK_ENTERED,
                TownSignalEvent.Severity.CRITICAL, newlyAtExitRisk, episodeId,
                "resident would leave at next morning settlement");
        if (recoveredExitRisk > 0) add(day, TownSignalEvent.Type.EXIT_RISK_RECOVERED,
                TownSignalEvent.Severity.INFORMATION, recoveredExitRisk, episodeId,
                "resident moved above next-morning removal threshold");
        for (TownObservationModel.ResidentStatus resident : exited) {
            ResidentDailyModel.MorningResult cause = ResidentDailyModel.settleMorning(
                    resident.health(), resident.mental(), resident.hasHousing(),
                    rules.homelessHealthLossPerDay(), rules.removalHealthThreshold(),
                    rules.removalMentalThreshold());
            TownSignalEvent.Type type = cause.removedForHealth() && cause.removedForMental()
                    ? TownSignalEvent.Type.RESIDENT_EXIT_BOTH
                    : cause.removedForHealth() ? TownSignalEvent.Type.RESIDENT_EXIT_HEALTH
                    : TownSignalEvent.Type.RESIDENT_EXIT_MENTAL;
            add(day, type, TownSignalEvent.Severity.IRREVERSIBLE, 1, episodeId, resident.id());
        }

        currentEpisodeAffectedResidentIds.addAll(newlyUnableIds);
        exited.forEach(resident -> currentEpisodeAffectedResidentIds.add(resident.id()));
        maximumEpisodeAffectedResidents = Math.max(
                maximumEpisodeAffectedResidents, currentEpisodeAffectedResidentIds.size());
        dailyExits[day] = exited.size();
        if (!exited.isEmpty() && firstExitWarningLeadDays == null) {
            firstExitWarningLeadDays = crisisActive ? day - episodeStartDay : 0;
        }
        int adverseEvents = 0;
        for (int index = eventStart; index < events.size(); index++) {
            TownSignalEvent event = events.get(index);
            if (event.severity() != TownSignalEvent.Severity.INFORMATION) {
                adverseEvents++;
            }
        }
        dailyAdverseEventCounts[day] = adverseEvents;

        TownObservationModel.ReserveSignal foodSignal =
                TownObservationModel.observeReserve(result.foodReserveDays(), previousFoodReserve);
        TownObservationModel.ReserveSignal fuelSignal =
                TownObservationModel.observeReserve(result.fuelReserveDays(), previousFuelReserve);
        boolean recovered = crisisActive
                && result.foodReserveDays() >= RECOVERED_RESERVE_DAYS
                && result.fuelReserveDays() >= RECOVERED_RESERVE_DAYS
                && !foodShortage && !fuelShortage && !houseUnsafe && !huntingStopped
                && current.exitRiskCount() == 0;
        if (recovered) {
            int duration = day - episodeStartDay + 1;
            recoveryDurations.add(duration);
            recoveredEpisodes++;
            add(day, TownSignalEvent.Type.CRISIS_RECOVERED,
                    TownSignalEvent.Severity.INFORMATION, 1, currentEpisodeId,
                    "7-day food and fuel reserves with all critical services restored");
            crisisActive = false;
        }
        observations.add(new DailyObservation(
                day, result.population(), result.cumulativeDeaths(),
                current.averageHealth(), current.p10Health(), current.minimumHealth(),
                current.averageMental(), current.p10Mental(), current.minimumMental(),
                current.unableToWorkCount(), current.exitRiskCount(),
                foodSignal.reserveDays(), foodSignal.dailyTrend(), foodSignal.timeToEmptyDays(),
                fuelSignal.reserveDays(), fuelSignal.dailyTrend(), fuelSignal.timeToEmptyDays(),
                adverseEvents, exited.size(), crisisActive, episodeId));

        minimumAverageHealth = Math.min(minimumAverageHealth, current.averageHealth());
        minimumP10Health = Math.min(minimumP10Health, current.p10Health());
        minimumAverageMental = Math.min(minimumAverageMental, current.averageMental());
        minimumP10Mental = Math.min(minimumP10Mental, current.p10Mental());
        minimumFoodReserve = Math.min(minimumFoodReserve, result.foodReserveDays());
        minimumFuelReserve = Math.min(minimumFuelReserve, result.fuelReserveDays());
        maximumFoodDrawdown = Math.max(maximumFoodDrawdown,
                Math.max(0.0, previousFoodReserve - result.foodReserveDays()));
        maximumFuelDrawdown = Math.max(maximumFuelDrawdown,
                Math.max(0.0, previousFuelReserve - result.fuelReserveDays()));
        maximumUnableToWork = Math.max(maximumUnableToWork, current.unableToWorkCount());
        maximumExitRisk = Math.max(maximumExitRisk, current.exitRiskCount());

        previousUnable = currentUnable;
        previousExitRisk = currentExitRisk;
        previousFoodReserve = result.foodReserveDays();
        previousFuelReserve = result.fuelReserveDays();
        previousFoodLow = foodLow;
        previousFuelLow = fuelLow;
        previousFoodShortage = foodShortage;
        previousFuelShortage = fuelShortage;
        previousHouseUnsafe = houseUnsafe;
        previousHuntingStopped = huntingStopped;
        previousClimateCold = climateBelowFullCoverageLimit;
    }

    RunMetrics finish() {
        if (maximumEpisodeAffectedResidents > initialPopulation) {
            throw new IllegalStateException("An episode cannot affect more distinct residents than the initial population.");
        }
        int totalExits = java.util.Arrays.stream(dailyExits).sum();
        int totalAdverse = java.util.Arrays.stream(dailyAdverseEventCounts).sum();
        List<Integer> exitDays = new ArrayList<>();
        for (int day = 0; day < dailyExits.length; day++) {
            if (dailyExits[day] > 0) exitDays.add(day);
        }
        double meanRecovery = recoveryDurations.stream().mapToInt(Integer::intValue)
                .average().orElse(0.0);
        return new RunMetrics(
                finiteMinimum(minimumAverageHealth), finiteMinimum(minimumP10Health),
                finiteMinimum(minimumAverageMental), finiteMinimum(minimumP10Mental),
                fraction(maximumUnableToWork, initialPopulation),
                fraction(maximumExitRisk, initialPopulation),
                finiteMinimum(minimumFoodReserve), finiteMinimum(minimumFuelReserve),
                maximumFoodDrawdown, maximumFuelDrawdown,
                ratePer30(totalExits), ratePer30(totalAdverse),
                TownObservationModel.fanoFactor(dailyExits),
                TownObservationModel.fanoFactor(dailyAdverseEventCounts),
                TownObservationModel.intervalCoefficientOfVariation(exitDays),
                crisisEpisodes, fraction(maximumEpisodeAffectedResidents, initialPopulation),
                firstExitWarningLeadDays == null ? -1 : firstExitWarningLeadDays,
                recoveredEpisodes, meanRecovery, crisisActive);
    }

    List<TownSignalEvent> events() {
        return List.copyOf(events);
    }

    List<DailyObservation> observations() {
        return List.copyOf(observations);
    }

    DailyObservation latestObservation() {
        return observations.get(observations.size() - 1);
    }

    private void addCrossing(
            int day,
            boolean current,
            boolean previous,
            TownSignalEvent.Type type,
            TownSignalEvent.Severity severity,
            int affected,
            long episodeId,
            String detail
    ) {
        if (current && !previous) add(day, type, severity, affected, episodeId, detail);
    }

    private void addTwoWayCrossing(
            int day,
            boolean current,
            boolean previous,
            TownSignalEvent.Type entered,
            TownSignalEvent.Type recovered,
            int affected,
            long episodeId,
            String detail
    ) {
        if (current == previous) return;
        add(day, current ? entered : recovered,
                current ? TownSignalEvent.Severity.WARNING
                        : TownSignalEvent.Severity.INFORMATION,
                affected, episodeId, detail);
    }

    private void add(
            int day,
            TownSignalEvent.Type type,
            TownSignalEvent.Severity severity,
            int affected,
            long episodeId,
            String detail
    ) {
        events.add(new TownSignalEvent(day, 0, type, severity,
                Math.max(1, affected), episodeId, detail));
    }

    private double ratePer30(int count) {
        return simulatedDays > 0 ? count * 30.0 / simulatedDays : 0.0;
    }

    private static int differenceCount(Set<String> left, Set<String> right) {
        int count = 0;
        for (String value : left) if (!right.contains(value)) count++;
        return count;
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private static double fraction(int numerator, int denominator) {
        return denominator > 0 ? (double) numerator / denominator : 0.0;
    }

    private static double finiteMinimum(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static TownObservationModel.ResidentSnapshot observe(
            List<TownStageThreeState.ResidentState> residents,
            TownObservationModel.ResidentRules rules
    ) {
        return TownObservationModel.observeResidents(
                residents.stream().map(TownStageFourObserver::status).toList(), rules);
    }

    private static TownObservationModel.ResidentStatus status(
            TownStageThreeState.ResidentState resident
    ) {
        return new TownObservationModel.ResidentStatus(
                resident.id(), resident.age(), resident.health(), resident.mental(),
                resident.homeId() != null);
    }

    private static TownObservationModel.ResidentRules residentRules(
            TownModelParameters parameters
    ) {
        TownModelParameters.ResidentParameters resident = parameters.residents();
        return new TownObservationModel.ResidentRules(
                resident.homelessHealthLossPerDay(), resident.removalHealthThreshold(),
                resident.removalMentalThreshold(), resident.minimumWorkingAge(),
                resident.minimumWorkingHealthExclusive(),
                resident.minimumWorkingMentalExclusive(), resident.workRequiresHousing());
    }

    record DailyObservation(
            int day,
            int population,
            int cumulativeExits,
            double averageHealth,
            double p10Health,
            double minimumHealth,
            double averageMental,
            double p10Mental,
            double minimumMental,
            int unableToWorkCount,
            int exitRiskCount,
            double foodReserveDays,
            double foodReserveTrendDaysPerDay,
            double foodTimeToEmptyDays,
            double fuelReserveDays,
            double fuelReserveTrendDaysPerDay,
            double fuelTimeToEmptyDays,
            int adverseEventCount,
            int residentExits,
            boolean crisisActive,
            long episodeId
    ) {
    }

    record RunMetrics(
            double minimumAverageHealth,
            double minimumP10Health,
            double minimumAverageMental,
            double minimumP10Mental,
            double maximumUnableToWorkFraction,
            double maximumExitRiskFraction,
            double minimumFoodReserveDays,
            double minimumFuelReserveDays,
            double maximumFoodDrawdownDays,
            double maximumFuelDrawdownDays,
            double residentExitRatePer30Days,
            double adverseSignalRatePer30Days,
            double residentExitFanoFactor,
            double adverseSignalFanoFactor,
            double residentExitIntervalCv,
            int crisisEpisodeCount,
            double maximumEpisodeAffectedFraction,
            int firstExitWarningLeadDays,
            int recoveredEpisodeCount,
            double meanRecoveryDays,
            boolean unrecoveredEpisode
    ) {
    }
}
