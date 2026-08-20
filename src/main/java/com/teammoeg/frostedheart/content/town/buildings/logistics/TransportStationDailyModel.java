/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.teammoeg.frostedheart.content.town.TownMathFunctions;

import java.util.Collection;

/** Forge-independent arithmetic for one transport-station settlement. */
public final class TransportStationDailyModel {
    private TransportStationDailyModel() {
    }

    /** Resident attributes and transport proficiency captured before work. */
    public record WorkerInput(
            double health,
            double mental,
            double strength,
            double intelligence,
            double proficiency
    ) {
    }

    /** Runtime or simulator parameters for transport production. */
    public record Parameters(
            double transportCapacityPerStandardWorkerDay,
            double healthWeight,
            double mentalWeight,
            double strengthWeight,
            double intelligenceWeight,
            double productivityAtAttributeZero,
            double productivityAtAttributeHundred,
            double maximumProficiency,
            double bonusAtMaximumProficiency,
            double minimumProductivity,
            double maximumProductivity
    ) {
    }

    /** Pure result for all eligible workers assigned to one station. */
    public record DailyResult(
            int workerCount,
            double totalProductivity,
            double producedCapacity
    ) {
    }

    public static DailyResult calculate(Collection<WorkerInput> workers, Parameters parameters) {
        if (workers == null || workers.isEmpty() || parameters == null) {
            return new DailyResult(0, 0.0, 0.0);
        }
        int workerCount = 0;
        double totalProductivity = 0.0;
        for (WorkerInput worker : workers) {
            if (worker == null) continue;
            workerCount++;
            totalProductivity = saturatingAdd(
                    totalProductivity, residentProductivity(worker, parameters));
        }
        return new DailyResult(
                workerCount,
                totalProductivity,
                producedCapacity(totalProductivity,
                        parameters.transportCapacityPerStandardWorkerDay()));
    }

    public static double residentProductivity(WorkerInput worker, Parameters parameters) {
        if (worker == null || parameters == null) return 0.0;
        double result = TownMathFunctions.linearResidentProductivity(
                new double[]{
                        finiteOrZero(worker.health()),
                        finiteOrZero(worker.mental()),
                        finiteOrZero(worker.strength()),
                        finiteOrZero(worker.intelligence())
                },
                new double[]{
                        nonNegative(parameters.healthWeight()),
                        nonNegative(parameters.mentalWeight()),
                        nonNegative(parameters.strengthWeight()),
                        nonNegative(parameters.intelligenceWeight())
                },
                finiteOrZero(worker.proficiency()),
                nonNegative(parameters.productivityAtAttributeZero()),
                nonNegative(parameters.productivityAtAttributeHundred()),
                nonNegative(parameters.maximumProficiency()),
                nonNegative(parameters.bonusAtMaximumProficiency()),
                nonNegative(parameters.minimumProductivity()),
                nonNegative(parameters.maximumProductivity())
        );
        return nonNegative(result);
    }

    public static double producedCapacity(
            double totalProductivity,
            double transportCapacityPerStandardWorkerDay
    ) {
        double productivity = nonNegative(totalProductivity);
        double outputPerWorker = nonNegative(transportCapacityPerStandardWorkerDay);
        double result = productivity * outputPerWorker;
        return Double.isFinite(result) ? result : 0.0;
    }

    private static double saturatingAdd(double left, double right) {
        double result = nonNegative(left) + nonNegative(right);
        return Double.isFinite(result) ? result : Double.MAX_VALUE;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
