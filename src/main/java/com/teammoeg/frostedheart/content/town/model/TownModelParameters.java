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
import com.teammoeg.frostedheart.content.town.TownMathFunctions;

import java.util.List;

/**
 * Forge-independent parameter snapshot for the town numerical model.
 * Stage 0 intentionally contains only parameters needed by its algebraic
 * audit; later stages can extend this aggregate without changing the formulas.
 */
public record TownModelParameters(
        MiningParameters mining,
        HuntingParameters hunting,
        HousingParameters housing,
        GeneratorT1Parameters generatorT1,
        List<MeatFoodParameters> meatFoods
) {
    public TownModelParameters {
        meatFoods = List.copyOf(meatFoods);
    }

    public static TownModelParameters currentDefaults() {
        return new TownModelParameters(
                new MiningParameters(
                        Defaults.MINING_BASE_OUTPUT_PER_SWE_DAY,
                        new ResidentProductivityParameters(
                                Defaults.MINING_HEALTH_WEIGHT,
                                Defaults.MINING_MENTAL_WEIGHT,
                                Defaults.MINING_STRENGTH_WEIGHT,
                                Defaults.MINING_INTELLIGENCE_WEIGHT,
                                Defaults.MINING_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
                                Defaults.MINING_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
                                Defaults.MINING_MAXIMUM_PROFICIENCY,
                                Defaults.MINING_BONUS_AT_MAXIMUM_PROFICIENCY,
                                Defaults.MINING_MINIMUM_PRODUCTIVITY,
                                Defaults.MINING_MAXIMUM_PRODUCTIVITY)),
                new HuntingParameters(
                        Defaults.HUNTING_EXPECTED_LOOT_ROLLS_PER_SWE_DAY,
                        new ResidentProductivityParameters(
                                Defaults.HUNTING_HEALTH_WEIGHT,
                                Defaults.HUNTING_MENTAL_WEIGHT,
                                Defaults.HUNTING_STRENGTH_WEIGHT,
                                Defaults.HUNTING_INTELLIGENCE_WEIGHT,
                                Defaults.HUNTING_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
                                Defaults.HUNTING_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
                                Defaults.HUNTING_MAXIMUM_PROFICIENCY,
                                Defaults.HUNTING_BONUS_AT_MAXIMUM_PROFICIENCY,
                                Defaults.HUNTING_MINIMUM_PRODUCTIVITY,
                                Defaults.HUNTING_MAXIMUM_PRODUCTIVITY)),
                new HousingParameters(Defaults.FOOD_PER_RESIDENT_DAY),
                new GeneratorT1Parameters(
                        GeneratorFuelModel.CURRENT_BASE_FUEL_DURATION_MULTIPLIER,
                        GeneratorFuelModel.CURRENT_BASE_PROCESS_TICKS_PER_GAME_TICK,
                        GeneratorFuelModel.CURRENT_OVERDRIVE_EXTRA_PROCESS_TICKS_PER_GAME_TICK,
                        GeneratorFuelModel.CURRENT_TOWN_BATCH_GAME_TICKS,
                        GeneratorFuelModel.GAME_TICKS_PER_DAY,
                        GeneratorHeatFieldModel.CURRENT_BASE_RADIUS_BLOCKS,
                        GeneratorHeatFieldModel.CURRENT_ADDITIONAL_RADIUS_PER_LEVEL_BLOCKS,
                        GeneratorHeatFieldModel.CURRENT_TEMPERATURE_PER_LEVEL_CELSIUS),
                List.of(
                        new MeatFoodParameters("minecraft:beef", "minecraft:cooked_beef", 3, 0.3, 8, 0.8),
                        new MeatFoodParameters("minecraft:porkchop", "minecraft:cooked_porkchop", 3, 0.3, 8, 0.8),
                        new MeatFoodParameters("minecraft:chicken", "minecraft:cooked_chicken", 2, 0.3, 6, 0.6),
                        new MeatFoodParameters("minecraft:mutton", "minecraft:cooked_mutton", 2, 0.3, 6, 0.8)
                ));
    }

    public record MiningParameters(
            double baseOutputPerStandardWorkerDay,
            ResidentProductivityParameters productivity
    ) {
    }

    public record HuntingParameters(
            double expectedLootRollsPerStandardWorkerDay,
            ResidentProductivityParameters productivity
    ) {
    }

    public record HousingParameters(double foodConsumptionPerResidentDay) {
    }

    public record GeneratorT1Parameters(
            double baseFuelDurationMultiplier,
            int baseProcessTicksPerGameTick,
            int overdriveExtraProcessTicksPerGameTick,
            int townBatchGameTicks,
            int gameTicksPerDay,
            int baseRadiusBlocks,
            int additionalRadiusPerLevelBlocks,
            int temperaturePerLevelCelsius
    ) {
    }

    public record ResidentProductivityParameters(
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
        public double productivity(
                double health,
                double mental,
                double strength,
                double intelligence,
                double proficiency
        ) {
            return TownMathFunctions.linearResidentProductivity(
                    new double[]{health, mental, strength, intelligence},
                    new double[]{healthWeight, mentalWeight, strengthWeight, intelligenceWeight},
                    proficiency,
                    productivityAtAttributeZero,
                    productivityAtAttributeHundred,
                    maximumProficiency,
                    bonusAtMaximumProficiency,
                    minimumProductivity,
                    maximumProductivity);
        }

        public double standardWorkerEquivalent() {
            return productivity(50.0, 50.0, 50.0, 50.0, 0.0);
        }
    }

    public record MeatFoodParameters(
            String rawItem,
            String cookedItem,
            int rawHunger,
            double rawSaturationModifier,
            int cookedHunger,
            double cookedSaturationModifier
    ) {
    }

    /** Defaults shared directly with FHConfig declarations. */
    public static final class Defaults {
        public static final double FOOD_PER_RESIDENT_DAY = 6.5;

        public static final double MINING_BASE_OUTPUT_PER_SWE_DAY = 3.5;
        public static final double MINING_HEALTH_WEIGHT = 30.0;
        public static final double MINING_MENTAL_WEIGHT = 10.0;
        public static final double MINING_STRENGTH_WEIGHT = 45.0;
        public static final double MINING_INTELLIGENCE_WEIGHT = 15.0;
        public static final double MINING_PRODUCTIVITY_AT_ATTRIBUTE_ZERO = 0.5;
        public static final double MINING_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED = 1.5;
        public static final double MINING_MAXIMUM_PROFICIENCY = 100.0;
        public static final double MINING_BONUS_AT_MAXIMUM_PROFICIENCY = 0.5;
        public static final double MINING_MINIMUM_PRODUCTIVITY = 0.5;
        public static final double MINING_MAXIMUM_PRODUCTIVITY = 2.0;

        public static final double HUNTING_EXPECTED_LOOT_ROLLS_PER_SWE_DAY = 7.0 / 6.0;
        public static final double HUNTING_HEALTH_WEIGHT = 25.0;
        public static final double HUNTING_MENTAL_WEIGHT = 20.0;
        public static final double HUNTING_STRENGTH_WEIGHT = 25.0;
        public static final double HUNTING_INTELLIGENCE_WEIGHT = 30.0;
        public static final double HUNTING_PRODUCTIVITY_AT_ATTRIBUTE_ZERO = 0.5;
        public static final double HUNTING_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED = 1.5;
        public static final double HUNTING_MAXIMUM_PROFICIENCY = 100.0;
        public static final double HUNTING_BONUS_AT_MAXIMUM_PROFICIENCY = 1.0;
        public static final double HUNTING_MINIMUM_PRODUCTIVITY = 0.5;
        public static final double HUNTING_MAXIMUM_PRODUCTIVITY = 2.5;

        private Defaults() {
        }
    }
}
