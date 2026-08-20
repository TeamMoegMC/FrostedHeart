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

import java.util.List;

/** Forge-independent daily transport-capacity aggregation for a whole town. */
public final class TownTransportCapacityModel {
    private TownTransportCapacityModel() {
    }

    public record StationInput(List<TransportStationDailyModel.WorkerInput> workers) {
        public StationInput {
            workers = workers == null ? List.of() : List.copyOf(workers);
        }
    }

    public record StationResult(
            int workerCount,
            double totalProductivity,
            double producedCapacity
    ) {
    }

    public record TownResult(
            int stationCount,
            int workerCount,
            double totalProductivity,
            double producedCapacity,
            List<StationResult> stations
    ) {
        public TownResult {
            stations = List.copyOf(stations);
        }
    }

    public static TownResult calculate(
            List<StationInput> stations,
            TownModelParameters.TransportStationParameters parameters
    ) {
        if (stations == null || stations.isEmpty() || parameters == null) {
            return new TownResult(0, 0, 0.0, 0.0, List.of());
        }
        TransportStationDailyModel.Parameters dailyParameters = toDailyParameters(parameters);
        List<StationResult> stationResults = stations.stream()
                .filter(station -> station != null)
                .map(station -> calculateStation(station, dailyParameters))
                .toList();
        int workerCount = 0;
        double totalProductivity = 0.0;
        double producedCapacity = 0.0;
        for (StationResult station : stationResults) {
            workerCount += station.workerCount();
            totalProductivity = saturatingAdd(totalProductivity, station.totalProductivity());
            producedCapacity = saturatingAdd(producedCapacity, station.producedCapacity());
        }
        return new TownResult(
                stationResults.size(), workerCount, totalProductivity, producedCapacity,
                stationResults);
    }

    private static StationResult calculateStation(
            StationInput station,
            TransportStationDailyModel.Parameters parameters
    ) {
        TransportStationDailyModel.DailyResult result = TransportStationDailyModel.calculate(
                station.workers(), parameters);
        return new StationResult(
                result.workerCount(), result.totalProductivity(), result.producedCapacity());
    }

    private static double saturatingAdd(double left, double right) {
        double result = left + right;
        return Double.isFinite(result) ? result : Double.MAX_VALUE;
    }

    private static TransportStationDailyModel.Parameters toDailyParameters(
            TownModelParameters.TransportStationParameters parameters
    ) {
        TownModelParameters.ResidentProductivityParameters productivity = parameters.productivity();
        return new TransportStationDailyModel.Parameters(
                parameters.capacityPerStandardWorkerDay(),
                productivity.healthWeight(), productivity.mentalWeight(),
                productivity.strengthWeight(), productivity.intelligenceWeight(),
                productivity.productivityAtAttributeZero(),
                productivity.productivityAtAttributeHundred(),
                productivity.maximumProficiency(), productivity.bonusAtMaximumProficiency(),
                productivity.minimumProductivity(), productivity.maximumProductivity());
    }
}
