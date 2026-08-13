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

import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorFuelModel;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingDailyModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodInventoryModel;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Pure controls and measurements for the fixed 24-resident stage-4 experiment. */
public final class TownStageFourTensionModel {
    static final double DANGER_RESERVE_MINIMUM_DAYS =
            TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS;
    static final double DANGER_RESERVE_MAXIMUM_DAYS =
            TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_WARNING_DAYS;
    private static final int BUILDING_HEIGHT_BLOCKS = 3;

    private TownStageFourTensionModel() {
    }

    /**
     * True when the current player-visible forecast contains the configured
     * cold level inside the action window. The level boundary is exactly the
     * current {@code WeatherForecast} cold-bottom plus sensitivity rule.
     */
    public static boolean forecastTriggersOverdrive(
            TownStageFourModel.ClimateSeries climate,
            int day,
            int morningHour,
            TownStageFourScenario.TensionExperiment experiment,
            TownModelParameters parameters
    ) {
        int firstHour = Math.addExact(Math.multiplyExact(day, 24), morningHour);
        int availableHours = climate.lengthHours();
        int lastHour = Math.min(
                availableHours - 1,
                Math.addExact(firstHour, experiment.overdriveActionWindowHours()));
        for (int hour = firstHour; hour <= lastHour; hour += experiment.forecastSampleHours()) {
            if (atOrBelowForecastLevel(
                    climate.temperatureAtHour(hour),
                    experiment.forecastTriggerLevel(), parameters.climate())) return true;
        }
        return false;
    }

    public static boolean atOrBelowForecastLevel(
            float climateTemperatureCelsius,
            int negativeLevel,
            TownModelParameters.ClimateParameters parameters
    ) {
        float bottom = switch (negativeLevel) {
            case -1 -> parameters.coldBottomNormalCelsius();
            case -2 -> parameters.coldBottomStrongCelsius();
            case -3 -> parameters.coldBottomSevereCelsius();
            case -4 -> parameters.coldBottomExtremeCelsius();
            default -> throw new IllegalArgumentException(
                    "Forecast trigger level must be in [-4,-1].");
        };
        return climateTemperatureCelsius
                < bottom + parameters.forecastSensitivityCelsius();
    }

    /**
     * Exports only surplus food/fuel after a town tick, representing a player
     * transmitter that keeps finite operating buffers. Other mine products are
     * left untouched and still occupy the real shared warehouse.
     */
    public static ExportResult trimOperationalReserves(
            TownStageThreeState state,
            TownStageThreeScenario normalTowerScenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            double foodCapDays,
            double fuelCapNormalDays
    ) {
        double exportedFoodUnits = trimFood(
                state, data,
                Math.max(0, state.residents().size())
                        * parameters.housing().foodConsumptionPerResidentDay()
                        * foodCapDays);
        double exportedFuelItems = trimFuel(
                state, normalTowerScenario, data, parameters, fuelCapNormalDays);
        return new ExportResult(exportedFoodUnits, exportedFuelItems);
    }

    public static boolean inDangerZone(double reserveDays) {
        return reserveDays >= DANGER_RESERVE_MINIMUM_DAYS
                && reserveDays < DANGER_RESERVE_MAXIMUM_DAYS;
    }

    static TownStageFourScenario forCapacities(
            TownStageFourScenario base,
            int mineCapacity,
            int requestedHuntCapacity,
            TownStageOneTwoData data,
            TownModelParameters parameters
    ) {
        int population = base.town().population().initialResidents();
        TownStageFourScenario compact = TownStageFourPopulationSweepSimulator.forPopulation(
                base, population, data, parameters);
        Rectangle hunt = huntingCapacityRectangle(requestedHuntCapacity, parameters);
        TownStageFourScenario.ThermalBuilding house = compact.building("house");
        int houseY = house.boxes().stream()
                .mapToInt(TownStageFourScenario.VoxelBox::minimumY).min().orElse(64);
        int huntY = houseY + BUILDING_HEIGHT_BLOCKS;
        TownStageFourScenario.ThermalBuilding hunting = thermalBuilding(
                "hunt-compact-capacity-" + requestedHuntCapacity,
                "hunt", hunt, huntY);
        TownStageThreeScenario sourceTown = compact.town();
        TownStageThreeScenario town = sourceTown.withWorkplaces(
                new TownStageThreeScenario.Workplaces(
                        mineCapacity, requestedHuntCapacity,
                        sourceTown.workplaces().huntRating()));
        TownStageFourScenario candidate = new TownStageFourScenario(
                town, compact.climateBurnInDays(), compact.morningHour(), compact.location(),
                compact.towerCenter(), List.of(house, hunting), null, base.tensionExperiment());
        int physicalHuntCapacity = TownStageFourModel.huntingCapacity(candidate, parameters);
        if (physicalHuntCapacity != requestedHuntCapacity) {
            town = town.withWorkplaces(new TownStageThreeScenario.Workplaces(
                    mineCapacity, physicalHuntCapacity, town.workplaces().huntRating()));
            candidate = new TownStageFourScenario(
                    town, compact.climateBurnInDays(), compact.morningHour(), compact.location(),
                    compact.towerCenter(), List.of(house, hunting), null,
                    base.tensionExperiment());
        }
        return candidate;
    }

    private static double trimFood(
            TownStageThreeState state,
            TownStageOneTwoData data,
            double targetFoodUnits
    ) {
        double currentFoodUnits = 0.0;
        List<FoodInventory> foods = new ArrayList<>();
        for (Map.Entry<String, Double> entry : state.inventorySnapshot().entrySet()) {
            TownStageOneTwoData.FoodDefinition food = data.foods().get(entry.getKey());
            if (food == null || food.foodUnitsPerItem() <= TownFoodInventoryModel.RESOURCE_EPSILON) {
                continue;
            }
            currentFoodUnits += entry.getValue() * food.foodUnitsPerItem();
            foods.add(new FoodInventory(entry.getKey(), entry.getValue(), food));
        }
        double excess = Math.max(0.0, currentFoodUnits - targetFoodUnits);
        foods.sort(Comparator
                .comparingInt((FoodInventory value) -> value.definition().foodLevel())
                .thenComparingDouble(value -> TownFoodInventoryModel.nutritionPerFoodUnit(
                        value.definition().nutritionPerItem(),
                        value.definition().foodUnitsPerItem()))
                .thenComparing(FoodInventory::item));
        double exported = 0.0;
        for (FoodInventory food : foods) {
            if (excess <= TownFoodInventoryModel.RESOURCE_EPSILON) break;
            double items = Math.min(
                    food.amountItems(), excess / food.definition().foodUnitsPerItem());
            double removed = state.cost(
                    food.item(), items, ResourceActionMode.MAXIMIZE).result().modifiedAmount();
            double removedUnits = removed * food.definition().foodUnitsPerItem();
            excess -= removedUnits;
            exported += removedUnits;
        }
        return exported;
    }

    private static double trimFuel(
            TownStageThreeState state,
            TownStageThreeScenario normalTowerScenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            double targetNormalDays
    ) {
        TownStageThreeScenario.Tower tower = normalTowerScenario.tower();
        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        int recipeTicks = "coal".equals(tower.fuel())
                ? data.coalRecipeProcessTicks() : data.cokeRecipeProcessTicks();
        int effectiveTicks = GeneratorFuelModel.effectiveFuelProcessTicks(
                recipeTicks, generator.baseFuelDurationMultiplier(),
                tower.researchEfficiencyBonus());
        double itemsPerNormalDay = GeneratorFuelModel.idealFuelItemsPerDay(
                effectiveTicks, generator.baseProcessTicksPerGameTick(),
                generator.gameTicksPerDay()) * tower.activeFraction();
        double processBalanceItems = (double) state.towerProcessBalanceTicks() / effectiveTicks;
        double convertibleCoal = "coke".equals(tower.fuel())
                ? state.amount("minecraft:coal") : 0.0;
        double storedFuelItems = state.amount(tower.fuelItem()) + convertibleCoal;
        double targetStoredItems = Math.max(
                0.0, targetNormalDays * itemsPerNormalDay - processBalanceItems);
        double excess = Math.max(0.0, storedFuelItems - targetStoredItems);
        double exported = 0.0;
        if ("coke".equals(tower.fuel())) {
            double coal = Math.min(excess, state.amount("minecraft:coal"));
            exported += state.cost(
                    "minecraft:coal", coal, ResourceActionMode.MAXIMIZE).result().modifiedAmount();
            excess -= coal;
        }
        if (excess > TownFoodInventoryModel.RESOURCE_EPSILON) {
            exported += state.cost(
                    tower.fuelItem(), excess,
                    ResourceActionMode.MAXIMIZE).result().modifiedAmount();
        }
        return exported;
    }

    private static Rectangle huntingCapacityRectangle(
            int requestedCapacity,
            TownModelParameters parameters
    ) {
        int requiredArea = 1;
        while (huntingCapacity(requiredArea, parameters) < requestedCapacity) requiredArea++;
        int width = Math.max(1, (int) Math.floor(Math.sqrt(requiredArea)));
        int depth = (int) Math.ceil((double) requiredArea / width);
        return new Rectangle(width, depth);
    }

    private static int huntingCapacity(int area, TownModelParameters parameters) {
        TownModelParameters.SpaceRatingParameters space = parameters.buildingScoring().space();
        double rating = TownMathFunctions.calculateSpaceRating(
                Math.multiplyExact(area, BUILDING_HEIGHT_BLOCKS), area,
                space.areaCoefficient(), space.heightLogCoefficient(), space.heightLogOffset(),
                space.responseScale(), space.responseExponent());
        return HuntingDailyModel.calculateCapacity(
                rating, area, parameters.hunting().floorBlocksPerWorkerSlot(),
                parameters.hunting().minimumWorkerSlots());
    }

    private static TownStageFourScenario.ThermalBuilding thermalBuilding(
            String id,
            String role,
            Rectangle rectangle,
            int minimumY
    ) {
        TownStageFourScenario.VoxelBox box = new TownStageFourScenario.VoxelBox(
                -Math.floorDiv(rectangle.width(), 2), minimumY,
                -Math.floorDiv(rectangle.depth(), 2),
                rectangle.width(), BUILDING_HEIGHT_BLOCKS, rectangle.depth());
        return new TownStageFourScenario.ThermalBuilding(
                id, role, rectangle.area(), List.of(box));
    }

    private record FoodInventory(
            String item,
            double amountItems,
            TownStageOneTwoData.FoodDefinition definition
    ) {
    }

    private record Rectangle(int width, int depth) {
        private int area() {
            return Math.multiplyExact(width, depth);
        }
    }

    public record ExportResult(double foodUnits, double fuelItems) {
    }
}
