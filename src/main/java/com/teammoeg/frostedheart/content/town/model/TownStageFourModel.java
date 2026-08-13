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

import com.teammoeg.frostedheart.content.climate.BlockTemperatureModel;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorHeatFieldModel;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.SphericalHeatFieldModel;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateEventModel;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClockSource;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingDailyModel;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Shared stage-4 climate, T1 sphere, and building-temperature calculations. */
public final class TownStageFourModel {
    private TownStageFourModel() {
    }

    public static ClimateSeries climateSeries(
            long seed,
            int burnInDays,
            int simulationDays,
            TownModelParameters parameters
    ) {
        TownModelParameters.ClimateParameters climate = parameters.climate();
        ClimateEventModel.Parameters eventParameters = eventParameters(climate);
        List<Track> tracks = new ArrayList<>(climate.trackCount());
        for (int index = 0; index < climate.trackCount(); index++) {
            RandomSource random = RandomSource.create(mixedSeed(seed, index));
            tracks.add(new Track(random, ClimateEventModel.generate(random, 0L, eventParameters)));
        }
        int hours = Math.multiplyExact(simulationDays, 24);
        long burnInSeconds = Math.multiplyExact((long) burnInDays, WorldClockSource.secondsPerDay);
        float[] temperatures = new float[hours];
        for (int hour = 0; hour < hours; hour++) {
            long time = burnInSeconds + (long) hour * WorldClockSource.secondsPerHour;
            float minimumNegative = 0.0F;
            float maximumPositive = 0.0F;
            for (Track track : tracks) {
                while (track.event.calmEndTime() < time) {
                    long nextStart = track.event.calmEndTime();
                    track.event = ClimateEventModel.generate(
                            track.random, nextStart, eventParameters);
                }
                float temperature = ClimateEventModel.temperatureAt(track.event, time);
                minimumNegative = Math.min(minimumNegative, temperature);
                maximumPositive = Math.max(maximumPositive, temperature);
            }
            temperatures[hour] = minimumNegative + maximumPositive;
        }
        return new ClimateSeries(seed, burnInDays, temperatures);
    }

    public static ThermalLayout analyzeLayout(
            TownStageFourScenario scenario,
            TownModelParameters parameters
    ) {
        return analyzeLayout(scenario, parameters, scenario.town().tower().overdrive());
    }

    public static ThermalLayout analyzeLayout(
            TownStageFourScenario scenario,
            TownModelParameters parameters,
            boolean overdrive
    ) {
        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        int radius = GeneratorHeatFieldModel.radiusBlocks(
                1.0, generator.baseRadiusBlocks(), generator.additionalRadiusPerLevelBlocks());
        int heat = GeneratorHeatFieldModel.temperatureCelsius(
                overdrive ? 2.0 : 1.0,
                generator.temperaturePerLevelCelsius());
        TownStageFourScenario.Position center = scenario.towerCenter();
        List<BuildingGeometry> buildings = new ArrayList<>();
        long coveredBuildingVoxels = 0L;
        for (TownStageFourScenario.ThermalBuilding building : scenario.thermalBuildings()) {
            long covered = 0L;
            for (TownStageFourScenario.VoxelBox box : building.boxes()) {
                for (int x = box.minimumX(); x < box.minimumX() + box.sizeX(); x++) {
                    for (int y = box.minimumY(); y < box.minimumY() + box.sizeY(); y++) {
                        for (int z = box.minimumZ(); z < box.minimumZ() + box.sizeZ(); z++) {
                            if (SphericalHeatFieldModel.contains(
                                    center.x(), center.y(), center.z(), radius, x, y, z)) covered++;
                        }
                    }
                }
            }
            coveredBuildingVoxels += covered;
            buildings.add(new BuildingGeometry(
                    building.id(), building.role(), building.floorAreaBlocks(),
                    building.voxelCount(), covered,
                    building.voxelCount() > 0L ? (double) covered / building.voxelCount() : 0.0));
        }
        long sphereVoxels = SphericalHeatFieldModel.latticeVolume(radius);
        long threeHighFloor = SphericalHeatFieldModel.centeredFootprintUpperBound(radius, 3);
        return new ThermalLayout(
                radius, heat, sphereVoxels, threeHighFloor,
                parameters.housing().floorBlocksPerResident() > 0.0
                        ? (long) Math.floor(threeHighFloor
                        / parameters.housing().floorBlocksPerResident()) : 0L,
                coveredBuildingVoxels,
                sphereVoxels > 0L ? (double) coveredBuildingVoxels / sphereVoxels : 0.0,
                buildings);
    }

    public static HourThermalResult evaluateHour(
            float climateTemperatureCelsius,
            boolean heatFieldActive,
            TownStageFourScenario scenario,
            TownModelParameters parameters,
            ThermalLayout layout
    ) {
        TownModelParameters.ClimateParameters climate = parameters.climate();
        float natural = BlockTemperatureModel.naturalTemperature(
                (float) scenario.location().dimensionTemperatureCelsius(),
                (float) scenario.location().biomeTemperatureCelsius(),
                0.0F,
                climateTemperatureCelsius,
                climate.blockMaximumClimateAffection());
        List<BuildingTemperature> temperatures = new ArrayList<>();
        for (BuildingGeometry geometry : layout.buildings()) {
            float coveredTemperature = heatFieldActive
                    ? BlockTemperatureModel.applyHeat(
                    natural, layout.heatTemperatureCelsius(),
                    climate.blockHeatApplicationMultiplier(), climate.absoluteZeroCelsius())
                    : natural;
            double average = geometry.coverageFraction() * coveredTemperature
                    + (1.0 - geometry.coverageFraction()) * natural;
            temperatures.add(new BuildingTemperature(
                    geometry.id(), geometry.role(), average,
                    geometry.coverageFraction(), heatFieldActive));
        }
        return new HourThermalResult(climateTemperatureCelsius, natural, temperatures);
    }

    public static TownStageThreeModel.DailyEnvironment dailyEnvironment(
            HourThermalResult thermal,
            TownStageFourScenario scenario,
            TownModelParameters parameters
    ) {
        double houseTemperature = thermal.building("house").temperatureCelsius();
        TownModelParameters.HousingParameters housing = parameters.housing();
        boolean houseWorkable = houseTemperature >= housing.minimumTemperatureCelsius()
                && houseTemperature <= housing.maximumTemperatureCelsius();

        TownStageFourScenario.ThermalBuilding huntingBuilding = scenario.building("hunt");
        double huntingTemperature = thermal.building("hunt").temperatureCelsius();
        TownModelParameters.BuildingScoringParameters scoring = parameters.buildingScoring();
        double spaceRating = TownMathFunctions.calculateSpaceRating(
                Math.toIntExact(huntingBuilding.voxelCount()),
                huntingBuilding.floorAreaBlocks(),
                scoring.space().areaCoefficient(), scoring.space().heightLogCoefficient(),
                scoring.space().heightLogOffset(), scoring.space().responseScale(),
                scoring.space().responseExponent());
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(
                huntingTemperature,
                scoring.temperature().comfortableTemperatureCelsius(),
                scoring.temperature().minimumRating(),
                scoring.temperature().sigmoidSlopePerCelsius(),
                scoring.temperature().halfPointTemperatureDifferenceCelsius());
        TownModelParameters.HuntingParameters hunting = parameters.hunting();
        double totalWeight = hunting.spaceRatingWeight() + hunting.temperatureRatingWeight();
        double rating = totalWeight > 0.0
                ? (hunting.spaceRatingWeight() * spaceRating
                + hunting.temperatureRatingWeight() * temperatureRating) / totalWeight
                : 0.0;
        return new TownStageThreeModel.DailyEnvironment(
                houseTemperature, houseWorkable, rating,
                huntingTemperature >= hunting.minimumWorkingTemperatureCelsius());
    }

    public static int huntingCapacity(
            TownStageFourScenario scenario,
            TownModelParameters parameters
    ) {
        TownStageFourScenario.ThermalBuilding building = scenario.building("hunt");
        TownModelParameters.SpaceRatingParameters space = parameters.buildingScoring().space();
        double rating = TownMathFunctions.calculateSpaceRating(
                Math.toIntExact(building.voxelCount()), building.floorAreaBlocks(),
                space.areaCoefficient(), space.heightLogCoefficient(), space.heightLogOffset(),
                space.responseScale(), space.responseExponent());
        return HuntingDailyModel.calculateCapacity(
                rating, building.floorAreaBlocks(),
                parameters.hunting().floorBlocksPerWorkerSlot(),
                parameters.hunting().minimumWorkerSlots());
    }

    public static ClimateEventModel.Parameters eventParameters(
            TownModelParameters.ClimateParameters climate
    ) {
        return new ClimateEventModel.Parameters(
                WorldClockSource.secondsPerHour, WorldClockSource.secondsPerDay,
                climate.eventChoiceRollBound(), climate.warmEventMinimumRollInclusive(),
                climate.openingWarmRollBonus(), climate.openingBiasThroughDayInclusive(),
                climate.coldBottomExtremeCelsius(), climate.coldBottomSevereCelsius(),
                climate.coldBottomStrongCelsius(), climate.coldBottomNormalCelsius(),
                climate.coldBottomWeightExtreme(), climate.coldBottomWeightSevere(),
                climate.coldBottomWeightStrong(), climate.coldBottomWeightNormal(),
                climate.eventMinimumDays(), climate.eventMaximumDaysExclusive(),
                climate.paddingMinimumHours(), climate.paddingMaximumHoursExclusive(),
                climate.calmMinimumDays(), climate.calmMaximumDaysExclusive(),
                climate.coldPreludePeakCelsius(), climate.warmPeakCelsius(),
                climate.eventNoiseStandardDeviationCelsius(), climate.warmNoiseScale());
    }

    private static long mixedSeed(long seed, int track) {
        long value = seed + 0x9E3779B97F4A7C15L * (track + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static final class Track {
        private final RandomSource random;
        private ClimateEventModel.EventDefinition event;

        private Track(RandomSource random, ClimateEventModel.EventDefinition event) {
            this.random = random;
            this.event = event;
        }
    }

    public record ClimateSeries(long seed, int burnInDays, float[] hourlyTemperatureCelsius) {
        public ClimateSeries {
            hourlyTemperatureCelsius = hourlyTemperatureCelsius.clone();
        }

        @Override
        public float[] hourlyTemperatureCelsius() {
            return hourlyTemperatureCelsius.clone();
        }

        public float temperature(int day, int hour) {
            return hourlyTemperatureCelsius[Math.addExact(Math.multiplyExact(day, 24), hour)];
        }

        public int lengthHours() {
            return hourlyTemperatureCelsius.length;
        }

        public float temperatureAtHour(int hour) {
            return hourlyTemperatureCelsius[hour];
        }
    }

    public record ThermalLayout(
            int radiusBlocks,
            int heatTemperatureCelsius,
            long sphereVoxelCount,
            long centeredThreeHighFloorUpperBoundBlocks,
            long centeredThreeHighHousingUpperBoundResidents,
            long coveredBuildingVoxels,
            double fieldUtilizationFraction,
            List<BuildingGeometry> buildings
    ) {
        public ThermalLayout {
            buildings = List.copyOf(buildings);
        }

        public BuildingGeometry building(String role) {
            return buildings.stream().filter(value -> role.equals(value.role()))
                    .findFirst().orElseThrow();
        }
    }

    public record BuildingGeometry(
            String id,
            String role,
            int floorAreaBlocks,
            long voxelCount,
            long coveredVoxelCount,
            double coverageFraction
    ) {
    }

    public record HourThermalResult(
            float climateTemperatureCelsius,
            float naturalBlockTemperatureCelsius,
            List<BuildingTemperature> buildings
    ) {
        public HourThermalResult {
            buildings = List.copyOf(buildings);
        }

        public BuildingTemperature building(String role) {
            return buildings.stream().filter(value -> role.equals(value.role()))
                    .findFirst().orElseThrow();
        }
    }

    public record BuildingTemperature(
            String id,
            String role,
            double temperatureCelsius,
            double spatialCoverageFraction,
            boolean heatFieldActive
    ) {
    }
}
