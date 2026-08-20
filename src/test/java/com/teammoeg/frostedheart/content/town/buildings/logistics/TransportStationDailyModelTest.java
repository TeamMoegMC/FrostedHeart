/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportStationDailyModelTest {
    private static final double EPSILON = 1.0e-12;

    @Test
    void h03StandardAndMaximumWorkersMatchExpectedOutput() {
        TransportStationDailyModel.Parameters parameters = defaults();

        TransportStationDailyModel.DailyResult standard = TransportStationDailyModel.calculate(
                List.of(new TransportStationDailyModel.WorkerInput(50, 50, 50, 50, 0)),
                parameters);
        TransportStationDailyModel.DailyResult maximum = TransportStationDailyModel.calculate(
                List.of(new TransportStationDailyModel.WorkerInput(100, 100, 100, 100, 100)),
                parameters);

        assertEquals(1, standard.workerCount());
        assertEquals(1.0, standard.totalProductivity(), EPSILON);
        assertEquals(64.0, standard.producedCapacity(), EPSILON);
        assertEquals(2.3, maximum.totalProductivity(), EPSILON);
        assertEquals(147.2, maximum.producedCapacity(), EPSILON);
    }

    @Test
    void h03WeightsAffectTheNormalizedAttributeAverage() {
        TransportStationDailyModel.Parameters parameters = defaults();

        assertEquals(0.85, TransportStationDailyModel.residentProductivity(
                new TransportStationDailyModel.WorkerInput(100, 0, 0, 0, 0), parameters), EPSILON);
        assertEquals(0.65, TransportStationDailyModel.residentProductivity(
                new TransportStationDailyModel.WorkerInput(0, 100, 0, 0, 0), parameters), EPSILON);
        assertEquals(0.80, TransportStationDailyModel.residentProductivity(
                new TransportStationDailyModel.WorkerInput(0, 0, 100, 0, 0), parameters), EPSILON);
        assertEquals(0.70, TransportStationDailyModel.residentProductivity(
                new TransportStationDailyModel.WorkerInput(0, 0, 0, 100, 0), parameters), EPSILON);
    }

    @Test
    void invalidInputsNeverProduceNegativeOrNonFiniteResults() {
        TransportStationDailyModel.Parameters invalid = new TransportStationDailyModel.Parameters(
                Double.POSITIVE_INFINITY,
                Double.NaN, -1.0, Double.POSITIVE_INFINITY, 0.0,
                Double.NaN, -1.0, Double.NEGATIVE_INFINITY, Double.NaN,
                -1.0, Double.POSITIVE_INFINITY);

        TransportStationDailyModel.DailyResult result = TransportStationDailyModel.calculate(
                List.of(new TransportStationDailyModel.WorkerInput(
                        Double.NaN, Double.POSITIVE_INFINITY, -50.0, 50.0, Double.NaN)),
                invalid);

        assertTrue(Double.isFinite(result.totalProductivity()));
        assertTrue(Double.isFinite(result.producedCapacity()));
        assertTrue(result.totalProductivity() >= 0.0);
        assertTrue(result.producedCapacity() >= 0.0);
        assertEquals(0.0, TransportStationDailyModel.producedCapacity(Double.NaN, 64.0), EPSILON);
        assertEquals(0.0, TransportStationDailyModel.producedCapacity(1.0, -64.0), EPSILON);
    }

    @Test
    void nullAndEmptyWorkerCollectionsSettleToZero() {
        assertEquals(new TransportStationDailyModel.DailyResult(0, 0.0, 0.0),
                TransportStationDailyModel.calculate(null, defaults()));
        assertEquals(new TransportStationDailyModel.DailyResult(0, 0.0, 0.0),
                TransportStationDailyModel.calculate(List.of(), defaults()));
    }

    @Test
    void zeroOutputKeepsWorkerAccountingButProducesNoCapacity() {
        TransportStationDailyModel.Parameters defaults = defaults();
        TransportStationDailyModel.Parameters disabled = new TransportStationDailyModel.Parameters(
                0.0,
                defaults.healthWeight(), defaults.mentalWeight(), defaults.strengthWeight(),
                defaults.intelligenceWeight(), defaults.productivityAtAttributeZero(),
                defaults.productivityAtAttributeHundred(), defaults.maximumProficiency(),
                defaults.bonusAtMaximumProficiency(), defaults.minimumProductivity(),
                defaults.maximumProductivity());

        TransportStationDailyModel.DailyResult result = TransportStationDailyModel.calculate(
                List.of(new TransportStationDailyModel.WorkerInput(50, 50, 50, 50, 0)),
                disabled);

        assertEquals(1, result.workerCount());
        assertEquals(1.0, result.totalProductivity(), EPSILON);
        assertEquals(0.0, result.producedCapacity(), EPSILON);
    }

    private static TransportStationDailyModel.Parameters defaults() {
        return new TransportStationDailyModel.Parameters(
                TownModelParameters.Defaults.TRANSPORT_STATION_CAPACITY_PER_STANDARD_WORKER_DAY,
                TownModelParameters.Defaults.TRANSPORT_STATION_HEALTH_WEIGHT,
                TownModelParameters.Defaults.TRANSPORT_STATION_MENTAL_WEIGHT,
                TownModelParameters.Defaults.TRANSPORT_STATION_STRENGTH_WEIGHT,
                TownModelParameters.Defaults.TRANSPORT_STATION_INTELLIGENCE_WEIGHT,
                TownModelParameters.Defaults.TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
                TownModelParameters.Defaults.TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
                TownModelParameters.Defaults.TRANSPORT_STATION_MAXIMUM_PROFICIENCY,
                TownModelParameters.Defaults.TRANSPORT_STATION_BONUS_AT_MAXIMUM_PROFICIENCY,
                TownModelParameters.Defaults.TRANSPORT_STATION_MINIMUM_PRODUCTIVITY,
                TownModelParameters.Defaults.TRANSPORT_STATION_MAXIMUM_PRODUCTIVITY);
    }
}
