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

import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.resident.ResidentActivity;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionSupportModel;
import com.teammoeg.frostedheart.content.town.transport.TransportConsumerParameters;

import java.util.List;

/**
 * Forge-independent parameter snapshot for the town numerical model.
 * Stage 0 snapshots the T1, climate, resident, housing and work parameters that
 * later simulation stages consume. T2 is intentionally absent.
 */
public record TownModelParameters(
        MiningParameters mining,
        HuntingParameters hunting,
        TransportStationParameters transportStation,
        TransportConsumerParameters transportConsumers,
        HousingParameters housing,
        ResidentParameters residents,
        BuildingScoringParameters buildingScoring,
        TerrainResourceParameters terrainResources,
        GeneratorT1Parameters generatorT1,
        ClimateParameters climate,
        ObservationParameters observation,
        List<MeatFoodParameters> meatFoods
) {
    public TownModelParameters {
        meatFoods = List.copyOf(meatFoods);
    }

    /** Source-compatible constructor for callers created before transport consumers existed. */
    public TownModelParameters(
            MiningParameters mining,
            HuntingParameters hunting,
            TransportStationParameters transportStation,
            HousingParameters housing,
            ResidentParameters residents,
            BuildingScoringParameters buildingScoring,
            TerrainResourceParameters terrainResources,
            GeneratorT1Parameters generatorT1,
            ClimateParameters climate,
            ObservationParameters observation,
            List<MeatFoodParameters> meatFoods
    ) {
        this(mining, hunting, transportStation, defaultTransportConsumerParameters(), housing, residents,
                buildingScoring, terrainResources, generatorT1, climate, observation, meatFoods);
    }

    /** Source-compatible constructor for callers created before transport-station parameters existed. */
    public TownModelParameters(
            MiningParameters mining,
            HuntingParameters hunting,
            HousingParameters housing,
            ResidentParameters residents,
            BuildingScoringParameters buildingScoring,
            TerrainResourceParameters terrainResources,
            GeneratorT1Parameters generatorT1,
            ClimateParameters climate,
            ObservationParameters observation,
            List<MeatFoodParameters> meatFoods
    ) {
        this(mining, hunting, defaultTransportStationParameters(), defaultTransportConsumerParameters(), housing, residents,
                buildingScoring, terrainResources, generatorT1, climate, observation, meatFoods);
    }

    /** Source-compatible constructor for scenario tests created before observation parameters existed. */
    public TownModelParameters(
            MiningParameters mining,
            HuntingParameters hunting,
            HousingParameters housing,
            ResidentParameters residents,
            BuildingScoringParameters buildingScoring,
            TerrainResourceParameters terrainResources,
            GeneratorT1Parameters generatorT1,
            ClimateParameters climate,
            List<MeatFoodParameters> meatFoods
    ) {
        this(mining, hunting, defaultTransportStationParameters(), defaultTransportConsumerParameters(), housing, residents,
                buildingScoring, terrainResources,
                generatorT1, climate,
                new ObservationParameters(Defaults.TOWN_OBSERVATION_HISTORY_DAYS,
                        Defaults.TOWN_OBSERVATION_RESERVE_WARNING_DAYS,
                        Defaults.TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS),
                meatFoods);
    }

    /** Returns a simulation-only snapshot with selected nutrition pacing values replaced. */
    public TownModelParameters withNutritionTuning(
            double referencePerFoodUnit,
            double reserveLossPerDay,
            double gainAtReference
    ) {
        if (!Double.isFinite(referencePerFoodUnit) || referencePerFoodUnit <= 0.0) {
            throw new IllegalArgumentException("referencePerFoodUnit must be finite and positive.");
        }
        if (!Double.isFinite(reserveLossPerDay) || reserveLossPerDay < 0.0) {
            throw new IllegalArgumentException("reserveLossPerDay must be finite and non-negative.");
        }
        if (!Double.isFinite(gainAtReference) || gainAtReference < 0.0) {
            throw new IllegalArgumentException("gainAtReference must be finite and non-negative.");
        }
        HousingParameters oldHousing = housing;
        HousingParameters tunedHousing = new HousingParameters(
                oldHousing.foodConsumptionPerResidentDay(), referencePerFoodUnit,
                oldHousing.foodDeficitPenaltyExponent(),
                oldHousing.healthLossAtZeroFoodPerResidentDay(),
                oldHousing.mentalLossAtZeroFoodPerResidentDay(),
                oldHousing.maximumHealthRecoveryPerResidentDay(),
                oldHousing.maximumMentalRecoveryPerResidentDay(),
                oldHousing.minimumFloorAreaBlocks(), oldHousing.minimumInteriorVolumeBlocks(),
                oldHousing.minimumTemperatureCelsius(), oldHousing.maximumTemperatureCelsius(),
                oldHousing.temperatureFullStressDistanceCelsius(),
                oldHousing.temperatureStressPenaltyExponent(),
                oldHousing.healthLossAtFullTemperatureStressPerResidentDay(),
                oldHousing.mentalLossAtFullTemperatureStressPerResidentDay(),
                oldHousing.floorBlocksPerResident(), oldHousing.temperatureComfortWeight(),
                oldHousing.spaceComfortWeight(), oldHousing.decorationComfortWeight(),
                oldHousing.decorationRating());
        ResidentParameters oldResidents = residents;
        ResidentNutritionParameters oldNutrition = oldResidents.nutrition();
        ResidentNutritionParameters tunedNutrition = new ResidentNutritionParameters(
                oldNutrition.maximumReserve(), oldNutrition.initialReserve(),
                oldNutrition.healthyReserve(), oldNutrition.severeReserve(),
                reserveLossPerDay, gainAtReference, oldNutrition.maximumCoverage(),
                oldNutrition.mealSelectionChunks(),
                oldNutrition.strengthGrowthEfficiencyAtZeroSupport(),
                oldNutrition.intelligenceGrowthEfficiencyAtZeroSupport(),
                oldNutrition.strengthMaintenanceThreshold(),
                oldNutrition.intelligenceMaintenanceThreshold(),
                oldNutrition.deficiencyExponent(),
                oldNutrition.strengthDecayAtZeroSupport(),
                oldNutrition.intelligenceDecayAtZeroSupport(),
                oldNutrition.supportWeights());
        ResidentParameters tunedResidents = new ResidentParameters(
                oldResidents.homelessHealthLossPerDay(), oldResidents.removalHealthThreshold(),
                oldResidents.removalMentalThreshold(), oldResidents.minimumWorkingAge(),
                oldResidents.minimumWorkingHealthExclusive(),
                oldResidents.minimumWorkingMentalExclusive(), oldResidents.workRequiresHousing(),
                oldResidents.maximumWorkProficiency(),
                oldResidents.proficiencyGrowthAtZeroPerWorkday(),
                oldResidents.minimumProficiencyGrowthPerWorkday(), oldResidents.generation(),
                oldResidents.aging(), tunedNutrition, oldResidents.residentialCareScoreBand(),
                oldResidents.townPolicyCooldownDays());
        return new TownModelParameters(
                mining, hunting, transportStation, transportConsumers, tunedHousing, tunedResidents, buildingScoring,
                terrainResources, generatorT1, climate, observation, meatFoods);
    }

    private static TransportStationParameters defaultTransportStationParameters() {
        return new TransportStationParameters(
                Defaults.TRANSPORT_STATION_CAPACITY_PER_STANDARD_WORKER_DAY,
                Defaults.TRANSPORT_STATION_FLOOR_BLOCKS_PER_WORKER_SLOT,
                Defaults.TRANSPORT_STATION_MINIMUM_WORKER_SLOTS,
                Defaults.TRANSPORT_STATION_MINIMUM_FLOOR_AREA_BLOCKS,
                Defaults.TRANSPORT_STATION_MINIMUM_INTERIOR_VOLUME_BLOCKS,
                new ResidentProductivityParameters(
                        Defaults.TRANSPORT_STATION_HEALTH_WEIGHT,
                        Defaults.TRANSPORT_STATION_MENTAL_WEIGHT,
                        Defaults.TRANSPORT_STATION_STRENGTH_WEIGHT,
                        Defaults.TRANSPORT_STATION_INTELLIGENCE_WEIGHT,
                        Defaults.TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
                        Defaults.TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
                        Defaults.TRANSPORT_STATION_MAXIMUM_PROFICIENCY,
                        Defaults.TRANSPORT_STATION_BONUS_AT_MAXIMUM_PROFICIENCY,
                        Defaults.TRANSPORT_STATION_MINIMUM_PRODUCTIVITY,
                        Defaults.TRANSPORT_STATION_MAXIMUM_PRODUCTIVITY),
                new ResidentActivity(
                        Defaults.TRANSPORT_STATION_PHYSICAL_ACTIVITY,
                Defaults.TRANSPORT_STATION_LEARNING_ACTIVITY));
    }

    private static TransportConsumerParameters defaultTransportConsumerParameters() {
        return new TransportConsumerParameters(
                Defaults.TRANSPORT_CONSUMER_DEFAULT_RATE_ITEMS_PER_SECOND,
                Defaults.TRANSPORT_CONSUMER_MINIMUM_RATE_ITEMS_PER_SECOND,
                Defaults.TRANSPORT_CONSUMER_MAXIMUM_RATE_ITEMS_PER_SECOND,
                Defaults.TRANSPORT_CONSUMER_WAREHOUSE_DISTANCE_COST_PER_BLOCK,
                Defaults.TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK);
    }

    /**
     * Builds the simulator's default input from the single source-owned
     * default table below. Gameplay code must read the corresponding
     * FHConfig values instead of calling this method.
     */
    public static TownModelParameters currentDefaults() {
        return new TownModelParameters(
                new MiningParameters(
                        Defaults.MINING_BASE_OUTPUT_PER_SWE_DAY,
                        Defaults.MINING_FLOOR_BLOCKS_PER_WORKER_SLOT,
                        Defaults.MINING_MINIMUM_WORKER_SLOTS,
                        Defaults.MINING_CONNECTION_RADIUS_BLOCKS,
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
                                Defaults.MINING_MAXIMUM_PRODUCTIVITY),
                        Defaults.MINING_ASSIGNMENT_BASE_PRIORITY,
                        Defaults.MINING_ASSIGNMENT_PENALTY_PER_WORKER,
                        Defaults.MINING_ASSIGNMENT_FILL_RATIO_BONUS,
                        new ResidentActivity(
                                Defaults.MINING_PHYSICAL_ACTIVITY,
                                Defaults.MINING_LEARNING_ACTIVITY)),
                new HuntingParameters(
                        Defaults.HUNTING_EXPECTED_LOOT_ROLLS_PER_SWE_DAY,
                        Defaults.HUNTING_PASSIVE_EXPECTED_LOOT_ROLLS_PER_BASE_DAY,
                        Defaults.HUNTING_USE_FRACTIONAL_LOOT_ROLL_CARRY,
                        Defaults.HUNTING_FLOOR_BLOCKS_PER_WORKER_SLOT,
                        Defaults.HUNTING_MINIMUM_WORKER_SLOTS,
                        Defaults.HUNTING_MINIMUM_FLOOR_AREA_BLOCKS,
                        Defaults.HUNTING_MINIMUM_INTERIOR_VOLUME_BLOCKS,
                        Defaults.HUNTING_MINIMUM_WORKING_TEMPERATURE_CELSIUS,
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
                                Defaults.HUNTING_MAXIMUM_PRODUCTIVITY),
                        Defaults.HUNTING_SPACE_RATING_WEIGHT,
                        Defaults.HUNTING_TEMPERATURE_RATING_WEIGHT,
                        Defaults.HUNTING_ASSIGNMENT_BASE_PRIORITY,
                        Defaults.HUNTING_ASSIGNMENT_PENALTY_PER_WORKER,
                        Defaults.HUNTING_ASSIGNMENT_FILL_RATIO_BONUS,
                        Defaults.HUNTING_ASSIGNMENT_RATING_MULTIPLIER,
                        new ResidentActivity(
                                Defaults.HUNTING_PHYSICAL_ACTIVITY,
                                Defaults.HUNTING_LEARNING_ACTIVITY)),
                        defaultTransportStationParameters(),
                        defaultTransportConsumerParameters(),
                new HousingParameters(
                        Defaults.HOUSING_FOOD_PER_RESIDENT_DAY,
                        Defaults.HOUSING_NUTRITION_REFERENCE_PER_FOOD_UNIT,
                        Defaults.HOUSING_FOOD_DEFICIT_PENALTY_EXPONENT,
                        Defaults.HOUSING_HEALTH_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY,
                        Defaults.HOUSING_MENTAL_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY,
                        Defaults.HOUSING_MAXIMUM_HEALTH_RECOVERY_PER_RESIDENT_DAY,
                        Defaults.HOUSING_MAXIMUM_MENTAL_RECOVERY_PER_RESIDENT_DAY,
                        Defaults.HOUSING_MINIMUM_FLOOR_AREA_BLOCKS,
                        Defaults.HOUSING_MINIMUM_INTERIOR_VOLUME_BLOCKS,
                        Defaults.HOUSING_MINIMUM_TEMPERATURE_CELSIUS,
                        Defaults.HOUSING_MAXIMUM_TEMPERATURE_CELSIUS,
                        Defaults.HOUSING_TEMPERATURE_FULL_STRESS_DISTANCE_CELSIUS,
                        Defaults.HOUSING_TEMPERATURE_STRESS_PENALTY_EXPONENT,
                        Defaults.HOUSING_HEALTH_LOSS_AT_FULL_TEMPERATURE_STRESS_PER_RESIDENT_DAY,
                        Defaults.HOUSING_MENTAL_LOSS_AT_FULL_TEMPERATURE_STRESS_PER_RESIDENT_DAY,
                        Defaults.HOUSING_FLOOR_BLOCKS_PER_RESIDENT,
                        Defaults.HOUSING_TEMPERATURE_COMFORT_WEIGHT,
                        Defaults.HOUSING_SPACE_COMFORT_WEIGHT,
                        Defaults.HOUSING_DECORATION_COMFORT_WEIGHT,
                        new DecorationRatingParameters(
                                Defaults.DECORATION_COUNT_LOG_OFFSET,
                                Defaults.DECORATION_COUNT_LOG_MULTIPLIER,
                                Defaults.DECORATION_TYPE_BASE_SCORE,
                                Defaults.DECORATION_BASE_DEMAND,
                                Defaults.DECORATION_FLOOR_BLOCKS_PER_DEMAND)),
                new ResidentParameters(
                        Defaults.RESIDENT_HOMELESS_HEALTH_LOSS_PER_DAY,
                        Defaults.RESIDENT_REMOVAL_HEALTH_THRESHOLD,
                        Defaults.RESIDENT_REMOVAL_MENTAL_THRESHOLD,
                        Defaults.RESIDENT_MINIMUM_WORKING_AGE,
                        Defaults.RESIDENT_MINIMUM_WORKING_HEALTH_EXCLUSIVE,
                        Defaults.RESIDENT_MINIMUM_WORKING_MENTAL_EXCLUSIVE,
                        Defaults.RESIDENT_WORK_REQUIRES_HOUSING,
                        Defaults.RESIDENT_MAXIMUM_WORK_PROFICIENCY,
                        Defaults.RESIDENT_PROFICIENCY_GROWTH_AT_ZERO_PER_WORKDAY,
                        Defaults.RESIDENT_MINIMUM_PROFICIENCY_GROWTH_PER_WORKDAY,
                        new ResidentGenerationParameters(
                                Defaults.RESIDENT_INITIAL_HEALTH_MINIMUM,
                                Defaults.RESIDENT_INITIAL_HEALTH_MAXIMUM,
                                Defaults.RESIDENT_INITIAL_MENTAL_MINIMUM,
                                Defaults.RESIDENT_INITIAL_MENTAL_MAXIMUM,
                                Defaults.RESIDENT_INITIAL_NUTRITION_MINIMUM,
                                Defaults.RESIDENT_INITIAL_NUTRITION_MAXIMUM,
                                Defaults.RESIDENT_ATTRIBUTE_SAMPLE_COUNT,
                                Defaults.RESIDENT_INFANT_STRENGTH_CENTER,
                                Defaults.RESIDENT_INFANT_INTELLIGENCE_CENTER,
                                Defaults.RESIDENT_CHILD_STRENGTH_CENTER,
                                Defaults.RESIDENT_CHILD_INTELLIGENCE_CENTER,
                                Defaults.RESIDENT_ADULT_STRENGTH_CENTER,
                                Defaults.RESIDENT_ADULT_INTELLIGENCE_CENTER,
                                Defaults.RESIDENT_ELDER_STRENGTH_CENTER,
                                Defaults.RESIDENT_ELDER_INTELLIGENCE_CENTER,
                                Defaults.RESIDENT_NON_ADULT_ATTRIBUTE_SPREAD,
                                Defaults.RESIDENT_ADULT_ATTRIBUTE_SPREAD,
                                Defaults.RESIDENT_INFANT_INITIAL_PROFICIENCY,
                                Defaults.RESIDENT_CHILD_MAXIMUM_INITIAL_PROFICIENCY,
                                Defaults.RESIDENT_ADULT_MAXIMUM_INITIAL_PROFICIENCY,
                                Defaults.RESIDENT_ELDER_MINIMUM_INITIAL_PROFICIENCY,
                                Defaults.RESIDENT_ELDER_MAXIMUM_INITIAL_PROFICIENCY,
                                Defaults.RESIDENT_ADULT_AGE_RANGE_DAYS_EXCLUSIVE,
                                new ResidentAgeWeightParameters(
                                        Defaults.RESIDENT_AGE_WEIGHT_INFANT,
                                        Defaults.RESIDENT_AGE_WEIGHT_CHILD,
                                        Defaults.RESIDENT_AGE_WEIGHT_ADULT,
                                        Defaults.RESIDENT_AGE_WEIGHT_ELDER),
                                new ResidentAgeWeightParameters(
                                        Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_INFANT,
                                        Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_CHILD,
                                        Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_ADULT,
                                        Defaults.RESIDENT_FALLBACK_AGE_WEIGHT_ELDER),
                                Defaults.RESIDENT_EDUCATION_WEIGHT_LEVEL_0,
                                Defaults.RESIDENT_EDUCATION_WEIGHT_LEVEL_1,
                                Defaults.RESIDENT_EDUCATION_WEIGHT_LEVEL_2,
                                Defaults.RESIDENT_EDUCATION_WEIGHT_LEVEL_3,
                                Defaults.RESIDENT_EDUCATION_WEIGHT_LEVEL_4,
                                Defaults.RESIDENT_EDUCATION_WEIGHT_LEVEL_5,
                                Defaults.RESIDENT_COLD_SURVIVOR_HEALTH_MINIMUM,
                                Defaults.RESIDENT_COLD_SURVIVOR_HEALTH_MAXIMUM,
                                Defaults.RESIDENT_COLD_SURVIVOR_ATTRIBUTE_BONUS,
                                Defaults.RESIDENT_COLD_SURVIVOR_PROFICIENCY_MULTIPLIER,
                                Defaults.RESIDENT_COLD_SURVIVOR_CHANCE),
                        new ResidentAgingParameters(
                                Defaults.RESIDENT_INFANT_TO_CHILD_DAYS,
                                Defaults.RESIDENT_CHILD_TO_ADULT_DAYS,
                                Defaults.RESIDENT_INFANT_BASE_ACTIVITY,
                                Defaults.RESIDENT_CHILD_BASE_ACTIVITY,
                                Defaults.RESIDENT_ADULT_BASE_ACTIVITY,
                                Defaults.RESIDENT_ELDER_BASE_ACTIVITY,
                                Defaults.RESIDENT_INFANT_STRENGTH_GAIN_PER_DAY,
                                Defaults.RESIDENT_INFANT_INTELLIGENCE_GAIN_PER_DAY,
                                Defaults.RESIDENT_INFANT_ATTRIBUTE_CAP,
                                Defaults.RESIDENT_CHILD_STRENGTH_GAIN_PER_DAY,
                                Defaults.RESIDENT_CHILD_INTELLIGENCE_GAIN_PER_DAY,
                                Defaults.RESIDENT_CHILD_STRENGTH_CAP,
                                Defaults.RESIDENT_CHILD_INTELLIGENCE_CAP,
                                Defaults.RESIDENT_ADULT_STRENGTH_GAIN_PER_DAY,
                                Defaults.RESIDENT_ADULT_INTELLIGENCE_GAIN_PER_DAY,
                                Defaults.RESIDENT_ADULT_ATTRIBUTE_CAP,
                                Defaults.RESIDENT_ELDER_STRENGTH_GAIN_PER_DAY,
                                Defaults.RESIDENT_ELDER_INTELLIGENCE_GAIN_PER_DAY,
                                Defaults.RESIDENT_ELDER_STRENGTH_AGE_DECAY_PER_DAY,
                                Defaults.RESIDENT_ELDER_INTELLIGENCE_AGE_DECAY_PER_DAY),
                        new ResidentNutritionParameters(
                                Defaults.RESIDENT_NUTRITION_MAXIMUM_RESERVE,
                                Defaults.RESIDENT_NUTRITION_INITIAL_RESERVE,
                                Defaults.RESIDENT_NUTRITION_HEALTHY_RESERVE,
                                Defaults.RESIDENT_NUTRITION_SEVERE_RESERVE,
                                Defaults.RESIDENT_NUTRITION_RESERVE_LOSS_PER_DAY,
                                Defaults.RESIDENT_NUTRITION_GAIN_AT_REFERENCE,
                                Defaults.RESIDENT_NUTRITION_MAXIMUM_COVERAGE,
                                Defaults.RESIDENT_NUTRITION_MEAL_SELECTION_CHUNKS,
                                Defaults.RESIDENT_STRENGTH_GROWTH_EFFICIENCY_AT_ZERO_SUPPORT,
                                Defaults.RESIDENT_INTELLIGENCE_GROWTH_EFFICIENCY_AT_ZERO_SUPPORT,
                                Defaults.RESIDENT_STRENGTH_MAINTENANCE_THRESHOLD,
                                Defaults.RESIDENT_INTELLIGENCE_MAINTENANCE_THRESHOLD,
                                Defaults.RESIDENT_NUTRITION_DEFICIENCY_EXPONENT,
                                Defaults.RESIDENT_STRENGTH_DECAY_AT_ZERO_SUPPORT,
                                Defaults.RESIDENT_INTELLIGENCE_DECAY_AT_ZERO_SUPPORT,
                                ResidentNutritionSupportModel.DEFAULT_WEIGHTS),
                        Defaults.RESIDENTIAL_CARE_SCORE_BAND,
                        Defaults.TOWN_POLICY_COOLDOWN_DAYS),
                new BuildingScoringParameters(
                        new TemperatureRatingParameters(
                                Defaults.BUILDING_COMFORTABLE_TEMPERATURE_CELSIUS,
                                Defaults.BUILDING_MINIMUM_TEMPERATURE_RATING,
                                Defaults.BUILDING_TEMPERATURE_RATING_SLOPE,
                                Defaults.BUILDING_TEMPERATURE_RATING_HALF_POINT_DIFFERENCE_CELSIUS),
                        new SpaceRatingParameters(
                                Defaults.BUILDING_SPACE_AREA_COEFFICIENT,
                                Defaults.BUILDING_SPACE_HEIGHT_LOG_COEFFICIENT,
                                Defaults.BUILDING_SPACE_HEIGHT_LOG_OFFSET,
                                Defaults.BUILDING_SPACE_RESPONSE_SCALE,
                                Defaults.BUILDING_SPACE_RESPONSE_EXPONENT)),
                new TerrainResourceParameters(
                        Defaults.ORE_RESERVE_PER_CHUNK,
                        Defaults.ORE_RECOVERY_PER_CHUNK_DAY,
                        Defaults.HUNT_RESERVE_PER_SQUARE_BLOCK,
                        Defaults.HUNT_RECOVERY_PER_SQUARE_BLOCK_DAY),
                new GeneratorT1Parameters(
                        Defaults.GENERATOR_T1_BASE_FUEL_DURATION_MULTIPLIER,
                        Defaults.GENERATOR_T1_BASE_PROCESS_TICKS_PER_GAME_TICK,
                        Defaults.GENERATOR_T1_OVERDRIVE_EXTRA_PROCESS_TICKS_PER_GAME_TICK,
                        Defaults.TOWN_UPDATE_INTERVAL_GAME_TICKS,
                        GameUnits.GAME_TICKS_PER_DAY,
                        Defaults.GENERATOR_T1_BASE_RADIUS_BLOCKS,
                        Defaults.GENERATOR_T1_ADDITIONAL_RADIUS_PER_LEVEL_BLOCKS,
                        Defaults.GENERATOR_T1_TEMPERATURE_PER_LEVEL_CELSIUS),
                new ClimateParameters(
                        Defaults.CLIMATE_TRACK_COUNT,
                        Defaults.CLIMATE_EVENT_CHOICE_ROLL_BOUND,
                        Defaults.CLIMATE_WARM_EVENT_MINIMUM_ROLL_INCLUSIVE,
                        Defaults.CLIMATE_OPENING_WARM_ROLL_BONUS,
                        Defaults.CLIMATE_OPENING_BIAS_THROUGH_DAY_INCLUSIVE,
                        Defaults.CLIMATE_COLD_BOTTOM_EXTREME_CELSIUS,
                        Defaults.CLIMATE_COLD_BOTTOM_SEVERE_CELSIUS,
                        Defaults.CLIMATE_COLD_BOTTOM_STRONG_CELSIUS,
                        Defaults.CLIMATE_COLD_BOTTOM_NORMAL_CELSIUS,
                        Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_EXTREME,
                        Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_SEVERE,
                        Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_STRONG,
                        Defaults.CLIMATE_COLD_BOTTOM_WEIGHT_NORMAL,
                        Defaults.CLIMATE_EVENT_MINIMUM_DAYS,
                        Defaults.CLIMATE_EVENT_MAXIMUM_DAYS_EXCLUSIVE,
                        Defaults.CLIMATE_PADDING_MINIMUM_HOURS,
                        Defaults.CLIMATE_PADDING_MAXIMUM_HOURS_EXCLUSIVE,
                        Defaults.CLIMATE_CALM_MINIMUM_DAYS,
                        Defaults.CLIMATE_CALM_MAXIMUM_DAYS_EXCLUSIVE,
                        Defaults.CLIMATE_COLD_PRELUDE_PEAK_CELSIUS,
                        Defaults.CLIMATE_WARM_PEAK_CELSIUS,
                        Defaults.CLIMATE_FORECAST_SENSITIVITY_CELSIUS,
                        Defaults.CLIMATE_EVENT_NOISE_STANDARD_DEVIATION_CELSIUS,
                        Defaults.CLIMATE_WARM_NOISE_SCALE,
                        Defaults.CLIMATE_ABSOLUTE_ZERO_CELSIUS,
                        Defaults.CLIMATE_OVERWORLD_BASELINE_CELSIUS,
                        Defaults.CLIMATE_STONE_INTERFACE_LEVEL,
                        Defaults.CLIMATE_SEA_LEVEL,
                        Defaults.CLIMATE_BLOCK_MAXIMUM_AFFECTION,
                        Defaults.CLIMATE_BLOCK_HEAT_APPLICATION_MULTIPLIER),
                new ObservationParameters(
                        Defaults.TOWN_OBSERVATION_HISTORY_DAYS,
                        Defaults.TOWN_OBSERVATION_RESERVE_WARNING_DAYS,
                        Defaults.TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS),
                List.of(
                        new MeatFoodParameters("minecraft:beef", "minecraft:cooked_beef", 3, 0.3, 8, 0.8),
                        new MeatFoodParameters("minecraft:porkchop", "minecraft:cooked_porkchop", 3, 0.3, 8, 0.8),
                        new MeatFoodParameters("minecraft:chicken", "minecraft:cooked_chicken", 2, 0.3, 6, 0.6),
                        new MeatFoodParameters("minecraft:mutton", "minecraft:cooked_mutton", 2, 0.3, 6, 0.8)
                ));
    }

    public record MiningParameters(
            double baseOutputPerStandardWorkerDay,
            double floorBlocksPerWorkerSlot,
            int minimumWorkerSlots,
            int connectionRadiusBlocks,
            ResidentProductivityParameters productivity,
            double assignmentBasePriority,
            double assignmentPenaltyPerWorker,
            double assignmentFillRatioBonus,
            ResidentActivity activity
    ) {
        public MiningParameters(
                double baseOutputPerStandardWorkerDay,
                double floorBlocksPerWorkerSlot,
                int minimumWorkerSlots,
                int connectionRadiusBlocks,
                ResidentProductivityParameters productivity,
                double assignmentBasePriority,
                double assignmentPenaltyPerWorker,
                double assignmentFillRatioBonus
        ) {
            this(baseOutputPerStandardWorkerDay, floorBlocksPerWorkerSlot,
                    minimumWorkerSlots, connectionRadiusBlocks, productivity,
                    assignmentBasePriority, assignmentPenaltyPerWorker,
                    assignmentFillRatioBonus,
                    new ResidentActivity(
                            Defaults.MINING_PHYSICAL_ACTIVITY,
                            Defaults.MINING_LEARNING_ACTIVITY));
        }
    }

    public record HuntingParameters(
            double expectedLootRollsPerStandardWorkerDay,
            double passiveExpectedLootRollsPerBaseDay,
            boolean useFractionalLootRollCarry,
            double floorBlocksPerWorkerSlot,
            int minimumWorkerSlots,
            int minimumFloorAreaBlocks,
            int minimumInteriorVolumeBlocks,
            double minimumWorkingTemperatureCelsius,
            ResidentProductivityParameters productivity,
            double spaceRatingWeight,
            double temperatureRatingWeight,
            double assignmentBasePriority,
            double assignmentPenaltyPerWorker,
            double assignmentFillRatioBonus,
            double assignmentRatingMultiplier,
            ResidentActivity activity
    ) {
        public HuntingParameters(
                double expectedLootRollsPerStandardWorkerDay,
                double passiveExpectedLootRollsPerBaseDay,
                boolean useFractionalLootRollCarry,
                double floorBlocksPerWorkerSlot,
                int minimumWorkerSlots,
                int minimumFloorAreaBlocks,
                int minimumInteriorVolumeBlocks,
                double minimumWorkingTemperatureCelsius,
                ResidentProductivityParameters productivity,
                double spaceRatingWeight,
                double temperatureRatingWeight,
                double assignmentBasePriority,
                double assignmentPenaltyPerWorker,
                double assignmentFillRatioBonus,
                double assignmentRatingMultiplier
        ) {
            this(expectedLootRollsPerStandardWorkerDay,
                    passiveExpectedLootRollsPerBaseDay, useFractionalLootRollCarry,
                    floorBlocksPerWorkerSlot, minimumWorkerSlots,
                    minimumFloorAreaBlocks, minimumInteriorVolumeBlocks,
                    minimumWorkingTemperatureCelsius, productivity,
                    spaceRatingWeight, temperatureRatingWeight,
                    assignmentBasePriority, assignmentPenaltyPerWorker,
                    assignmentFillRatioBonus, assignmentRatingMultiplier,
                    new ResidentActivity(
                            Defaults.HUNTING_PHYSICAL_ACTIVITY,
                            Defaults.HUNTING_LEARNING_ACTIVITY));
        }
    }

    public record TransportStationParameters(
            double capacityPerStandardWorkerDay,
            double floorBlocksPerWorkerSlot,
            int minimumWorkerSlots,
            int minimumFloorAreaBlocks,
            int minimumInteriorVolumeBlocks,
            ResidentProductivityParameters productivity,
            ResidentActivity activity
    ) {
        public TransportStationParameters(
                double capacityPerStandardWorkerDay,
                double floorBlocksPerWorkerSlot,
                int minimumWorkerSlots,
                int minimumFloorAreaBlocks,
                int minimumInteriorVolumeBlocks,
                ResidentProductivityParameters productivity
        ) {
            this(capacityPerStandardWorkerDay, floorBlocksPerWorkerSlot,
                    minimumWorkerSlots, minimumFloorAreaBlocks,
                    minimumInteriorVolumeBlocks, productivity,
                    new ResidentActivity(
                            Defaults.TRANSPORT_STATION_PHYSICAL_ACTIVITY,
                            Defaults.TRANSPORT_STATION_LEARNING_ACTIVITY));
        }
    }

    public record HousingParameters(
            double foodConsumptionPerResidentDay,
            double nutritionReferencePerFoodUnit,
            double foodDeficitPenaltyExponent,
            double healthLossAtZeroFoodPerResidentDay,
            double mentalLossAtZeroFoodPerResidentDay,
            double maximumHealthRecoveryPerResidentDay,
            double maximumMentalRecoveryPerResidentDay,
            int minimumFloorAreaBlocks,
            int minimumInteriorVolumeBlocks,
            double minimumTemperatureCelsius,
            double maximumTemperatureCelsius,
            double temperatureFullStressDistanceCelsius,
            double temperatureStressPenaltyExponent,
            double healthLossAtFullTemperatureStressPerResidentDay,
            double mentalLossAtFullTemperatureStressPerResidentDay,
            double floorBlocksPerResident,
            double temperatureComfortWeight,
            double spaceComfortWeight,
            double decorationComfortWeight,
            DecorationRatingParameters decorationRating
    ) {
    }

    public record ResidentParameters(
            double homelessHealthLossPerDay,
            double removalHealthThreshold,
            double removalMentalThreshold,
            int minimumWorkingAge,
            double minimumWorkingHealthExclusive,
            double minimumWorkingMentalExclusive,
            boolean workRequiresHousing,
            double maximumWorkProficiency,
            double proficiencyGrowthAtZeroPerWorkday,
            double minimumProficiencyGrowthPerWorkday,
            ResidentGenerationParameters generation,
            ResidentAgingParameters aging,
            ResidentNutritionParameters nutrition,
            double residentialCareScoreBand,
            int townPolicyCooldownDays
    ) {
    }

    /** Four-channel reserve, recovery, growth, and meal-choice tuning. */
    public record ResidentNutritionParameters(
            double maximumReserve,
            double initialReserve,
            double healthyReserve,
            double severeReserve,
            double reserveLossPerDay,
            double gainAtReference,
            double maximumCoverage,
            int mealSelectionChunks,
            double strengthGrowthEfficiencyAtZeroSupport,
            double intelligenceGrowthEfficiencyAtZeroSupport,
            double strengthMaintenanceThreshold,
            double intelligenceMaintenanceThreshold,
            double deficiencyExponent,
            double strengthDecayAtZeroSupport,
            double intelligenceDecayAtZeroSupport,
            ResidentNutritionSupportModel.Weights supportWeights
    ) {
    }

    /** Recruitment-time resident distribution currently used by gameplay. */
    public record ResidentGenerationParameters(
            double initialHealthMinimum,
            double initialHealthMaximum,
            double initialMentalMinimum,
            double initialMentalMaximum,
            double initialNutritionMinimum,
            double initialNutritionMaximum,
            int attributeSampleCount,
            double infantStrengthCenter,
            double infantIntelligenceCenter,
            double childStrengthCenter,
            double childIntelligenceCenter,
            double adultStrengthCenter,
            double adultIntelligenceCenter,
            double elderStrengthCenter,
            double elderIntelligenceCenter,
            double nonAdultAttributeSpread,
            double adultAttributeSpread,
            double infantInitialProficiency,
            double childMaximumInitialProficiency,
            double adultMaximumInitialProficiency,
            double elderMinimumInitialProficiency,
            double elderMaximumInitialProficiency,
            int adultAgeRangeDaysExclusive,
            ResidentAgeWeightParameters ageWeights,
            ResidentAgeWeightParameters fallbackAgeWeights,
            double educationWeightLevel0,
            double educationWeightLevel1,
            double educationWeightLevel2,
            double educationWeightLevel3,
            double educationWeightLevel4,
            double educationWeightLevel5,
            double coldSurvivorHealthMinimum,
            double coldSurvivorHealthMaximum,
            double coldSurvivorAttributeBonus,
            double coldSurvivorProficiencyMultiplier,
            double coldSurvivorChance
    ) {
    }

    public record ResidentAgeWeightParameters(
            double infant,
            double child,
            double adult,
            double elder
    ) {
    }

    public record ResidentAgingParameters(
            int infantToChildDays,
            int childToAdultDays,
            double infantBaseActivity,
            double childBaseActivity,
            double adultBaseActivity,
            double elderBaseActivity,
            double infantStrengthGainPerDay,
            double infantIntelligenceGainPerDay,
            double infantAttributeCap,
            double childStrengthGainPerDay,
            double childIntelligenceGainPerDay,
            double childStrengthCap,
            double childIntelligenceCap,
            double adultStrengthGainPerDay,
            double adultIntelligenceGainPerDay,
            double adultAttributeCap,
            double elderStrengthGainPerDay,
            double elderIntelligenceGainPerDay,
            double elderStrengthAgeDecayPerDay,
            double elderIntelligenceAgeDecayPerDay
    ) {
    }

    public record BuildingScoringParameters(
            TemperatureRatingParameters temperature,
            SpaceRatingParameters space
    ) {
    }

    public record TemperatureRatingParameters(
            double comfortableTemperatureCelsius,
            double minimumRating,
            double sigmoidSlopePerCelsius,
            double halfPointTemperatureDifferenceCelsius
    ) {
    }

    public record SpaceRatingParameters(
            double areaCoefficient,
            double heightLogCoefficient,
            double heightLogOffset,
            double responseScale,
            double responseExponent
    ) {
    }

    public record DecorationRatingParameters(
            double countLogOffset,
            double countLogMultiplier,
            double typeBaseScore,
            double baseDemand,
            double floorBlocksPerDemand
    ) {
    }

    public record TerrainResourceParameters(
            double oreReservePerChunk,
            double oreRecoveryPerChunkDay,
            double huntReservePerSquareBlock,
            double huntRecoveryPerSquareBlockDay
    ) {
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

    /** Ordinary long-term climate and block-temperature parameters used in stage 4. */
    public record ClimateParameters(
            int trackCount,
            int eventChoiceRollBound,
            int warmEventMinimumRollInclusive,
            int openingWarmRollBonus,
            int openingBiasThroughDayInclusive,
            float coldBottomExtremeCelsius,
            float coldBottomSevereCelsius,
            float coldBottomStrongCelsius,
            float coldBottomNormalCelsius,
            int coldBottomWeightExtreme,
            int coldBottomWeightSevere,
            int coldBottomWeightStrong,
            int coldBottomWeightNormal,
            int eventMinimumDays,
            int eventMaximumDaysExclusive,
            int paddingMinimumHours,
            int paddingMaximumHoursExclusive,
            int calmMinimumDays,
            int calmMaximumDaysExclusive,
            float coldPreludePeakCelsius,
            float warmPeakCelsius,
            float forecastSensitivityCelsius,
            float eventNoiseStandardDeviationCelsius,
            float warmNoiseScale,
            float absoluteZeroCelsius,
            float overworldBaselineCelsius,
            int stoneInterfaceLevel,
            int seaLevel,
            float blockMaximumClimateAffection,
            float blockHeatApplicationMultiplier
    ) {
    }

    /** Player-facing operational thresholds shared by gameplay and simulation. */
    public record ObservationParameters(
            int historyDays,
            double reserveWarningDays,
            double reserveCriticalDays
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

    /**
     * Single source of truth for FH-owned model defaults.
     * <p>
     * Change balancing defaults here. TownModelParameters consumes them for
     * simulation, while FHConfig consumes them when declaring runtime config
     * defaults. Runtime gameplay code must read FHConfig, never this class.
     */
    public static final class Defaults {
        public static final int TOWN_UPDATE_INTERVAL_GAME_TICKS = 20;
        public static final int TOWN_OBSERVATION_HISTORY_DAYS = 90;
        public static final double TOWN_OBSERVATION_RESERVE_WARNING_DAYS = 7.0;
        public static final double TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS = 3.0;

		public static final double HOUSING_FOOD_PER_RESIDENT_DAY = 20.0;
		public static final double HOUSING_NUTRITION_REFERENCE_PER_FOOD_UNIT = 200.0;
        public static final double RESIDENT_NUTRITION_MAXIMUM_RESERVE = 100.0;
        public static final double RESIDENT_NUTRITION_INITIAL_RESERVE = 70.0;
        public static final double RESIDENT_NUTRITION_HEALTHY_RESERVE = 70.0;
        public static final double RESIDENT_NUTRITION_SEVERE_RESERVE = 20.0;
		public static final double RESIDENT_NUTRITION_RESERVE_LOSS_PER_DAY = 1.0;
		public static final double RESIDENT_NUTRITION_GAIN_AT_REFERENCE = 2.0;
        public static final double RESIDENT_NUTRITION_MAXIMUM_COVERAGE = 2.0;
        public static final int RESIDENT_NUTRITION_MEAL_SELECTION_CHUNKS = 8;
        public static final double RESIDENT_STRENGTH_GROWTH_EFFICIENCY_AT_ZERO_SUPPORT = 0.20;
        public static final double RESIDENT_INTELLIGENCE_GROWTH_EFFICIENCY_AT_ZERO_SUPPORT = 0.40;
        public static final double RESIDENT_STRENGTH_MAINTENANCE_THRESHOLD = 0.40;
        public static final double RESIDENT_INTELLIGENCE_MAINTENANCE_THRESHOLD = 0.30;
        public static final double RESIDENT_NUTRITION_DEFICIENCY_EXPONENT = 1.5;
        public static final double RESIDENT_STRENGTH_DECAY_AT_ZERO_SUPPORT = 0.70;
        public static final double RESIDENT_INTELLIGENCE_DECAY_AT_ZERO_SUPPORT = 0.17;
        public static final double RESIDENTIAL_CARE_SCORE_BAND = 0.05;
        public static final int TOWN_POLICY_COOLDOWN_DAYS = 7;
        public static final double HOUSING_FOOD_DEFICIT_PENALTY_EXPONENT = 2.0;
        public static final double HOUSING_HEALTH_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY = 8.0;
        public static final double HOUSING_MENTAL_LOSS_AT_ZERO_FOOD_PER_RESIDENT_DAY = 5.0;
        public static final double HOUSING_MAXIMUM_HEALTH_RECOVERY_PER_RESIDENT_DAY = 2.0;
        public static final double HOUSING_MAXIMUM_MENTAL_RECOVERY_PER_RESIDENT_DAY = 1.5;
        public static final int HOUSING_MINIMUM_FLOOR_AREA_BLOCKS = 4;
        public static final int HOUSING_MINIMUM_INTERIOR_VOLUME_BLOCKS = 8;
        public static final double HOUSING_MINIMUM_TEMPERATURE_CELSIUS = 0.0;
        public static final double HOUSING_MAXIMUM_TEMPERATURE_CELSIUS = 40.0;
        public static final double HOUSING_TEMPERATURE_FULL_STRESS_DISTANCE_CELSIUS = 20.0;
        public static final double HOUSING_TEMPERATURE_STRESS_PENALTY_EXPONENT = 2.0;
        public static final double HOUSING_HEALTH_LOSS_AT_FULL_TEMPERATURE_STRESS_PER_RESIDENT_DAY = 10.0;
        public static final double HOUSING_MENTAL_LOSS_AT_FULL_TEMPERATURE_STRESS_PER_RESIDENT_DAY = 5.0;
        public static final double HOUSING_FLOOR_BLOCKS_PER_RESIDENT = 4.0;
        public static final double HOUSING_TEMPERATURE_COMFORT_WEIGHT = 0.4;
        public static final double HOUSING_SPACE_COMFORT_WEIGHT = 0.3;
        public static final double HOUSING_DECORATION_COMFORT_WEIGHT = 0.3;

        public static final double BUILDING_COMFORTABLE_TEMPERATURE_CELSIUS = 24.0;
        public static final double BUILDING_MINIMUM_TEMPERATURE_RATING = 0.017;
        public static final double BUILDING_TEMPERATURE_RATING_SLOPE = 0.4;
        public static final double BUILDING_TEMPERATURE_RATING_HALF_POINT_DIFFERENCE_CELSIUS = 10.0;
        public static final double BUILDING_SPACE_AREA_COEFFICIENT = 1.55;
        public static final double BUILDING_SPACE_HEIGHT_LOG_COEFFICIENT = 0.6;
        public static final double BUILDING_SPACE_HEIGHT_LOG_OFFSET = 1.6;
        public static final double BUILDING_SPACE_RESPONSE_SCALE = 0.024;
        public static final double BUILDING_SPACE_RESPONSE_EXPONENT = 1.11;

        public static final double DECORATION_COUNT_LOG_OFFSET = 0.32;
        public static final double DECORATION_COUNT_LOG_MULTIPLIER = 1.75;
        public static final double DECORATION_TYPE_BASE_SCORE = 0.9;
        public static final double DECORATION_BASE_DEMAND = 6.0;
        public static final double DECORATION_FLOOR_BLOCKS_PER_DEMAND = 16.0;

        public static final double RESIDENT_HOMELESS_HEALTH_LOSS_PER_DAY = 10.0;
        public static final double RESIDENT_REMOVAL_HEALTH_THRESHOLD = 5.0;
        public static final double RESIDENT_REMOVAL_MENTAL_THRESHOLD = 5.0;
        public static final int RESIDENT_MINIMUM_WORKING_AGE = 1;
        public static final double RESIDENT_MINIMUM_WORKING_HEALTH_EXCLUSIVE = 10.0;
        public static final double RESIDENT_MINIMUM_WORKING_MENTAL_EXCLUSIVE = 5.0;
        public static final boolean RESIDENT_WORK_REQUIRES_HOUSING = true;
        public static final double RESIDENT_MAXIMUM_WORK_PROFICIENCY = 100.0;
        public static final double RESIDENT_PROFICIENCY_GROWTH_AT_ZERO_PER_WORKDAY = 2.4;
        public static final double RESIDENT_MINIMUM_PROFICIENCY_GROWTH_PER_WORKDAY = 0.25;
        public static final double RESIDENT_INITIAL_HEALTH_MINIMUM = 30.0;
        public static final double RESIDENT_INITIAL_HEALTH_MAXIMUM = 70.0;
        public static final double RESIDENT_INITIAL_MENTAL_MINIMUM = 30.0;
        public static final double RESIDENT_INITIAL_MENTAL_MAXIMUM = 70.0;
        public static final double RESIDENT_INITIAL_NUTRITION_MINIMUM = 30.0;
        public static final double RESIDENT_INITIAL_NUTRITION_MAXIMUM = 70.0;
        public static final int RESIDENT_ATTRIBUTE_SAMPLE_COUNT = 4;
        public static final double RESIDENT_INFANT_STRENGTH_CENTER = 20.0;
        public static final double RESIDENT_INFANT_INTELLIGENCE_CENTER = 30.0;
        public static final double RESIDENT_CHILD_STRENGTH_CENTER = 40.0;
        public static final double RESIDENT_CHILD_INTELLIGENCE_CENTER = 40.0;
        public static final double RESIDENT_ADULT_STRENGTH_CENTER = 50.0;
        public static final double RESIDENT_ADULT_INTELLIGENCE_CENTER = 50.0;
        public static final double RESIDENT_ELDER_STRENGTH_CENTER = 35.0;
        public static final double RESIDENT_ELDER_INTELLIGENCE_CENTER = 65.0;
        public static final double RESIDENT_NON_ADULT_ATTRIBUTE_SPREAD = 0.8;
        public static final double RESIDENT_ADULT_ATTRIBUTE_SPREAD = 1.0;
        public static final double RESIDENT_INFANT_INITIAL_PROFICIENCY = 0.0;
        public static final double RESIDENT_CHILD_MAXIMUM_INITIAL_PROFICIENCY = 25.0;
        public static final double RESIDENT_ADULT_MAXIMUM_INITIAL_PROFICIENCY = 50.0;
        public static final double RESIDENT_ELDER_MINIMUM_INITIAL_PROFICIENCY = 50.0;
        public static final double RESIDENT_ELDER_MAXIMUM_INITIAL_PROFICIENCY = 100.0;
        public static final int RESIDENT_ADULT_AGE_RANGE_DAYS_EXCLUSIVE = 3650;
        public static final double RESIDENT_AGE_WEIGHT_INFANT = 10.0;
        public static final double RESIDENT_AGE_WEIGHT_CHILD = 20.0;
        public static final double RESIDENT_AGE_WEIGHT_ADULT = 60.0;
        public static final double RESIDENT_AGE_WEIGHT_ELDER = 10.0;
        public static final double RESIDENT_FALLBACK_AGE_WEIGHT_INFANT = 10.0;
        public static final double RESIDENT_FALLBACK_AGE_WEIGHT_CHILD = 20.0;
        public static final double RESIDENT_FALLBACK_AGE_WEIGHT_ADULT = 50.0;
        public static final double RESIDENT_FALLBACK_AGE_WEIGHT_ELDER = 20.0;
        public static final double RESIDENT_EDUCATION_WEIGHT_LEVEL_0 = 0.15;
        public static final double RESIDENT_EDUCATION_WEIGHT_LEVEL_1 = 0.50;
        public static final double RESIDENT_EDUCATION_WEIGHT_LEVEL_2 = 0.20;
        public static final double RESIDENT_EDUCATION_WEIGHT_LEVEL_3 = 0.10;
        public static final double RESIDENT_EDUCATION_WEIGHT_LEVEL_4 = 0.04;
        public static final double RESIDENT_EDUCATION_WEIGHT_LEVEL_5 = 0.01;
        public static final double RESIDENT_COLD_SURVIVOR_HEALTH_MINIMUM = 30.0;
        public static final double RESIDENT_COLD_SURVIVOR_HEALTH_MAXIMUM = 40.0;
        public static final double RESIDENT_COLD_SURVIVOR_ATTRIBUTE_BONUS = 15.0;
        public static final double RESIDENT_COLD_SURVIVOR_PROFICIENCY_MULTIPLIER = 1.5;
        public static final double RESIDENT_COLD_SURVIVOR_CHANCE = 0.5;
        public static final int RESIDENT_INFANT_TO_CHILD_DAYS = 30;
        public static final int RESIDENT_CHILD_TO_ADULT_DAYS = 60;
        public static final double RESIDENT_INFANT_BASE_ACTIVITY = 1.0;
        public static final double RESIDENT_CHILD_BASE_ACTIVITY = 0.7;
        public static final double RESIDENT_ADULT_BASE_ACTIVITY = 0.3;
        public static final double RESIDENT_ELDER_BASE_ACTIVITY = 0.1;
        public static final double RESIDENT_INFANT_STRENGTH_GAIN_PER_DAY = 1.8;
        public static final double RESIDENT_INFANT_INTELLIGENCE_GAIN_PER_DAY = 1.6;
        public static final double RESIDENT_INFANT_ATTRIBUTE_CAP = 40.0;
        public static final double RESIDENT_CHILD_STRENGTH_GAIN_PER_DAY = 3.9;
        public static final double RESIDENT_CHILD_INTELLIGENCE_GAIN_PER_DAY = 4.2;
        public static final double RESIDENT_CHILD_STRENGTH_CAP = 80.0;
        public static final double RESIDENT_CHILD_INTELLIGENCE_CAP = 85.0;
        public static final double RESIDENT_ADULT_STRENGTH_GAIN_PER_DAY = 0.05;
        public static final double RESIDENT_ADULT_INTELLIGENCE_GAIN_PER_DAY = 0.05;
        public static final double RESIDENT_ADULT_ATTRIBUTE_CAP = 100.0;
        public static final double RESIDENT_ELDER_STRENGTH_GAIN_PER_DAY = 0.06;
        public static final double RESIDENT_ELDER_INTELLIGENCE_GAIN_PER_DAY = 0.05;
        public static final double RESIDENT_ELDER_STRENGTH_AGE_DECAY_PER_DAY = 0.0048;
        public static final double RESIDENT_ELDER_INTELLIGENCE_AGE_DECAY_PER_DAY = 0.002;

        public static final double GENERATOR_T1_BASE_FUEL_DURATION_MULTIPLIER = 0.7;
        public static final int GENERATOR_T1_BASE_PROCESS_TICKS_PER_GAME_TICK = 1;
        public static final int GENERATOR_T1_OVERDRIVE_EXTRA_PROCESS_TICKS_PER_GAME_TICK = 1;
        public static final int GENERATOR_T1_BASE_RADIUS_BLOCKS = 16;
        public static final int GENERATOR_T1_ADDITIONAL_RADIUS_PER_LEVEL_BLOCKS = 8;
        public static final int GENERATOR_T1_TEMPERATURE_PER_LEVEL_CELSIUS = 10;

        public static final int CLIMATE_TRACK_COUNT = 3;
        public static final int CLIMATE_EVENT_CHOICE_ROLL_BOUND = 10;
        public static final int CLIMATE_WARM_EVENT_MINIMUM_ROLL_INCLUSIVE = 8;
        public static final int CLIMATE_OPENING_WARM_ROLL_BONUS = 3;
        public static final int CLIMATE_OPENING_BIAS_THROUGH_DAY_INCLUSIVE = 15;
        public static final float CLIMATE_COLD_BOTTOM_EXTREME_CELSIUS = -40.0F;
        public static final float CLIMATE_COLD_BOTTOM_SEVERE_CELSIUS = -30.0F;
        public static final float CLIMATE_COLD_BOTTOM_STRONG_CELSIUS = -20.0F;
        public static final float CLIMATE_COLD_BOTTOM_NORMAL_CELSIUS = -10.0F;
        public static final int CLIMATE_COLD_BOTTOM_WEIGHT_EXTREME = 1;
        public static final int CLIMATE_COLD_BOTTOM_WEIGHT_SEVERE = 2;
        public static final int CLIMATE_COLD_BOTTOM_WEIGHT_STRONG = 3;
        public static final int CLIMATE_COLD_BOTTOM_WEIGHT_NORMAL = 4;
        public static final int CLIMATE_EVENT_MINIMUM_DAYS = 2;
        public static final int CLIMATE_EVENT_MAXIMUM_DAYS_EXCLUSIVE = 7;
        public static final int CLIMATE_PADDING_MINIMUM_HOURS = 8;
        public static final int CLIMATE_PADDING_MAXIMUM_HOURS_EXCLUSIVE = 24;
        public static final int CLIMATE_CALM_MINIMUM_DAYS = 2;
        public static final int CLIMATE_CALM_MAXIMUM_DAYS_EXCLUSIVE = 7;
        public static final float CLIMATE_COLD_PRELUDE_PEAK_CELSIUS = -5.0F;
        public static final float CLIMATE_WARM_PEAK_CELSIUS = 8.0F;
        public static final float CLIMATE_FORECAST_SENSITIVITY_CELSIUS = 2.0F;
        public static final float CLIMATE_EVENT_NOISE_STANDARD_DEVIATION_CELSIUS = 1.0F;
        public static final float CLIMATE_WARM_NOISE_SCALE = 2.0F;
        public static final float CLIMATE_ABSOLUTE_ZERO_CELSIUS = -273.0F;
        public static final float CLIMATE_OVERWORLD_BASELINE_CELSIUS = -10.0F;
        public static final int CLIMATE_STONE_INTERFACE_LEVEL = 0;
        public static final int CLIMATE_SEA_LEVEL = 63;
        public static final float CLIMATE_BLOCK_MAXIMUM_AFFECTION = 0.5F;
        public static final float CLIMATE_BLOCK_HEAT_APPLICATION_MULTIPLIER = 2.0F;

        public static final double MINING_BASE_OUTPUT_PER_SWE_DAY = 3.5;
        public static final double MINING_PHYSICAL_ACTIVITY = 1.0;
        public static final double MINING_LEARNING_ACTIVITY = 0.25;
        public static final double MINING_FLOOR_BLOCKS_PER_WORKER_SLOT = 4.0;
        public static final int MINING_MINIMUM_WORKER_SLOTS = 1;
        public static final int MINING_CONNECTION_RADIUS_BLOCKS = 1024;
        public static final double TRANSPORT_STATION_FLOOR_BLOCKS_PER_WORKER_SLOT = 4.0;
        public static final int TRANSPORT_STATION_MINIMUM_WORKER_SLOTS = 1;
        public static final int TRANSPORT_STATION_MINIMUM_FLOOR_AREA_BLOCKS = 4;
        public static final int TRANSPORT_STATION_MINIMUM_INTERIOR_VOLUME_BLOCKS = 8;
        public static final double TRANSPORT_STATION_CAPACITY_PER_STANDARD_WORKER_DAY = 64.0;
        public static final double TRANSPORT_STATION_PHYSICAL_ACTIVITY = 1.0;
        public static final double TRANSPORT_STATION_LEARNING_ACTIVITY = 0.25;
        public static final double TRANSPORT_STATION_HEALTH_WEIGHT = 35.0;
        public static final double TRANSPORT_STATION_MENTAL_WEIGHT = 15.0;
        public static final double TRANSPORT_STATION_STRENGTH_WEIGHT = 30.0;
        public static final double TRANSPORT_STATION_INTELLIGENCE_WEIGHT = 20.0;
        public static final double TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_ZERO = 0.5;
        public static final double TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED = 1.5;
        public static final double TRANSPORT_STATION_MAXIMUM_PROFICIENCY = 100.0;
        public static final double TRANSPORT_STATION_BONUS_AT_MAXIMUM_PROFICIENCY = 0.8;
        public static final double TRANSPORT_STATION_MINIMUM_PRODUCTIVITY = 0.5;
        public static final double TRANSPORT_STATION_MAXIMUM_PRODUCTIVITY = 2.3;
        public static final int TRANSPORT_CONSUMER_DEFAULT_RATE_ITEMS_PER_SECOND = 20;
        public static final int TRANSPORT_CONSUMER_MINIMUM_RATE_ITEMS_PER_SECOND = 1;
        public static final int TRANSPORT_CONSUMER_MAXIMUM_RATE_ITEMS_PER_SECOND = 1280;
        public static final double TRANSPORT_CONSUMER_WAREHOUSE_DISTANCE_COST_PER_BLOCK = 0.05;
        public static final double TRANSPORT_CONSUMER_P2P_DISTANCE_COST_PER_BLOCK = 0.05;
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
        public static final double MINING_ASSIGNMENT_BASE_PRIORITY = 0.4;
        public static final double MINING_ASSIGNMENT_PENALTY_PER_WORKER = 1.0;
        public static final double MINING_ASSIGNMENT_FILL_RATIO_BONUS = 1.0;

        public static final double HUNTING_EXPECTED_LOOT_ROLLS_PER_SWE_DAY = 7.0 / 6.0;
        public static final double HUNTING_PHYSICAL_ACTIVITY = 1.0;
        public static final double HUNTING_LEARNING_ACTIVITY = 0.25;
        public static final double HUNTING_PASSIVE_EXPECTED_LOOT_ROLLS_PER_BASE_DAY = 0.0;
        public static final boolean HUNTING_USE_FRACTIONAL_LOOT_ROLL_CARRY = true;
        public static final double HUNTING_FLOOR_BLOCKS_PER_WORKER_SLOT = 4.0;
        public static final int HUNTING_MINIMUM_WORKER_SLOTS = 1;
        public static final int HUNTING_MINIMUM_FLOOR_AREA_BLOCKS = 4;
        public static final int HUNTING_MINIMUM_INTERIOR_VOLUME_BLOCKS = 8;
        public static final double HUNTING_MINIMUM_WORKING_TEMPERATURE_CELSIUS = 0.0;
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
        public static final double HUNTING_SPACE_RATING_WEIGHT = 3.0;
        public static final double HUNTING_TEMPERATURE_RATING_WEIGHT = 2.0;
        public static final double HUNTING_ASSIGNMENT_BASE_PRIORITY = 0.5;
        public static final double HUNTING_ASSIGNMENT_PENALTY_PER_WORKER = 1.0;
        public static final double HUNTING_ASSIGNMENT_FILL_RATIO_BONUS = 1.0;
        public static final double HUNTING_ASSIGNMENT_RATING_MULTIPLIER = 1.0;

        public static final double ORE_RESERVE_PER_CHUNK = 1000.0;
        public static final double ORE_RECOVERY_PER_CHUNK_DAY = 0.0;
        public static final double HUNT_RESERVE_PER_SQUARE_BLOCK = 0.1;
        public static final double HUNT_RECOVERY_PER_SQUARE_BLOCK_DAY = 0.005;

        private Defaults() {
        }
    }

    /** Fixed Minecraft unit conversions, not gameplay tuning parameters. */
    public static final class GameUnits {
        public static final int GAME_TICKS_PER_DAY = 24_000;

        private GameUnits() {
        }
    }
}
