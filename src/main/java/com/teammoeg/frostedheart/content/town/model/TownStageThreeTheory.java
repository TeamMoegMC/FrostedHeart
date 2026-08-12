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
import java.util.List;

/** Closed-form stage-3 labor and self-supply frontier at initial standard attributes. */
public final class TownStageThreeTheory {
    private TownStageThreeTheory() {
    }

    public static TheorySummary evaluate(
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters
    ) {
        double standardMiningSwe = parameters.mining().productivity().standardWorkerEquivalent();
        double standardHuntingSwe = parameters.hunting().productivity().standardWorkerEquivalent();
        double coalFraction = TownStageOneTwoTheory.itemWeightFraction(
                data.mineWeights(), "minecraft:coal");
        double coalPerMiningSwe = parameters.mining().baseOutputPerStandardWorkerDay() * coalFraction;
        TownStageOneTwoTheory.Moment cookedMoment = TownStageOneTwoTheory.lootMoment(
                data.huntingLoot(), data.meats(), Double.POSITIVE_INFINITY);
        TownStageOneTwoTheory.Moment rawMoment = TownStageOneTwoTheory.lootMoment(
                data.huntingLoot(), data.meats(), 0.0);
        double rollsPerHuntingSwe = parameters.hunting().expectedLootRollsPerStandardWorkerDay();
        double cookedFoodPerHuntingSwe = cookedMoment.meanFoodUnitsPerRoll() * rollsPerHuntingSwe;
        double rawFoodPerHuntingSwe = rawMoment.meanFoodUnitsPerRoll() * rollsPerHuntingSwe;
        int recipeTicks = "coal".equals(scenario.tower().fuel())
                ? data.coalRecipeProcessTicks() : data.cokeRecipeProcessTicks();
        TownStageOneTwoTheory.TowerFuelTheory tower = TownStageOneTwoTheory.towerFuel(
                recipeTicks, parameters.generatorT1(), scenario.tower().overdrive(),
                scenario.tower().researchEfficiencyBonus());
        double rawCoalDemand = tower.theoryItemsPerActiveDay() * scenario.tower().activeFraction();
        double fuelMiningSwe = rawCoalDemand / coalPerMiningSwe;
        int population = scenario.population().standardAdults();
        double foodDemand = population * parameters.housing().foodConsumptionPerResidentDay();
        double cookedHuntingSwe = foodDemand / cookedFoodPerHuntingSwe;
        double rawHuntingSwe = foodDemand / rawFoodPerHuntingSwe;

        List<FrontierPoint> frontier = new ArrayList<>();
        for (int frontierPopulation = 1;
             frontierPopulation <= scenario.diagnostics().frontierMaximumPopulation();
             frontierPopulation++) {
            double frontierFoodDemand = frontierPopulation
                    * parameters.housing().foodConsumptionPerResidentDay();
            for (int miners = 0; miners <= frontierPopulation; miners++) {
                int hunters = frontierPopulation - miners;
                double fuelCoverage = miners * standardMiningSwe * coalPerMiningSwe
                        / Math.max(rawCoalDemand, 1.0e-12);
                double foodCoverage = hunters * standardHuntingSwe * cookedFoodPerHuntingSwe
                        / Math.max(frontierFoodDemand, 1.0e-12);
                frontier.add(new FrontierPoint(
                        frontierPopulation, miners, hunters, fuelCoverage, foodCoverage,
                        Math.min(fuelCoverage, foodCoverage),
                        fuelCoverage >= 1.0 && foodCoverage >= 1.0));
            }
        }

        Integer minimumPopulationCooked = minimumPopulation(
                fuelMiningSwe / standardMiningSwe,
                parameters.housing().foodConsumptionPerResidentDay()
                        / (standardHuntingSwe * cookedFoodPerHuntingSwe));
        Integer minimumPopulationRaw = minimumPopulation(
                fuelMiningSwe / standardMiningSwe,
                parameters.housing().foodConsumptionPerResidentDay()
                        / (standardHuntingSwe * rawFoodPerHuntingSwe));
        return new TheorySummary(
                standardMiningSwe, standardHuntingSwe,
                coalPerMiningSwe, cookedFoodPerHuntingSwe, rawFoodPerHuntingSwe,
                parameters.housing().foodConsumptionPerResidentDay(), rawCoalDemand,
                fuelMiningSwe, cookedHuntingSwe, rawHuntingSwe,
                population - fuelMiningSwe / standardMiningSwe
                        - cookedHuntingSwe / standardHuntingSwe,
                minimumPopulationCooked, minimumPopulationRaw, List.copyOf(frontier));
    }

    private static Integer minimumPopulation(double fixedFuelWorkers, double foodWorkersPerResident) {
        if (foodWorkersPerResident >= 1.0) return null;
        return (int) Math.ceil(fixedFuelWorkers / (1.0 - foodWorkersPerResident));
    }

    public record TheorySummary(
            double standardMiningSwePerMiner,
            double standardHuntingSwePerHunter,
            double coalItemsPerMiningSweDay,
            double cookedFoodUnitsPerHuntingSweDay,
            double rawFoodUnitsPerHuntingSweDay,
            double foodUnitsPerResidentDay,
            double towerRawCoalEquivalentItemsPerDay,
            double requiredMiningSwe,
            double requiredCookedHuntingSwe,
            double requiredRawHuntingSwe,
            double continuousLaborMarginResidents,
            Integer minimumCookedLoopPopulation,
            Integer minimumRawLoopPopulation,
            List<FrontierPoint> frontier
    ) {
        public TheorySummary {
            frontier = List.copyOf(frontier);
        }
    }

    public record FrontierPoint(
            int population,
            int miners,
            int hunters,
            double fuelCoverage,
            double foodCoverage,
            double jointCoverage,
            boolean feasible
    ) {
    }
}
