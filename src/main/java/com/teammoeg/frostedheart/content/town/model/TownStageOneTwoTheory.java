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
import com.teammoeg.frostedheart.content.town.buildings.mine.MiningDailyModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Closed-form and exactly enumerated baselines for stage-1/2 kernels. */
public final class TownStageOneTwoTheory {
    private TownStageOneTwoTheory() {
    }

    public static double totalSwe(
            List<TownStageOneTwoScenario.Worker> workers,
            TownModelParameters.ResidentProductivityParameters productivity
    ) {
        return workers.stream().mapToDouble(worker -> productivity.productivity(
                worker.health(), worker.mental(), worker.strength(),
                worker.intelligence(), worker.proficiency())).sum();
    }

    public static double positiveWeightSum(List<TownStageZeroModel.WeightedResource> values) {
        return values.stream().mapToDouble(TownStageZeroModel.WeightedResource::weight)
                .filter(value -> value > 0.0).sum();
    }

    public static double itemWeightFraction(
            List<TownStageZeroModel.WeightedResource> values,
            String item
    ) {
        double total = positiveWeightSum(values);
        double selected = values.stream().filter(value -> item.equals(value.item()))
                .mapToDouble(TownStageZeroModel.WeightedResource::weight)
                .filter(value -> value > 0.0).sum();
        return total > 0.0 ? selected / total : 0.0;
    }

    public static MiningTheory mining(
            double totalMiningSwe,
            double baseOutputPerSweDay,
            List<TownStageZeroModel.WeightedResource> weights,
            double oreReservePerChunk
    ) {
        double ore = MiningDailyModel.requestedOutput(totalMiningSwe, baseOutputPerSweDay);
        double coalFraction = itemWeightFraction(weights, "minecraft:coal");
        double coal = ore * coalFraction;
        return new MiningTheory(
                totalMiningSwe,
                ore,
                coalFraction,
                coal,
                totalMiningSwe > 0.0 ? coal / totalMiningSwe : 0.0,
                MiningDailyModel.exhaustedChunks(ore, oreReservePerChunk),
                MiningDailyModel.enteredChunks(ore, oreReservePerChunk));
    }

    /** Exact mixture moment for one current hunting-loot-table roll. */
    public static Moment lootMoment(
            List<TownStageZeroModel.WeightedLootEntry> entries,
            List<TownFoodProcessingModel.MeatDefinition> meats,
            double processingCapacityItems
    ) {
        Map<String, TownFoodProcessingModel.MeatDefinition> meatByRaw = new HashMap<>();
        meats.forEach(meat -> meatByRaw.put(meat.rawItem(), meat));
        double totalWeight = entries.stream().mapToDouble(TownStageZeroModel.WeightedLootEntry::weight)
                .filter(value -> value > 0.0).sum();
        double mean = 0.0;
        double second = 0.0;
        double meatMean = 0.0;
        double meatSecond = 0.0;
        for (TownStageZeroModel.WeightedLootEntry entry : entries) {
            if (entry.weight() <= 0.0) continue;
            int minimum = (int) Math.rint(entry.minimumCount());
            int maximum = (int) Math.rint(entry.maximumCount());
            int outcomeCount = maximum - minimum + 1;
            TownFoodProcessingModel.MeatDefinition meat = meatByRaw.get(entry.item());
            for (int count = minimum; count <= maximum; count++) {
                double probability = entry.weight() / totalWeight / outcomeCount;
                double food = 0.0;
                double meatItems = 0.0;
                if (meat != null) {
                    meatItems = count;
                    double cooked = Double.isInfinite(processingCapacityItems)
                            ? count : Math.min(count, Math.max(0.0, processingCapacityItems));
                    food = cooked * meat.cookedFoodUnitsPerItem()
                            + (count - cooked) * meat.rawFoodUnitsPerItem();
                }
                mean += probability * food;
                second += probability * food * food;
                meatMean += probability * meatItems;
                meatSecond += probability * meatItems * meatItems;
            }
        }
        return new Moment(
                mean,
                Math.max(0.0, second - mean * mean),
                meatMean,
                Math.max(0.0, meatSecond - meatMean * meatMean));
    }

    public static LootSample sampleLoot(
            List<TownStageZeroModel.WeightedLootEntry> entries,
            SplittableRandom random
    ) {
        double totalWeight = entries.stream().mapToDouble(TownStageZeroModel.WeightedLootEntry::weight)
                .filter(value -> value > 0.0).sum();
        if (totalWeight <= 0.0) throw new IllegalArgumentException("Hunting loot has no positive weight.");
        double target = random.nextDouble(totalWeight);
        TownStageZeroModel.WeightedLootEntry selected = null;
        double cumulative = 0.0;
        for (TownStageZeroModel.WeightedLootEntry entry : entries) {
            if (entry.weight() <= 0.0) continue;
            cumulative += entry.weight();
            if (target < cumulative) {
                selected = entry;
                break;
            }
        }
        if (selected == null) selected = entries.get(entries.size() - 1);
        int minimum = (int) Math.rint(selected.minimumCount());
        int maximum = (int) Math.rint(selected.maximumCount());
        int count = minimum == maximum ? minimum : random.nextInt(minimum, maximum + 1);
        return new LootSample(selected.item(), count);
    }

    public static TowerFuelTheory towerFuel(
            int recipeProcessTicks,
            TownModelParameters.GeneratorT1Parameters generator,
            boolean overdrive,
            double researchEfficiencyBonus
    ) {
        int effectiveTicks = GeneratorFuelModel.effectiveFuelProcessTicks(
                recipeProcessTicks,
                generator.baseFuelDurationMultiplier(),
                researchEfficiencyBonus);
        int processTicksPerGameTick = generator.baseProcessTicksPerGameTick()
                + (overdrive ? generator.overdriveExtraProcessTicksPerGameTick() : 0);
        double itemsPerActiveDay = GeneratorFuelModel.idealFuelItemsPerDay(
                effectiveTicks, processTicksPerGameTick, generator.gameTicksPerDay());
        int requestedPerBatch = Math.multiplyExact(
                processTicksPerGameTick, generator.townBatchGameTicks());
        long dailyDemand = (long) processTicksPerGameTick * generator.gameTicksPerDay();
        long repeatDays = effectiveTicks / greatestCommonDivisor(effectiveTicks, dailyDemand);
        long repeatDemand = Math.multiplyExact(dailyDemand, repeatDays);
        GeneratorFuelModel.FuelSettlement exactCycle = GeneratorFuelModel.settleProcessDemand(
                effectiveTicks, requestedPerBatch, repeatDemand, 0L);
        double simulatedItemsPerActiveDay = (double) exactCycle.loadedFuelItems() / repeatDays;
        return new TowerFuelTheory(
                effectiveTicks,
                processTicksPerGameTick,
                itemsPerActiveDay,
                repeatDays,
                exactCycle.loadedFuelItems(),
                exactCycle.remainingProcessTicks(),
                simulatedItemsPerActiveDay);
    }

    public static HouseDailyModel.SettlementParameters houseParameters(TownModelParameters parameters) {
        TownModelParameters.HousingParameters housing = parameters.housing();
        TownModelParameters.TemperatureRatingParameters temperature =
                parameters.buildingScoring().temperature();
        TownModelParameters.SpaceRatingParameters space = parameters.buildingScoring().space();
        return new HouseDailyModel.SettlementParameters(
                housing.foodConsumptionPerResidentDay(),
                housing.nutritionReferencePerFoodUnit(),
                housing.minimumNutritionRecoveryMultiplier(),
                temperature.comfortableTemperatureCelsius(),
                temperature.minimumRating(),
                temperature.sigmoidSlopePerCelsius(),
                temperature.halfPointTemperatureDifferenceCelsius(),
                space.areaCoefficient(),
                space.heightLogCoefficient(),
                space.heightLogOffset(),
                space.responseScale(),
                space.responseExponent(),
                housing.temperatureComfortWeight(),
                housing.spaceComfortWeight(),
                housing.decorationComfortWeight());
    }

    public static HouseDailyModel.ResidentEffectParameters residentEffectParameters(
            TownModelParameters parameters
    ) {
        TownModelParameters.HousingParameters housing = parameters.housing();
        return new HouseDailyModel.ResidentEffectParameters(
                housing.foodDeficitPenaltyExponent(),
                housing.healthLossAtZeroFoodPerResidentDay(),
                housing.mentalLossAtZeroFoodPerResidentDay(),
                housing.minimumTemperatureCelsius(),
                housing.maximumTemperatureCelsius(),
                housing.temperatureFullStressDistanceCelsius(),
                housing.temperatureStressPenaltyExponent(),
                housing.healthLossAtFullTemperatureStressPerResidentDay(),
                housing.mentalLossAtFullTemperatureStressPerResidentDay(),
                housing.maximumHealthRecoveryPerResidentDay(),
                housing.maximumMentalRecoveryPerResidentDay());
    }

    private static long greatestCommonDivisor(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0L) {
            long next = left % right;
            left = right;
            right = next;
        }
        return left;
    }

    public record MiningTheory(
            double totalMiningSwe,
            double totalOreItems,
            double coalFraction,
            double coalItems,
            double coalPerMiningSweDay,
            long exhaustedChunks,
            long enteredChunks
    ) {
    }

    public record Moment(
            double meanFoodUnitsPerRoll,
            double varianceFoodUnitsPerRoll,
            double meanMeatItemsPerRoll,
            double varianceMeatItemsPerRoll
    ) {
    }

    public record LootSample(String item, int count) {
    }

    public record TowerFuelTheory(
            int effectiveProcessTicksPerItem,
            int processTicksPerGameTick,
            double theoryItemsPerActiveDay,
            long exactCycleDays,
            long exactCycleLoadedItems,
            long exactCycleRemainingProcessTicks,
            double simulationItemsPerActiveDay
    ) {
    }
}
