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

import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationDailyModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownTransportCapacityModelTest {
    private static final double EPSILON = 1.0e-12;

    @Test
    void smallTownWithOneWeakWorkerProducesHalfTheStandardCapacity() {
        TownTransportCapacityModel.TownResult result = calculate(List.of(
                station(worker(0, 0, 0, 0, 0))));

        assertEquals(1, result.stationCount());
        assertEquals(1, result.workerCount());
        assertEquals(0.5, result.totalProductivity(), EPSILON);
        assertEquals(32.0, result.producedCapacity(), EPSILON);
    }

    @Test
    void standardTownScalesLinearlyWithStandardWorkers() {
        TownTransportCapacityModel.TownResult result = calculate(List.of(
                station(standardWorker(), standardWorker(), standardWorker(), standardWorker())));

        assertEquals(4.0, result.totalProductivity(), EPSILON);
        assertEquals(256.0, result.producedCapacity(), EPSILON);
    }

    @Test
    void maximumAttributesAndProficiencyReachTheConfiguredCeiling() {
        TownTransportCapacityModel.TownResult result = calculate(List.of(
                station(worker(100, 100, 100, 100, 100))));

        assertEquals(2.3, result.totalProductivity(), EPSILON);
        assertEquals(147.2, result.producedCapacity(), EPSILON);
    }

    @Test
    void multipleStationsExposePerStationAndTownTotals() {
        TownTransportCapacityModel.TownResult result = calculate(List.of(
                station(standardWorker()),
                station(standardWorker(), standardWorker()),
                station()));

        assertEquals(3, result.stationCount());
        assertEquals(3, result.workerCount());
        assertEquals(64.0, result.stations().get(0).producedCapacity(), EPSILON);
        assertEquals(128.0, result.stations().get(1).producedCapacity(), EPSILON);
        assertEquals(0.0, result.stations().get(2).producedCapacity(), EPSILON);
        assertEquals(192.0, result.producedCapacity(), EPSILON);
    }

    @Test
    void changedSimulationParameterIsVisibleInOutput() {
        TownModelParameters.TransportStationParameters defaults =
                TownModelParameters.currentDefaults().transportStation();
        TownModelParameters.TransportStationParameters tuned =
                new TownModelParameters.TransportStationParameters(
                        80.0,
                        defaults.floorBlocksPerWorkerSlot(), defaults.minimumWorkerSlots(),
                        defaults.minimumFloorAreaBlocks(), defaults.minimumInteriorVolumeBlocks(),
                        defaults.productivity());

        TownTransportCapacityModel.TownResult result = TownTransportCapacityModel.calculate(
                List.of(station(standardWorker())), tuned);

        assertEquals(80.0, result.producedCapacity(), EPSILON);
    }

    private static TownTransportCapacityModel.TownResult calculate(
            List<TownTransportCapacityModel.StationInput> stations
    ) {
        return TownTransportCapacityModel.calculate(
                stations, TownModelParameters.currentDefaults().transportStation());
    }

    private static TownTransportCapacityModel.StationInput station(
            TransportStationDailyModel.WorkerInput... workers
    ) {
        return new TownTransportCapacityModel.StationInput(List.of(workers));
    }

    private static TransportStationDailyModel.WorkerInput standardWorker() {
        return worker(50, 50, 50, 50, 0);
    }

    private static TransportStationDailyModel.WorkerInput worker(
            double health,
            double mental,
            double strength,
            double intelligence,
            double proficiency
    ) {
        return new TransportStationDailyModel.WorkerInput(
                health, mental, strength, intelligence, proficiency);
    }
}
