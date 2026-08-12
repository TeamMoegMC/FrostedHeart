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
import com.teammoeg.frostedheart.content.town.buildings.house.HouseDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.mine.MiningDailyModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentAgingModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodInventoryModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Exact day-order stage-3 transition built from gameplay-owned pure kernels. */
public final class TownStageThreeModel {
    private TownStageThreeModel() {
    }

    public static DayResult settleDay(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            SplittableRandom random
    ) {
        int day = state.day();
        List<ResourceFlow> flows = new ArrayList<>();
        settleMorningAndAging(state, parameters);
        restoreAndFillAssignments(state, scenario, parameters);

        double miningSwe = 0.0;
        double huntingSwe = 0.0;
        double foodRequired = 0.0;
        double foodConsumed = 0.0;
        double foodSatisfaction = 1.0;
        double oreRequested = 0.0;
        double coalAccepted = 0.0;
        int huntRolls = 0;
        double huntingFoodPotential = 0.0;
        double huntingFoodAccepted = 0.0;

        for (String building : scenario.buildingOrder()) {
            switch (building) {
                case TownStageThreeState.HOUSE_ID -> {
                    HouseStep result = settleHouse(state, scenario, data, parameters, flows);
                    foodRequired = result.requiredFoodUnits();
                    foodConsumed = result.consumedFoodUnits();
                    foodSatisfaction = result.foodSatisfaction();
                }
                case TownStageThreeState.MINE_ID -> {
                    MiningStep result = settleMine(state, scenario, data, parameters, flows);
                    miningSwe = result.swe();
                    oreRequested = result.oreRequested();
                    coalAccepted = result.coalAccepted();
                }
                case TownStageThreeState.HUNT_ID -> {
                    HuntingStep result = settleHunt(
                            state, scenario, data, parameters, random, flows);
                    huntingSwe = result.swe();
                    huntRolls = result.rolls();
                    huntingFoodPotential = result.potentialFood();
                    huntingFoodAccepted = result.acceptedFood();
                }
                default -> throw new IllegalArgumentException("Unknown stage-3 building: " + building);
            }
        }

        ProcessingStep processing = settleProcessing(state, scenario, data, flows);
        TowerStep tower = settleTower(state, scenario, data, parameters, flows);
        state.recordLabor(miningSwe, huntingSwe);
        state.setHuntUnits(Math.min(
                scenario.terrain().maximumHuntUnits(),
                state.huntUnits() + scenario.terrain().huntRecoveryUnitsPerDay()));

        int population = state.residents().size();
        double foodReserveDays = foodReserveDays(state, data, parameters, population);
        double fuelReserveDays = fuelReserveDays(state, scenario, data, parameters);
        double minimumHealth = state.residents().stream()
                .mapToDouble(TownStageThreeState.ResidentState::health).min().orElse(0.0);
        double minimumMental = state.residents().stream()
                .mapToDouble(TownStageThreeState.ResidentState::mental).min().orElse(0.0);
        double meanHealth = state.residents().stream()
                .mapToDouble(TownStageThreeState.ResidentState::health).average().orElse(0.0);
        double meanMental = state.residents().stream()
                .mapToDouble(TownStageThreeState.ResidentState::mental).average().orElse(0.0);
        int miners = (int) state.residents().stream()
                .filter(resident -> TownStageThreeState.MINE_ID.equals(resident.workId())).count();
        int hunters = (int) state.residents().stream()
                .filter(resident -> TownStageThreeState.HUNT_ID.equals(resident.workId())).count();
        long exhaustedChunks = MiningDailyModel.exhaustedChunks(
                state.cumulativeOreRequested(), parameters.terrainResources().oreReservePerChunk());
        long enteredChunks = MiningDailyModel.enteredChunks(
                state.cumulativeOreRequested(), parameters.terrainResources().oreReservePerChunk());

        DayResult result = new DayResult(
                day, population, state.deaths(), miners, hunters,
                miningSwe, huntingSwe,
                foodRequired, foodConsumed, foodSatisfaction,
                foodReserveDays, fuelReserveDays,
                tower.serviceFraction(), tower.loadedFuelItems(),
                oreRequested, coalAccepted, huntRolls,
                huntingFoodPotential + processing.foodGain(),
                huntingFoodAccepted + processing.foodGain(),
                processing.coalProcessed(), processing.meatProcessed(),
                state.totalInventoryItems(), state.capacityLeft(),
                minimumHealth, minimumMental, meanHealth, meanMental,
                state.huntUnits(), exhaustedChunks, enteredChunks,
                List.copyOf(flows));
        state.advanceDay();
        return result;
    }

    private static void settleMorningAndAging(
            TownStageThreeState state,
            TownModelParameters parameters
    ) {
        TownModelParameters.ResidentParameters residentParameters = parameters.residents();
        List<TownStageThreeState.ResidentState> dead = new ArrayList<>();
        for (TownStageThreeState.ResidentState resident : state.residents()) {
            ResidentDailyModel.MorningResult morning = ResidentDailyModel.settleMorning(
                    resident.health(), resident.mental(), resident.homeId() != null,
                    residentParameters.homelessHealthLossPerDay(),
                    residentParameters.removalHealthThreshold(),
                    residentParameters.removalMentalThreshold());
            resident.setHealth(morning.healthAfterHomelessPenalty());
            if (morning.removed()) {
                dead.add(resident);
                continue;
            }
            ResidentAgingModel.AgingResult aging = ResidentAgingModel.settleDay(
                    resident.age(), resident.ageDays(), resident.strength(),
                    resident.intelligence(), agingParameters(residentParameters.aging()));
            resident.setAge(aging.age());
            resident.setAgeDays(aging.ageDays());
            resident.setStrength(aging.strength());
            resident.setIntelligence(aging.intelligence());
        }
        state.residents().removeAll(dead);
        state.addDeaths(dead.size());
    }

    private static void restoreAndFillAssignments(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownModelParameters parameters
    ) {
        int houseCapacity = houseCapacity(scenario, parameters);
        trimAssignments(state.residents(), true, TownStageThreeState.HOUSE_ID, houseCapacity);
        trimAssignments(state.residents(), false, TownStageThreeState.MINE_ID,
                scenario.workplaces().mineCapacity());
        trimAssignments(state.residents(), false, TownStageThreeState.HUNT_ID,
                scenario.workplaces().huntCapacity());
        for (TownStageThreeState.ResidentState resident : state.residents()) {
            if (resident.homeId() == null && assignedCount(
                    state.residents(), true, TownStageThreeState.HOUSE_ID) < houseCapacity) {
                resident.setHomeId(TownStageThreeState.HOUSE_ID);
            }
        }

        List<TownStageThreeState.ResidentState> availableResidents = state.residents().stream()
                .filter(resident -> resident.workId() == null
                        && resident.homeId() != null
                        && resident.age() != 0)
                .toList();
        List<Workplace> workplaces = List.of(
                new Workplace(TownStageThreeState.MINE_ID, scenario.workplaces().mineCapacity()),
                new Workplace(TownStageThreeState.HUNT_ID, scenario.workplaces().huntCapacity()));
        TownAssignmentModel.fillVacancies(
                availableResidents,
                workplaces,
                Workplace::capacity,
                workplace -> assignedCount(state.residents(), false, workplace.id()),
                (workplace, count) -> assignmentPriority(workplace.id(), count, scenario, parameters),
                (workplace, resident) -> canWork(resident, parameters),
                (workplace, resident) -> productivity(workplace.id(), resident, parameters))
                .forEach(assignment -> assignment.resident().setWorkId(assignment.workplace().id()));
    }

    private static void trimAssignments(
            List<TownStageThreeState.ResidentState> residents,
            boolean home,
            String id,
            int capacity
    ) {
        int excess = assignedCount(residents, home, id) - Math.max(0, capacity);
        if (excess <= 0) return;
        for (TownStageThreeState.ResidentState resident : residents) {
            String assigned = home ? resident.homeId() : resident.workId();
            if (id.equals(assigned) && excess-- > 0) {
                if (home) resident.setHomeId(null);
                else resident.setWorkId(null);
            }
            if (excess <= 0) break;
        }
    }

    private static int assignedCount(
            List<TownStageThreeState.ResidentState> residents,
            boolean home,
            String id
    ) {
        return (int) residents.stream().filter(resident -> id.equals(
                home ? resident.homeId() : resident.workId())).count();
    }

    private static double assignmentPriority(
            String workplace,
            int count,
            TownStageThreeScenario scenario,
            TownModelParameters parameters
    ) {
        if (TownStageThreeState.MINE_ID.equals(workplace)) {
            TownModelParameters.MiningParameters mining = parameters.mining();
            return MiningDailyModel.assignmentPriority(
                    count, scenario.workplaces().mineCapacity(),
                    mining.assignmentBasePriority(), mining.assignmentPenaltyPerWorker(),
                    mining.assignmentFillRatioBonus());
        }
        TownModelParameters.HuntingParameters hunting = parameters.hunting();
        return HuntingDailyModel.assignmentPriority(
                count, scenario.workplaces().huntCapacity(), scenario.workplaces().huntRating(),
                hunting.assignmentBasePriority(), hunting.assignmentPenaltyPerWorker(),
                hunting.assignmentFillRatioBonus(), hunting.assignmentRatingMultiplier());
    }

    private static HouseStep settleHouse(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            List<ResourceFlow> flows
    ) {
        List<TownStageThreeState.ResidentState> residents = state.residents().stream()
                .filter(resident -> TownStageThreeState.HOUSE_ID.equals(resident.homeId()))
                .toList();
        double requiredFood = residents.size() * parameters.housing().foodConsumptionPerResidentDay();
        List<TownFoodInventoryModel.FoodStack> foods = new ArrayList<>();
        for (Map.Entry<String, Double> item : state.inventorySnapshot().entrySet()) {
            TownStageOneTwoData.FoodDefinition food = data.foods().get(item.getKey());
            if (food == null) continue;
            foods.add(new TownFoodInventoryModel.FoodStack(
                    food.item(), food.foodLevel(), item.getValue(),
                    food.foodUnitsPerItem(), food.nutritionPerItem()));
        }
        TownFoodInventoryModel.Consumption consumption =
                TownFoodInventoryModel.consume(requiredFood, foods);
        for (TownFoodInventoryModel.FoodUse use : consumption.uses()) {
            state.cost(use.item(), use.amountItems(), ResourceActionMode.MAXIMIZE);
            flows.add(new ResourceFlow(state.day(), "house", "consume", use.item(),
                    use.amountItems(), use.amountItems(), 0.0));
        }
        HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        residents.size(), consumption.consumedFoodUnits(),
                        consumption.consumedNutrition(), scenario.house().temperatureCelsius(),
                        scenario.house().areaBlocks(), scenario.house().volumeBlocks(),
                        scenario.house().decorationRating()),
                TownStageOneTwoTheory.houseParameters(parameters));
        for (TownStageThreeState.ResidentState resident : residents) {
            HouseDailyModel.ResidentEffects effects = HouseDailyModel.calculateResidentEffects(
                    resident.health(), resident.mental(), report.foodSatisfaction(),
                    report.nutritionRecoveryMultiplier(), report.effectiveTemperature(),
                    report.temperatureRating(), report.comfortRating(),
                    TownStageOneTwoTheory.residentEffectParameters(parameters));
            resident.setHealth(resident.health() + effects.healthDelta());
            resident.setMental(resident.mental() + effects.mentalDelta());
        }
        state.addFoodDemand(requiredFood);
        if (report.foodSatisfaction() < 1.0 - TownFoodInventoryModel.RESOURCE_EPSILON) {
            state.markFoodShortage();
        }
        return new HouseStep(requiredFood, consumption.consumedFoodUnits(), report.foodSatisfaction());
    }

    private static MiningStep settleMine(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            List<ResourceFlow> flows
    ) {
        List<TownStageThreeState.ResidentState> workers = eligibleWorkers(
                state, TownStageThreeState.MINE_ID, parameters);
        double swe = workers.stream().mapToDouble(
                resident -> productivity(TownStageThreeState.MINE_ID, resident, parameters)).sum();
        double requested = MiningDailyModel.requestedOutput(
                swe, parameters.mining().baseOutputPerStandardWorkerDay());
        double totalWeight = TownStageOneTwoTheory.positiveWeightSum(data.mineWeights());
        double acceptedOre = 0.0;
        double requestedCoal = 0.0;
        double acceptedCoal = 0.0;
        List<TownStageZeroModel.WeightedResource> resources = data.mineWeights().stream()
                .sorted(Comparator.comparing(TownStageZeroModel.WeightedResource::item)).toList();
        for (TownStageZeroModel.WeightedResource resource : resources) {
            double amount = MiningDailyModel.weightedShare(requested, resource.weight(), totalWeight);
            TownStageThreeState.InventoryMutation mutation = state.add(
                    resource.item(), amount, ResourceActionMode.ATTEMPT);
            double accepted = mutation.result().modifiedAmount();
            acceptedOre += accepted;
            if ("minecraft:coal".equals(resource.item())) {
                requestedCoal += amount;
                acceptedCoal += accepted;
            }
            flows.add(new ResourceFlow(state.day(), "mine", "produce", resource.item(),
                    amount, accepted, mutation.result().residualAmount()));
        }
        if (requested > TownFoodInventoryModel.RESOURCE_EPSILON) {
            for (TownStageThreeState.ResidentState worker : workers) {
                worker.setMiningProficiency(growProficiency(worker.miningProficiency(), parameters));
            }
        }
        state.recordMining(requested, acceptedOre, requestedCoal, acceptedCoal);
        return new MiningStep(swe, requested, acceptedOre, requestedCoal, acceptedCoal);
    }

    private static HuntingStep settleHunt(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            SplittableRandom random,
            List<ResourceFlow> flows
    ) {
        List<TownStageThreeState.ResidentState> workers = eligibleWorkers(
                state, TownStageThreeState.HUNT_ID, parameters);
        double swe = workers.stream().mapToDouble(
                resident -> productivity(TownStageThreeState.HUNT_ID, resident, parameters)).sum();
        TownModelParameters.HuntingParameters hunting = parameters.hunting();
        HuntingDailyModel.RollPlan plan = HuntingDailyModel.planRolls(
                swe, hunting.expectedLootRollsPerStandardWorkerDay(),
                hunting.passiveExpectedLootRollsPerBaseDay(), state.huntRollCarry(),
                hunting.useFractionalLootRollCarry(), state.huntUnits());
        state.setHuntRollCarry(plan.nextCarry());
        state.setHuntUnits(state.huntUnits() - plan.executedRolls());

        Map<String, Integer> loot = new LinkedHashMap<>();
        for (int roll = 0; roll < plan.executedRolls(); roll++) {
            TownStageOneTwoTheory.LootSample sample =
                    TownStageOneTwoTheory.sampleLoot(data.huntingLoot(), random);
            if (sample.count() > 0) loot.merge(sample.item(), sample.count(), Integer::sum);
        }
        double potentialFood = 0.0;
        double acceptedFood = 0.0;
        List<Map.Entry<String, Integer>> ordered = loot.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();
        for (Map.Entry<String, Integer> entry : ordered) {
            TownStageThreeState.InventoryMutation mutation = state.add(
                    entry.getKey(), entry.getValue(), ResourceActionMode.MAXIMIZE);
            double accepted = mutation.result().modifiedAmount();
            TownStageOneTwoData.FoodDefinition food = data.foods().get(entry.getKey());
            if (food != null) {
                potentialFood += entry.getValue() * food.foodUnitsPerItem();
                acceptedFood += accepted * food.foodUnitsPerItem();
            }
            flows.add(new ResourceFlow(state.day(), "hunt", "produce", entry.getKey(),
                    entry.getValue(), accepted, mutation.result().residualAmount()));
        }
        state.recordHuntingFood(potentialFood, acceptedFood);
        if (plan.hasWorkerOpportunity()) {
            for (TownStageThreeState.ResidentState worker : workers) {
                worker.setHuntingProficiency(growProficiency(worker.huntingProficiency(), parameters));
            }
        }
        return new HuntingStep(swe, plan.executedRolls(), potentialFood, acceptedFood);
    }

    private static ProcessingStep settleProcessing(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            List<ResourceFlow> flows
    ) {
        double coalProcessed = Math.min(
                state.amount("minecraft:coal"), scenario.processing().coalToCokeItemsPerDay());
        if (coalProcessed > TownFoodInventoryModel.RESOURCE_EPSILON) {
            state.cost("minecraft:coal", coalProcessed, ResourceActionMode.ATTEMPT);
            state.add(scenario.processing().cokeItem(), coalProcessed, ResourceActionMode.ATTEMPT);
            flows.add(new ResourceFlow(state.day(), "processing", "consume",
                    "minecraft:coal", coalProcessed, coalProcessed, 0.0));
            flows.add(new ResourceFlow(state.day(), "processing", "produce",
                    scenario.processing().cokeItem(),
                    coalProcessed, coalProcessed, 0.0));
        }

        Map<String, Double> raw = new HashMap<>();
        for (TownFoodProcessingModel.MeatDefinition meat : data.meats()) {
            raw.put(meat.rawItem(), state.amount(meat.rawItem()));
        }
        TownFoodProcessingModel.ProcessingResult processing = TownFoodProcessingModel.process(
                raw, scenario.processing().rawMeatItemsPerDay(), data.meats());
        double foodGain = 0.0;
        for (TownFoodProcessingModel.MeatDefinition meat : data.meats()) {
            double before = raw.getOrDefault(meat.rawItem(), 0.0);
            double after = processing.remainingRaw().getOrDefault(meat.rawItem(), 0.0);
            double converted = Math.max(0.0, before - after);
            if (converted <= TownFoodInventoryModel.RESOURCE_EPSILON) continue;
            state.cost(meat.rawItem(), converted, ResourceActionMode.ATTEMPT);
            state.add(meat.cookedItem(), converted, ResourceActionMode.ATTEMPT);
            foodGain += converted * meat.foodGainPerItem();
            flows.add(new ResourceFlow(state.day(), "processing", "consume",
                    meat.rawItem(), converted, converted, 0.0));
            flows.add(new ResourceFlow(state.day(), "processing", "produce",
                    meat.cookedItem(), converted, converted, 0.0));
        }
        state.recordProcessingFoodGain(foodGain);
        return new ProcessingStep(coalProcessed, processing.processedItems(), foodGain);
    }

    private static TowerStep settleTower(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            List<ResourceFlow> flows
    ) {
        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        int recipeTicks = "coal".equals(scenario.tower().fuel())
                ? data.coalRecipeProcessTicks() : data.cokeRecipeProcessTicks();
        String item = scenario.tower().fuelItem();
        int effectiveTicks = GeneratorFuelModel.effectiveFuelProcessTicks(
                recipeTicks, generator.baseFuelDurationMultiplier(),
                scenario.tower().researchEfficiencyBonus());
        int processTicksPerGameTick = generator.baseProcessTicksPerGameTick()
                + (scenario.tower().overdrive()
                ? generator.overdriveExtraProcessTicksPerGameTick() : 0);
        int requestedPerBatch = Math.multiplyExact(
                processTicksPerGameTick, generator.townBatchGameTicks());
        long requested = Math.round(
                (double) processTicksPerGameTick * generator.gameTicksPerDay()
                        * scenario.tower().activeFraction());
        long availableItems = (long) Math.floor(state.amount(item)
                + TownFoodInventoryModel.RESOURCE_EPSILON);
        GeneratorFuelModel.FiniteFuelSettlement settlement =
                GeneratorFuelModel.settleFiniteProcessDemand(
                        effectiveTicks, requestedPerBatch, requested,
                        state.towerProcessBalanceTicks(), availableItems);
        state.cost(item, settlement.loadedFuelItems(), ResourceActionMode.ATTEMPT);
        state.setTowerProcessBalanceTicks(settlement.remainingProcessTicks());
        double rawCoalDemand = (double) requested / effectiveTicks;
        state.addRawCoalDemand(rawCoalDemand);
        if (settlement.serviceFraction() < 1.0 - TownFoodInventoryModel.RESOURCE_EPSILON) {
            state.markFuelShortage();
        }
        flows.add(new ResourceFlow(state.day(), "tower", "consume", item,
                settlement.loadedFuelItems(), settlement.loadedFuelItems(), 0.0));
        flows.add(new ResourceFlow(state.day(), "tower", "service", "process_ticks",
                requested, settlement.consumedProcessTicks(),
                requested - settlement.consumedProcessTicks()));
        return new TowerStep(
                settlement.serviceFraction(), settlement.loadedFuelItems(),
                settlement.consumedProcessTicks(), requested);
    }

    public static double foodReserveDays(
            TownStageThreeState state,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            int population
    ) {
        if (population <= 0) return 0.0;
        double food = 0.0;
        for (Map.Entry<String, Double> entry : state.inventorySnapshot().entrySet()) {
            TownStageOneTwoData.FoodDefinition definition = data.foods().get(entry.getKey());
            if (definition != null) food += entry.getValue() * definition.foodUnitsPerItem();
        }
        return food / (population * parameters.housing().foodConsumptionPerResidentDay());
    }

    public static double fuelReserveDays(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters
    ) {
        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        int recipeTicks = "coal".equals(scenario.tower().fuel())
                ? data.coalRecipeProcessTicks() : data.cokeRecipeProcessTicks();
        int effectiveTicks = GeneratorFuelModel.effectiveFuelProcessTicks(
                recipeTicks, generator.baseFuelDurationMultiplier(),
                scenario.tower().researchEfficiencyBonus());
        int processTicksPerGameTick = generator.baseProcessTicksPerGameTick()
                + (scenario.tower().overdrive()
                ? generator.overdriveExtraProcessTicksPerGameTick() : 0);
        double itemsPerDay = GeneratorFuelModel.idealFuelItemsPerDay(
                effectiveTicks, processTicksPerGameTick, generator.gameTicksPerDay())
                * scenario.tower().activeFraction();
        if (itemsPerDay <= 0.0) return Double.POSITIVE_INFINITY;
        String item = scenario.tower().fuelItem();
        double convertibleCoal = "coke".equals(scenario.tower().fuel())
                ? state.amount("minecraft:coal") : 0.0;
        double processBalanceItems = (double) state.towerProcessBalanceTicks() / effectiveTicks;
        return (state.amount(item) + convertibleCoal + processBalanceItems) / itemsPerDay;
    }

    public static int houseCapacity(
            TownStageThreeScenario scenario,
            TownModelParameters parameters
    ) {
        TownStageThreeScenario.House house = scenario.house();
        HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        0, 0.0, 0.0, house.temperatureCelsius(),
                        house.areaBlocks(), house.volumeBlocks(), house.decorationRating()),
                TownStageOneTwoTheory.houseParameters(parameters));
        return HouseDailyModel.calculateCapacity(
                report.spaceRating(), house.areaBlocks(),
                parameters.housing().floorBlocksPerResident(), house.bedCount());
    }

    private static List<TownStageThreeState.ResidentState> eligibleWorkers(
            TownStageThreeState state,
            String workplace,
            TownModelParameters parameters
    ) {
        return state.residents().stream()
                .filter(resident -> workplace.equals(resident.workId()))
                .filter(resident -> canWork(resident, parameters))
                .toList();
    }

    private static boolean canWork(
            TownStageThreeState.ResidentState resident,
            TownModelParameters parameters
    ) {
        TownModelParameters.ResidentParameters rules = parameters.residents();
        return ResidentDailyModel.canWork(
                resident.age(), resident.health(), resident.mental(), resident.homeId() != null,
                rules.minimumWorkingAge(), rules.minimumWorkingHealthExclusive(),
                rules.minimumWorkingMentalExclusive(), rules.workRequiresHousing());
    }

    private static double productivity(
            String workplace,
            TownStageThreeState.ResidentState resident,
            TownModelParameters parameters
    ) {
        if (TownStageThreeState.MINE_ID.equals(workplace)) {
            return parameters.mining().productivity().productivity(
                    resident.health(), resident.mental(), resident.strength(),
                    resident.intelligence(), resident.miningProficiency());
        }
        return parameters.hunting().productivity().productivity(
                resident.health(), resident.mental(), resident.strength(),
                resident.intelligence(), resident.huntingProficiency());
    }

    private static double growProficiency(double proficiency, TownModelParameters parameters) {
        TownModelParameters.ResidentParameters resident = parameters.residents();
        return proficiency + ResidentAttributeModel.calculateDailyProficiencyGain(
                proficiency, resident.proficiencyGrowthAtZeroPerWorkday(),
                resident.minimumProficiencyGrowthPerWorkday(),
                resident.maximumWorkProficiency());
    }

    private static ResidentAgingModel.Parameters agingParameters(
            TownModelParameters.ResidentAgingParameters aging
    ) {
        return new ResidentAgingModel.Parameters(
                aging.infantToChildDays(), aging.childToAdultDays(),
                aging.infantStrengthGainPerDay(), aging.infantIntelligenceGainPerDay(),
                aging.infantAttributeCap(), aging.childStrengthGainPerDay(),
                aging.childIntelligenceGainPerDay(), aging.childStrengthCap(),
                aging.childIntelligenceCap(), aging.adultStrengthGainPerDay(),
                aging.adultIntelligenceGainPerDay(), aging.adultAttributeCap(),
                aging.elderStrengthDecayPerDay(), aging.elderStrengthFloor());
    }

    private record Workplace(String id, int capacity) {
    }

    private record HouseStep(double requiredFoodUnits, double consumedFoodUnits, double foodSatisfaction) {
    }

    private record MiningStep(
            double swe,
            double oreRequested,
            double oreAccepted,
            double coalRequested,
            double coalAccepted
    ) {
    }

    private record HuntingStep(double swe, int rolls, double potentialFood, double acceptedFood) {
    }

    private record ProcessingStep(double coalProcessed, double meatProcessed, double foodGain) {
    }

    private record TowerStep(
            double serviceFraction,
            long loadedFuelItems,
            long consumedProcessTicks,
            long requestedProcessTicks
    ) {
    }

    public record ResourceFlow(
            int day,
            String source,
            String action,
            String resource,
            double requested,
            double modified,
            double rejected
    ) {
    }

    public record DayResult(
            int day,
            int population,
            int cumulativeDeaths,
            int assignedMiners,
            int assignedHunters,
            double miningSwe,
            double huntingSwe,
            double foodRequired,
            double foodConsumed,
            double foodSatisfaction,
            double foodReserveDays,
            double fuelReserveDays,
            double towerServiceFraction,
            long loadedFuelItems,
            double oreRequested,
            double coalAccepted,
            int huntingRolls,
            double huntingFoodPotential,
            double huntingFoodAccepted,
            double coalProcessed,
            double meatProcessed,
            double inventoryItems,
            double capacityLeft,
            double minimumHealth,
            double minimumMental,
            double meanHealth,
            double meanMental,
            double huntUnits,
            long exhaustedOreChunks,
            long enteredOreChunks,
            List<ResourceFlow> resourceFlows
    ) {
        public DayResult {
            resourceFlows = List.copyOf(resourceFlows);
        }
    }
}
