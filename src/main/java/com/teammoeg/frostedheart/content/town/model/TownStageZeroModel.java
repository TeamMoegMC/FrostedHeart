/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.town.model;

import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorFuelModel;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorHeatFieldModel;
import com.teammoeg.frostedheart.content.town.resource.TownFoodResourceAmount;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Pure algebraic audit required by stage 0 of docs/town-model.md. */
public final class TownStageZeroModel {
    private TownStageZeroModel() {
    }

    public static StageZeroMetrics analyze(
            TownModelParameters parameters,
            List<WeightedResource> mineWeights,
            List<WeightedLootEntry> huntingLoot,
            int coalRecipeProcessTicks,
            int cokeRecipeProcessTicks,
            double generatorEfficiencyResearchBonus
    ) {
        double miningStandardSwe = parameters.mining().productivity().standardWorkerEquivalent();
        double huntingStandardSwe = parameters.hunting().productivity().standardWorkerEquivalent();

        double totalMineWeight = positiveSum(mineWeights, WeightedResource::weight);
        double coalWeight = mineWeights.stream()
                .filter(entry -> "minecraft:coal".equals(entry.item()))
                .mapToDouble(WeightedResource::weight)
                .sum();
        double coalFraction = safeDivide(coalWeight, totalMineWeight);
        double coalPerMiningSweDay = parameters.mining().baseOutputPerStandardWorkerDay()
                * miningStandardSwe * coalFraction;

        Map<String, TownModelParameters.MeatFoodParameters> meatByRawItem =
                parameters.meatFoods().stream().collect(Collectors.toMap(
                        TownModelParameters.MeatFoodParameters::rawItem,
                        value -> value));
        double totalLootWeight = positiveSum(huntingLoot, WeightedLootEntry::weight);
        double meatPerRoll = expectedPerRoll(huntingLoot, totalLootWeight, entry ->
                meatByRawItem.containsKey(entry.item()) ? entry.expectedCount() : 0.0);
        double rawFoodPerRoll = expectedPerRoll(huntingLoot, totalLootWeight, entry -> {
            TownModelParameters.MeatFoodParameters meat = meatByRawItem.get(entry.item());
            if (meat == null) return 0.0;
            return entry.expectedCount() * TownFoodResourceAmount.fromFoodProperties(
                    meat.rawHunger(), meat.rawSaturationModifier());
        });
        double cookedFoodPerRoll = expectedPerRoll(huntingLoot, totalLootWeight, entry -> {
            TownModelParameters.MeatFoodParameters meat = meatByRawItem.get(entry.item());
            if (meat == null) return 0.0;
            return entry.expectedCount() * TownFoodResourceAmount.fromFoodProperties(
                    meat.cookedHunger(), meat.cookedSaturationModifier());
        });

        double rollsPerHuntingSweDay = parameters.hunting().expectedLootRollsPerStandardWorkerDay()
                * huntingStandardSwe;
        double meatPerHuntingSweDay = rollsPerHuntingSweDay * meatPerRoll;
        double rawFoodPerHuntingSweDay = rollsPerHuntingSweDay * rawFoodPerRoll;
        double cookedFoodPerHuntingSweDay = rollsPerHuntingSweDay * cookedFoodPerRoll;
        double foodPerResidentDay = parameters.housing().foodConsumptionPerResidentDay();

        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        int effectiveCoalTicks = GeneratorFuelModel.effectiveFuelProcessTicks(
                coalRecipeProcessTicks,
                generator.baseFuelDurationMultiplier(),
                generatorEfficiencyResearchBonus);
        int effectiveCokeTicks = GeneratorFuelModel.effectiveFuelProcessTicks(
                cokeRecipeProcessTicks,
                generator.baseFuelDurationMultiplier(),
                generatorEfficiencyResearchBonus);
        int normalProcessTicksPerGameTick = generator.baseProcessTicksPerGameTick();
        double idealCoalPerDay = GeneratorFuelModel.idealFuelItemsPerDay(
                effectiveCoalTicks, normalProcessTicksPerGameTick, generator.gameTicksPerDay());
        double idealCokePerDay = GeneratorFuelModel.idealFuelItemsPerDay(
                effectiveCokeTicks, normalProcessTicksPerGameTick, generator.gameTicksPerDay());
        double currentBatchCoalPerDay = GeneratorFuelModel.currentTownBatchFuelItemsPerDay(
                effectiveCoalTicks,
                normalProcessTicksPerGameTick,
                generator.townBatchGameTicks(),
                generator.gameTicksPerDay());
        double currentBatchCokePerDay = GeneratorFuelModel.currentTownBatchFuelItemsPerDay(
                effectiveCokeTicks,
                normalProcessTicksPerGameTick,
                generator.townBatchGameTicks(),
                generator.gameTicksPerDay());

        return new StageZeroMetrics(
                miningStandardSwe,
                huntingStandardSwe,
                coalFraction,
                coalPerMiningSweDay,
                meatPerRoll,
                meatPerHuntingSweDay,
                rawFoodPerRoll,
                rawFoodPerHuntingSweDay,
                cookedFoodPerRoll,
                cookedFoodPerHuntingSweDay,
                foodPerResidentDay,
                safeDivide(foodPerResidentDay, rawFoodPerHuntingSweDay),
                safeDivide(foodPerResidentDay, cookedFoodPerHuntingSweDay),
                effectiveCoalTicks,
                effectiveCokeTicks,
                idealCoalPerDay,
                idealCokePerDay,
                currentBatchCoalPerDay,
                currentBatchCokePerDay,
                safeDivide(idealCoalPerDay, coalPerMiningSweDay),
                safeDivide(idealCokePerDay, coalPerMiningSweDay),
                safeDivide(currentBatchCoalPerDay, coalPerMiningSweDay),
                safeDivide(currentBatchCokePerDay, coalPerMiningSweDay),
                GeneratorHeatFieldModel.radiusBlocks(
                        1.0, generator.baseRadiusBlocks(), generator.additionalRadiusPerLevelBlocks()),
                GeneratorHeatFieldModel.temperatureCelsius(
                        1.0, generator.temperaturePerLevelCelsius()));
    }

    private static <T> double positiveSum(List<T> values, ToDoubleFunction<T> getter) {
        return values.stream().mapToDouble(getter).filter(value -> value > 0.0).sum();
    }

    private static double expectedPerRoll(
            List<WeightedLootEntry> entries,
            double totalWeight,
            ToDoubleFunction<WeightedLootEntry> value
    ) {
        double weighted = entries.stream()
                .filter(entry -> entry.weight() > 0.0)
                .mapToDouble(entry -> entry.weight() * value.applyAsDouble(entry))
                .sum();
        return safeDivide(weighted, totalWeight);
    }

    private static double safeDivide(double numerator, double denominator) {
        return denominator > 0.0 ? numerator / denominator : 0.0;
    }

    public record WeightedResource(String item, double weight) {
    }

    public record WeightedLootEntry(String item, double weight, double minimumCount, double maximumCount) {
        public double expectedCount() {
            return (minimumCount + maximumCount) / 2.0;
        }
    }

    public record StageZeroMetrics(
            double miningStandardWorkerSwe,
            double huntingStandardWorkerSwe,
            double coalFractionOfMineOutput,
            double coalPerMiningSweDay,
            double meatPerHuntingRoll,
            double meatPerHuntingSweDay,
            double rawFoodUnitsPerHuntingRoll,
            double rawFoodUnitsPerHuntingSweDay,
            double cookedFoodUnitsPerHuntingRoll,
            double cookedFoodUnitsPerHuntingSweDay,
            double foodUnitsPerResidentDay,
            double rawDietHuntingSwePerResident,
            double cookedDietHuntingSwePerResident,
            int effectiveCoalProcessTicks,
            int effectiveCokeProcessTicks,
            double idealTowerCoalPerActiveDay,
            double idealTowerCokePerActiveDay,
            double currentTownBatchTowerCoalPerActiveDay,
            double currentTownBatchTowerCokePerActiveDay,
            double idealTowerMiningSweUsingCoal,
            double idealTowerMiningSweUsingCoke,
            double currentTownBatchMiningSweUsingCoal,
            double currentTownBatchMiningSweUsingCoke,
            int t1NormalRadiusBlocks,
            int t1NormalHeatFieldCelsius
    ) {
    }
}
