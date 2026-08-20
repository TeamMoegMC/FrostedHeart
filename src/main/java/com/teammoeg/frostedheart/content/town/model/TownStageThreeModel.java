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
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentDailyModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentPublicMenuModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodInventoryModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        return settleDay(state, scenario, data, parameters, random,
                new DailyEnvironment(
                        scenario.house().temperatureCelsius(), true,
                        scenario.workplaces().huntRating(), true));
    }

    public static DayResult settleDay(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            SplittableRandom random,
            DailyEnvironment environment
    ) {
        int day = state.day();
        List<ResourceFlow> flows = new ArrayList<>();
        decayNutrition(state, parameters);
        StaffingStep staffing = restoreAndFillAssignments(
                state, scenario, parameters, environment);

        double miningSwe = 0.0;
        double huntingSwe = 0.0;
        double oreRequested = 0.0;
        double coalAccepted = 0.0;
        int huntRolls = 0;
        double huntingFoodPotential = 0.0;
        double huntingFoodAccepted = 0.0;

        for (String building : scenario.buildingOrder()) {
            switch (building) {
                case TownStageThreeState.HOUSE_ID -> {
                    // Housing is settled after production and processing below.
                }
                case TownStageThreeState.MINE_ID -> {
                    MiningStep result = settleMine(state, scenario, data, parameters, flows);
                    miningSwe = result.swe();
                    oreRequested = result.oreRequested();
                    coalAccepted = result.coalAccepted();
                }
                case TownStageThreeState.HUNT_ID -> {
                    HuntingStep result = settleHunt(
                            state, scenario, data, parameters, random, environment, flows);
                    huntingSwe = result.swe();
                    huntRolls = result.rolls();
                    huntingFoodPotential = result.potentialFood();
                    huntingFoodAccepted = result.acceptedFood();
                }
                default -> throw new IllegalArgumentException("Unknown stage-3 building: " + building);
            }
        }

        ProcessingStep processing = settleProcessing(state, scenario, data, flows);
        settleDailySupplies(state, scenario, flows);
        HouseStep house = settleHouse(
                state, scenario, data, parameters, environment, flows);
        settleAging(state, parameters);
        settleResidentsMorning(state, parameters);
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
                staffing.totalTargets(), staffing.coveredTargets(),
                staffing.targetShortfall(), staffing.eligibleUnassigned(),
                staffing.unableToWork(), staffing.workplaceChanges(),
                miningSwe, huntingSwe,
                house.requiredFoodUnits(), house.consumedFoodUnits(), house.foodSatisfaction(),
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

    private static void decayNutrition(
            TownStageThreeState state,
            TownModelParameters parameters
    ) {
        TownModelParameters.ResidentNutritionParameters nutrition =
                parameters.residents().nutrition();
        for (TownStageThreeState.ResidentState resident : state.residents()) {
            resident.resetDailyActivity();
            resident.setNutrition(resident.nutrition().decay(
                    nutrition.reserveLossPerDay(), nutrition.maximumReserve()));
        }
    }

    private static void settleResidentsMorning(
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
            }
        }
        state.residents().removeAll(dead);
        state.addDeaths(dead.size());
    }

    private static void settleAging(
            TownStageThreeState state,
            TownModelParameters parameters
    ) {
        TownModelParameters.ResidentParameters residentParameters = parameters.residents();
        TownModelParameters.ResidentAgingParameters aging = residentParameters.aging();
        TownModelParameters.ResidentNutritionParameters nutrition = residentParameters.nutrition();
        for (TownStageThreeState.ResidentState resident : state.residents()) {
            int growthAge = resident.age();
            int nextDays = Math.max(0, resident.ageDays()) + 1;
            int nextAge = growthAge;
            if (growthAge == 0 && nextDays >= aging.infantToChildDays()) {
                nextAge = 1;
            } else if (growthAge == 1 && nextDays >= aging.childToAdultDays()) {
                nextAge = 2;
            }
            double baseActivity;
            double strengthRate;
            double intelligenceRate;
            double strengthCap;
            double intelligenceCap;
            double strengthAgeDecay = 0.0;
            double intelligenceAgeDecay = 0.0;
            switch (growthAge) {
                case 0 -> {
                    baseActivity = aging.infantBaseActivity();
                    strengthRate = aging.infantStrengthGainPerDay();
                    intelligenceRate = aging.infantIntelligenceGainPerDay();
                    strengthCap = aging.infantAttributeCap();
                    intelligenceCap = aging.infantAttributeCap();
                }
                case 1 -> {
                    baseActivity = aging.childBaseActivity();
                    strengthRate = aging.childStrengthGainPerDay();
                    intelligenceRate = aging.childIntelligenceGainPerDay();
                    strengthCap = aging.childStrengthCap();
                    intelligenceCap = aging.childIntelligenceCap();
                }
                case 2 -> {
                    baseActivity = aging.adultBaseActivity();
                    strengthRate = aging.adultStrengthGainPerDay();
                    intelligenceRate = aging.adultIntelligenceGainPerDay();
                    strengthCap = aging.adultAttributeCap();
                    intelligenceCap = aging.adultAttributeCap();
                }
                case 3 -> {
                    baseActivity = aging.elderBaseActivity();
                    strengthRate = aging.elderStrengthGainPerDay();
                    intelligenceRate = aging.elderIntelligenceGainPerDay();
                    strengthCap = 100.0;
                    intelligenceCap = 100.0;
                    strengthAgeDecay = aging.elderStrengthAgeDecayPerDay();
                    intelligenceAgeDecay = aging.elderIntelligenceAgeDecayPerDay();
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported simulated resident age: " + growthAge);
            }
            ResidentNutritionSupportModel.Supports support =
                    ResidentNutritionSupportModel.supports(
                            ResidentNutritionSupportModel.satisfaction(
                                    resident.nutrition(), nutrition.healthyReserve()),
                            nutrition.supportWeights());
            var activity = resident.dailyActivity();
            var strengthChange = ResidentAttributeModel.settleDailyAttribute(
                    resident.strength(), activity.physical(), baseActivity, support.strength(),
                    strengthRate, strengthCap,
                    nutrition.strengthGrowthEfficiencyAtZeroSupport(),
                    nutrition.strengthMaintenanceThreshold(), nutrition.deficiencyExponent(),
                    nutrition.strengthDecayAtZeroSupport(), strengthAgeDecay);
            var intelligenceChange = ResidentAttributeModel.settleDailyAttribute(
                    resident.intelligence(), activity.learning(), baseActivity,
                    support.intelligence(), intelligenceRate, intelligenceCap,
                    nutrition.intelligenceGrowthEfficiencyAtZeroSupport(),
                    nutrition.intelligenceMaintenanceThreshold(), nutrition.deficiencyExponent(),
                    nutrition.intelligenceDecayAtZeroSupport(), intelligenceAgeDecay);
            resident.setStrength(strengthChange.nextValue());
            resident.setIntelligence(intelligenceChange.nextValue());
            resident.setAge(nextAge);
            resident.setAgeDays(nextDays);
        }
    }

    private static StaffingStep restoreAndFillAssignments(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownModelParameters parameters,
            DailyEnvironment environment
    ) {
        int houseCapacity = houseCapacity(scenario, parameters);
        trimAssignments(state.residents(), true, TownStageThreeState.HOUSE_ID, houseCapacity);
        for (TownStageThreeState.ResidentState resident : state.residents()) {
            if (environment.houseAcceptsNewResidents()
                    && resident.homeId() == null && assignedCount(
                    state.residents(), true, TownStageThreeState.HOUSE_ID) < houseCapacity) {
                resident.setHomeId(TownStageThreeState.HOUSE_ID);
            }
        }

        Map<TownStageThreeState.ResidentState, String> previous = new java.util.IdentityHashMap<>();
        for (TownStageThreeState.ResidentState resident : state.residents()) {
            previous.put(resident, resident.workId());
        }
        List<TownAssignmentModel.Workplace<String>> workplaces = new ArrayList<>();
        for (String workplace : scenario.staffing().queue()) {
            int capacity = TownStageThreeState.MINE_ID.equals(workplace)
                    ? scenario.workplaces().mineCapacity()
                    : scenario.workplaces().huntCapacity();
            boolean workable = !TownStageThreeState.HUNT_ID.equals(workplace)
                    || environment.huntingWorkable();
            workplaces.add(new TownAssignmentModel.Workplace<>(
                    workplace, capacity, scenario.staffing().target(workplace), workable));
        }
        TownAssignmentModel.Plan<TownStageThreeState.ResidentState, String> assignmentPlan =
                TownAssignmentModel.plan(
                        state.residents(), workplaces, previous::get,
                        (workplace, resident) -> canWork(resident, parameters),
                        (workplace, resident) -> productivity(workplace, resident, parameters),
                        Comparator.comparing(TownStageThreeState.ResidentState::id));
        state.residents().forEach(resident -> resident.setWorkId(null));
        assignmentPlan.assignments().forEach(assignment ->
                assignment.resident().setWorkId(assignment.workplace()));
        int totalTargets = assignmentPlan.workplaces().values().stream()
                .mapToInt(TownAssignmentModel.WorkplaceStatus::effectiveTarget).sum();
        int coveredTargets = assignmentPlan.workplaces().values().stream()
                .mapToInt(status -> Math.min(status.assigned(), status.effectiveTarget())).sum();
        int eligible = (int) state.residents().stream()
                .filter(resident -> canWork(resident, parameters)).count();
        int eligibleUnassigned = (int) assignmentPlan.unassignedResidents().stream()
                .filter(resident -> canWork(resident, parameters)).count();
        int workplaceChanges = (int) state.residents().stream()
                .filter(resident -> !Objects.equals(previous.get(resident), resident.workId()))
                .count();
        return new StaffingStep(
                totalTargets, coveredTargets, totalTargets - coveredTargets,
                eligibleUnassigned, state.residents().size() - eligible,
                workplaceChanges);
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

    private static HouseStep settleHouse(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            DailyEnvironment environment,
            List<ResourceFlow> flows
    ) {
        List<TownStageThreeState.ResidentState> residents = state.residents().stream()
                .filter(resident -> TownStageThreeState.HOUSE_ID.equals(resident.homeId()))
                .sorted(simulatedCareOrder(parameters)).toList();
        TownModelParameters.HousingParameters housing = parameters.housing();
        TownModelParameters.ResidentNutritionParameters nutrition =
                parameters.residents().nutrition();
        double ration = Math.max(0.0, housing.foodConsumptionPerResidentDay());
        double requiredFood = residents.size() * ration;
        List<SimulationFoodCandidate> foods = new ArrayList<>();
        double availableFood = 0.0;
        for (Map.Entry<String, Double> item : state.inventorySnapshot().entrySet()) {
            TownStageOneTwoData.FoodDefinition food = data.foods().get(item.getKey());
            if (food == null) continue;
            foods.add(new SimulationFoodCandidate(
                    food.item(), food.foodLevel(), item.getValue(),
                    food.foodUnitsPerItem(), food.nutrition()));
            availableFood += item.getValue() * food.foodUnitsPerItem();
        }
        double allocatedFood = Math.min(requiredFood, availableFood);
        Map<TownStageThreeState.ResidentState, SimulationMeal> meals = new LinkedHashMap<>();
        double residentShare = residents.isEmpty() ? 0.0 : 1.0 / residents.size();
        List<ResidentPublicMenuModel.Candidate<String>> modelFoods = foods.stream()
                .map(food -> new ResidentPublicMenuModel.Candidate<>(
                        food.item, food.item, food.foodLevel, food.amountItems,
                        food.foodUnitsPerItem, food.nutrition)).toList();
        List<ResidentPublicMenuModel.Recipient> modelResidents = residents.stream()
                .map(resident -> new ResidentPublicMenuModel.Recipient(
                        resident.nutrition(), residentShare)).toList();
        ResidentPublicMenuModel.Plan<String> menuPlan = ResidentPublicMenuModel.plan(
                allocatedFood, nutrition.mealSelectionChunks(), modelFoods, modelResidents,
                new ResidentPublicMenuModel.Parameters(
                        housing.nutritionReferencePerFoodUnit(), nutrition.gainAtReference(),
                        nutrition.maximumCoverage(), nutrition.maximumReserve(),
                        nutrition.healthyReserve()));
        Map<String, Double> usedItems = new LinkedHashMap<>(menuPlan.itemAmounts());
        SimulationMeal publicMenu = new SimulationMeal(
                menuPlan.foodUnits(), menuPlan.nutrition());
        double consumedFood = publicMenu.foodUnits();
        for (TownStageThreeState.ResidentState resident : residents) {
            SimulationMeal meal = new SimulationMeal(
                    publicMenu.foodUnits() * residentShare,
                    publicMenu.nutrition().scale(residentShare));
            meals.put(resident, meal);
        }
        for (Map.Entry<String, Double> use : usedItems.entrySet()) {
            state.cost(use.getKey(), use.getValue(), ResourceActionMode.MAXIMIZE);
            flows.add(new ResourceFlow(state.day(), "house", "consume", use.getKey(),
                    use.getValue(), use.getValue(), 0.0));
        }
        HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        residents.size(), consumedFood,
                        environment.houseTemperatureCelsius(),
                        scenario.house().areaBlocks(), scenario.house().volumeBlocks(),
                        scenario.house().decorationRating()),
                TownStageOneTwoTheory.houseParameters(parameters));
        for (TownStageThreeState.ResidentState resident : residents) {
            SimulationMeal meal = meals.get(resident);
            ResidentNutrition updated = resident.nutrition().withMeal(
                    meal.nutrition(), housing.nutritionReferencePerFoodUnit(),
                    nutrition.gainAtReference(), nutrition.maximumCoverage(),
                    nutrition.maximumReserve());
            resident.setNutrition(updated);
            ResidentNutritionSupportModel.Supports support =
                    ResidentNutritionSupportModel.supports(
                            ResidentNutritionSupportModel.satisfaction(
                                    updated, nutrition.healthyReserve()),
                            nutrition.supportWeights());
            double satisfaction = ration <= 0.0 ? 1.0
                    : Math.max(0.0, Math.min(1.0, meal.foodUnits() / ration));
            HouseDailyModel.ResidentEffects effects = HouseDailyModel.calculateResidentEffects(
                    resident.health(), resident.mental(), satisfaction,
                    ResidentNutritionSupportModel.healthRecoveryMultiplier(support.health()),
                    ResidentNutritionSupportModel.mentalRecoveryMultiplier(support.mental()),
                    report.effectiveTemperature(),
                    report.temperatureRating(), report.comfortRating(),
                    TownStageOneTwoTheory.residentEffectParameters(parameters));
            resident.setHealth(resident.health() + effects.healthDelta());
            resident.setMental(resident.mental() + effects.mentalDelta());
        }
        state.addFoodDemand(requiredFood);
        double satisfaction = requiredFood <= 0.0 ? 1.0
                : Math.max(0.0, Math.min(1.0, consumedFood / requiredFood));
        if (satisfaction < 1.0 - TownFoodInventoryModel.RESOURCE_EPSILON) {
            state.markFoodShortage();
        }
        return new HouseStep(requiredFood, consumedFood, satisfaction);
    }

    private static Comparator<TownStageThreeState.ResidentState> simulatedCareOrder(
            TownModelParameters parameters
    ) {
        return Comparator
                .comparing((TownStageThreeState.ResidentState resident) ->
                        canWork(resident, parameters))
                .thenComparingDouble(TownStageThreeState.ResidentState::health)
                .thenComparingDouble(resident -> resident.nutrition().minimum())
                .thenComparingDouble(TownStageThreeState.ResidentState::mental)
                .thenComparing(TownStageThreeState.ResidentState::id);
    }

    private record SimulationMeal(
            double foodUnits,
            ResidentNutrition.NutritionIntake nutrition
    ) {
    }

    private static final class SimulationFoodCandidate {
        private final String item;
        private final int foodLevel;
        private double amountItems;
        private final double foodUnitsPerItem;
        private final ResidentNutrition.NutritionIntake nutrition;

        private SimulationFoodCandidate(
                String item,
                int foodLevel,
                double amountItems,
                double foodUnitsPerItem,
                ResidentNutrition.NutritionIntake nutrition
        ) {
            this.item = item;
            this.foodLevel = foodLevel;
            this.amountItems = Math.max(0.0, amountItems);
            this.foodUnitsPerItem = Math.max(0.0, foodUnitsPerItem);
            this.nutrition = nutrition;
        }

    }

    private static MiningStep settleMine(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            TownStageOneTwoData data,
            TownModelParameters parameters,
            List<ResourceFlow> flows
    ) {
        List<TownStageThreeState.ResidentState> workers = assignedWorkers(
                state, TownStageThreeState.MINE_ID);
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
                worker.recordActivity(parameters.mining().activity());
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
            DailyEnvironment environment,
            List<ResourceFlow> flows
    ) {
        if (!environment.huntingWorkable()) {
            return new HuntingStep(0.0, 0, 0.0, 0.0);
        }
        List<TownStageThreeState.ResidentState> workers = assignedWorkers(
                state, TownStageThreeState.HUNT_ID);
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
                worker.recordActivity(parameters.hunting().activity());
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

    private static void settleDailySupplies(
            TownStageThreeState state,
            TownStageThreeScenario scenario,
            List<ResourceFlow> flows
    ) {
        for (TownStageThreeScenario.InventoryItem supply
                : scenario.warehouse().dailySupplies()) {
            TownStageThreeState.InventoryMutation mutation = state.add(
                    supply.item(), supply.amountItems(), ResourceActionMode.MAXIMIZE);
            flows.add(new ResourceFlow(
                    state.day(), "external_supply", "produce", supply.item(),
                    supply.amountItems(), mutation.result().modifiedAmount(),
                    mutation.result().residualAmount()));
        }
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
                        0, 0.0, house.temperatureCelsius(),
                        house.areaBlocks(), house.volumeBlocks(), house.decorationRating()),
                TownStageOneTwoTheory.houseParameters(parameters));
        return HouseDailyModel.calculateCapacity(
                report.spaceRating(), house.areaBlocks(),
                parameters.housing().floorBlocksPerResident(), house.bedCount());
    }

    private static List<TownStageThreeState.ResidentState> assignedWorkers(
            TownStageThreeState state,
            String workplace
    ) {
        return state.residents().stream()
                .filter(resident -> workplace.equals(resident.workId()))
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

    private record StaffingStep(
            int totalTargets,
            int coveredTargets,
            int targetShortfall,
            int eligibleUnassigned,
            int unableToWork,
            int workplaceChanges
    ) {
    }

    /** Morning building snapshot supplied by stage 3 (constant) or stage 4 (climate/heat field). */
    public record DailyEnvironment(
            double houseTemperatureCelsius,
            boolean houseAcceptsNewResidents,
            double huntingRating,
            boolean huntingWorkable
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
            int staffingTargetWorkers,
            int staffingTargetCovered,
            int staffingTargetShortfall,
            int eligibleUnassignedWorkers,
            int unableToWorkResidents,
            int workplaceChanges,
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
