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

import com.teammoeg.frostedheart.content.town.resource.TownFoodInventoryModel;
import com.teammoeg.frostedheart.content.town.resource.TownInventoryModel;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resident.ResidentGenerationModel;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Mutable, serializable-in-principle state for one stage-3 Monte Carlo run. */
public final class TownStageThreeState {
    public static final String HOUSE_ID = "house";
    public static final String MINE_ID = "mine";
    public static final String HUNT_ID = "hunt";

    private int day;
    private final List<ResidentState> residents;
    private final LinkedHashMap<String, Double> inventory;
    private final double warehouseCapacityItems;
    private double huntRollCarry;
    private double huntUnits;
    private long towerProcessBalanceTicks;
    private int deaths;
    private Integer firstFoodShortageDay;
    private Integer firstFuelShortageDay;
    private double cumulativeOreRequested;
    private double cumulativeOreAccepted;
    private double cumulativeCoalRequested;
    private double cumulativeCoalAccepted;
    private double cumulativeHuntingFoodPotential;
    private double cumulativeHuntingFoodAccepted;
    private double cumulativeFoodDemand;
    private double cumulativeRawCoalDemand;
    private double cumulativeRejectedItems;
    private double cumulativeMiningSweDays;
    private double cumulativeHuntingSweDays;

    private TownStageThreeState(
            List<ResidentState> residents,
            LinkedHashMap<String, Double> inventory,
            double warehouseCapacityItems,
            double huntUnits
    ) {
        this.residents = residents;
        this.inventory = inventory;
        this.warehouseCapacityItems = warehouseCapacityItems;
        this.huntUnits = huntUnits;
    }

    public static TownStageThreeState initial(TownStageThreeScenario scenario) {
        return initial(scenario, TownModelParameters.currentDefaults(),
                new SplittableRandom(scenario.simulation().seed()));
    }

    public static TownStageThreeState initial(
            TownStageThreeScenario scenario,
            TownModelParameters parameters,
            SplittableRandom random
    ) {
        TownStageThreeScenario.Population population = scenario.population();
        double initialNutrition = Math.max(0.0, Math.min(
                parameters.residents().nutrition().maximumReserve(),
                parameters.residents().nutrition().initialReserve()));
        ResidentNutrition nutrition = new ResidentNutrition(
                initialNutrition, initialNutrition, initialNutrition, initialNutrition);
        List<ResidentState> residents = new ArrayList<>(population.initialResidents());
        for (int index = 0; index < population.initialResidents(); index++) {
            if (population.initialization()
                    == TownStageThreeScenario.PopulationInitialization.GAME_GENERATED) {
                ResidentGenerationModel.GeneratedResident generated =
                        ResidentGenerationModel.generate(
                                random::nextDouble, random::nextInt,
                                generationParameters(parameters.residents()));
                residents.add(new ResidentState(
                        String.format("resident-%03d", index),
                        generated.health(), generated.mental(), generated.strength(),
                        generated.intelligence(), generated.miningProficiency(),
                        generated.huntingProficiency(), generated.age(), generated.ageDays(),
                        HOUSE_ID, null, nutrition));
            } else {
                residents.add(new ResidentState(
                        String.format("resident-%03d", index),
                        population.initialHealth(), population.initialMental(),
                        population.initialStrength(), population.initialIntelligence(),
                        population.initialMiningProficiency(),
                        population.initialHuntingProficiency(),
                        2, population.initialAgeDays(), HOUSE_ID, null, nutrition));
            }
        }
        LinkedHashMap<String, Double> inventory = new LinkedHashMap<>();
        for (TownStageThreeScenario.InventoryItem item : scenario.warehouse().initialInventory()) {
            inventory.merge(item.item(), item.amountItems(), Double::sum);
        }
        return new TownStageThreeState(
                residents, inventory, scenario.warehouse().capacityItems(),
                scenario.terrain().initialHuntUnits());
    }

    private static ResidentGenerationModel.Parameters generationParameters(
            TownModelParameters.ResidentParameters residents
    ) {
        TownModelParameters.ResidentGenerationParameters generation = residents.generation();
        TownModelParameters.ResidentAgingParameters aging = residents.aging();
        return new ResidentGenerationModel.Parameters(
                generation.initialHealth(), generation.initialMental(),
                generation.attributeSampleCount(),
                new ResidentGenerationModel.AttributeCenters(
                        generation.infantStrengthCenter(), generation.infantIntelligenceCenter()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.childStrengthCenter(), generation.childIntelligenceCenter()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.adultStrengthCenter(), generation.adultIntelligenceCenter()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.elderStrengthCenter(), generation.elderIntelligenceCenter()),
                generation.nonAdultAttributeSpread(), generation.adultAttributeSpread(),
                generation.infantInitialProficiency(),
                generation.childMaximumInitialProficiency(),
                generation.adultMaximumInitialProficiency(),
                generation.elderMinimumInitialProficiency(),
                generation.elderMaximumInitialProficiency(),
                aging.infantToChildDays(), aging.childToAdultDays(),
                generation.adultAgeRangeDaysExclusive(),
                ageWeights(generation.ageWeights()), ageWeights(generation.fallbackAgeWeights()));
    }

    private static ResidentGenerationModel.AgeWeights ageWeights(
            TownModelParameters.ResidentAgeWeightParameters weights
    ) {
        return new ResidentGenerationModel.AgeWeights(
                weights.infant(), weights.child(), weights.adult(), weights.elder());
    }

    public InventoryMutation add(String item, double requested, ResourceActionMode mode) {
        TownInventoryModel.Mutation mutation = TownInventoryModel.settle(
                requested, capacityLeft(), mode);
        if (mutation.modifiedAmount() > TownFoodInventoryModel.RESOURCE_EPSILON) {
            inventory.merge(item, mutation.modifiedAmount(), Double::sum);
        }
        cumulativeRejectedItems += mutation.residualAmount();
        return new InventoryMutation(item, true, mode, mutation);
    }

    public InventoryMutation cost(String item, double requested, ResourceActionMode mode) {
        TownInventoryModel.Mutation mutation = TownInventoryModel.settle(
                requested, amount(item), mode);
        if (mutation.modifiedAmount() > TownFoodInventoryModel.RESOURCE_EPSILON) {
            double remaining = amount(item) - mutation.modifiedAmount();
            if (remaining <= TownFoodInventoryModel.RESOURCE_EPSILON) inventory.remove(item);
            else inventory.put(item, remaining);
        }
        return new InventoryMutation(item, false, mode, mutation);
    }

    public double amount(String item) {
        return inventory.getOrDefault(item, 0.0);
    }

    public double totalInventoryItems() {
        return inventory.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double capacityLeft() {
        return Math.max(0.0, warehouseCapacityItems - totalInventoryItems());
    }

    public Map<String, Double> inventorySnapshot() {
        return Map.copyOf(inventory);
    }

    public List<ResidentState> residents() {
        return residents;
    }

    public int day() {
        return day;
    }

    public void advanceDay() {
        day++;
    }

    public double huntRollCarry() {
        return huntRollCarry;
    }

    public void setHuntRollCarry(double huntRollCarry) {
        this.huntRollCarry = huntRollCarry;
    }

    public double huntUnits() {
        return huntUnits;
    }

    public void setHuntUnits(double huntUnits) {
        this.huntUnits = Math.max(0.0, huntUnits);
    }

    public long towerProcessBalanceTicks() {
        return towerProcessBalanceTicks;
    }

    public void setTowerProcessBalanceTicks(long towerProcessBalanceTicks) {
        this.towerProcessBalanceTicks = Math.max(0L, towerProcessBalanceTicks);
    }

    public int deaths() {
        return deaths;
    }

    public void addDeaths(int count) {
        deaths += Math.max(0, count);
    }

    public Integer firstFoodShortageDay() {
        return firstFoodShortageDay;
    }

    public void markFoodShortage() {
        if (firstFoodShortageDay == null) firstFoodShortageDay = day;
    }

    public Integer firstFuelShortageDay() {
        return firstFuelShortageDay;
    }

    public void markFuelShortage() {
        if (firstFuelShortageDay == null) firstFuelShortageDay = day;
    }

    public double cumulativeOreRequested() {
        return cumulativeOreRequested;
    }

    public double cumulativeOreAccepted() {
        return cumulativeOreAccepted;
    }

    public double cumulativeCoalRequested() {
        return cumulativeCoalRequested;
    }

    public double cumulativeCoalAccepted() {
        return cumulativeCoalAccepted;
    }

    public void recordMining(double oreRequested, double oreAccepted, double coalRequested, double coalAccepted) {
        cumulativeOreRequested += oreRequested;
        cumulativeOreAccepted += oreAccepted;
        cumulativeCoalRequested += coalRequested;
        cumulativeCoalAccepted += coalAccepted;
    }

    public double cumulativeHuntingFoodPotential() {
        return cumulativeHuntingFoodPotential;
    }

    public double cumulativeHuntingFoodAccepted() {
        return cumulativeHuntingFoodAccepted;
    }

    public void recordHuntingFood(double potential, double accepted) {
        cumulativeHuntingFoodPotential += potential;
        cumulativeHuntingFoodAccepted += accepted;
    }

    public void recordProcessingFoodGain(double acceptedGain) {
        cumulativeHuntingFoodAccepted += Math.max(0.0, acceptedGain);
        cumulativeHuntingFoodPotential += Math.max(0.0, acceptedGain);
    }

    public double cumulativeFoodDemand() {
        return cumulativeFoodDemand;
    }

    public void addFoodDemand(double demand) {
        cumulativeFoodDemand += Math.max(0.0, demand);
    }

    public double cumulativeRawCoalDemand() {
        return cumulativeRawCoalDemand;
    }

    public void addRawCoalDemand(double demand) {
        cumulativeRawCoalDemand += Math.max(0.0, demand);
    }

    public double cumulativeRejectedItems() {
        return cumulativeRejectedItems;
    }

    public void recordLabor(double miningSwe, double huntingSwe) {
        cumulativeMiningSweDays += Math.max(0.0, miningSwe);
        cumulativeHuntingSweDays += Math.max(0.0, huntingSwe);
    }

    public double cumulativeMiningSweDays() {
        return cumulativeMiningSweDays;
    }

    public double cumulativeHuntingSweDays() {
        return cumulativeHuntingSweDays;
    }

    public record InventoryMutation(
            String item,
            boolean add,
            ResourceActionMode mode,
            TownInventoryModel.Mutation result
    ) {
    }

    public static final class ResidentState {
        private final String id;
        private double health;
        private double mental;
        private double strength;
        private double intelligence;
        private double miningProficiency;
        private double huntingProficiency;
        private int age;
        private int ageDays;
        private String homeId;
        private String workId;
        private ResidentNutrition nutrition;

        ResidentState(
                String id,
                double health,
                double mental,
                double strength,
                double intelligence,
                double miningProficiency,
                double huntingProficiency,
                int age,
                int ageDays,
                String homeId,
                String workId,
                ResidentNutrition nutrition
        ) {
            this.id = id;
            this.health = health;
            this.mental = mental;
            this.strength = strength;
            this.intelligence = intelligence;
            this.miningProficiency = miningProficiency;
            this.huntingProficiency = huntingProficiency;
            this.age = age;
            this.ageDays = ageDays;
            this.homeId = homeId;
            this.workId = workId;
            this.nutrition = nutrition;
        }

        public String id() { return id; }
        public double health() { return health; }
        public double mental() { return mental; }
        public double strength() { return strength; }
        public double intelligence() { return intelligence; }
        public double miningProficiency() { return miningProficiency; }
        public double huntingProficiency() { return huntingProficiency; }
        public int age() { return age; }
        public int ageDays() { return ageDays; }
        public String homeId() { return homeId; }
        public String workId() { return workId; }
        public ResidentNutrition nutrition() { return nutrition; }

        public void setHealth(double value) { health = bounded(value); }
        public void setMental(double value) { mental = bounded(value); }
        public void setStrength(double value) { strength = bounded(value); }
        public void setIntelligence(double value) { intelligence = bounded(value); }
        public void setMiningProficiency(double value) { miningProficiency = bounded(value); }
        public void setHuntingProficiency(double value) { huntingProficiency = bounded(value); }
        public void setAge(int value) { age = value; }
        public void setAgeDays(int value) { ageDays = Math.max(0, value); }
        public void setHomeId(String value) { homeId = value; }
        public void setWorkId(String value) { workId = value; }
        public void setNutrition(ResidentNutrition value) {
            nutrition = value == null ? ResidentNutrition.DEFAULT_VALUE : value;
        }

        private static double bounded(double value) {
            if (!Double.isFinite(value)) return 0.0;
            return Math.max(0.0, Math.min(100.0, value));
        }
    }
}
