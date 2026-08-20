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

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import com.teammoeg.frostedheart.content.town.model.TownStageOneTwoTheory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseBuildingDailySettlementTest {
    @Test
    void coldHouseStillRunsResidentSettlement() {
        assertFalse(HouseDailyModel.isBuildingWorkable(true, 4, 8, false, 4, 8));
        assertTrue(HouseDailyModel.shouldRunDailySettlement(true, 4, 8, 4, 8));
    }

    @Test
    void structurallyInvalidHouseDoesNotRunSettlement() {
        assertFalse(HouseDailyModel.isBuildingWorkable(false, 4, 8, false, 4, 8));
        assertFalse(HouseDailyModel.shouldRunDailySettlement(false, 4, 8, 4, 8));
    }

    @Test
    void capacityMatchesEffectiveAreaAndBeds() {
        assertEquals(3, HouseDailyModel.calculateCapacity(0.75, 16, 4.0, 8));
        assertEquals(1, HouseDailyModel.calculateCapacity(0.75, 16, 4.0, 1));
    }

    @Test
    void defaultControlledSettlementMatchesClosedFormRegression() {
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        double requiredFood = parameters.housing().foodConsumptionPerResidentDay();
        HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        1,
                        requiredFood,
                        24.0,
                        16,
                        48,
                        0.75),
                TownStageOneTwoTheory.houseParameters(parameters));
        HouseDailyModel.ResidentEffects effects = HouseDailyModel.calculateResidentEffects(
                50.0,
                50.0,
                report.foodSatisfaction(),
                1.0,
                1.0,
                report.effectiveTemperature(),
                report.temperatureRating(),
                report.comfortRating(),
                TownStageOneTwoTheory.residentEffectParameters(parameters));

        assertEquals(0.9990137900379085, effects.healthDelta(), 1.0e-12);
        assertEquals(0.6082182608716355, effects.mentalDelta(), 1.0e-12);
        assertEquals(0.0, effects.foodStress(), 1.0e-12);
        assertEquals(0.0, effects.temperatureStress(), 1.0e-12);
    }

    @Test
    void foodDeficitPenaltyIsConvexAndBounded() {
        assertEquals(0.0, HouseDailyModel.calculateFoodDeficitStress(1.0, 2.0), 1.0e-12);
        assertEquals(0.01, HouseDailyModel.calculateFoodDeficitStress(0.9, 2.0), 1.0e-12);
        assertEquals(0.25, HouseDailyModel.calculateFoodDeficitStress(0.5, 2.0), 1.0e-12);
        assertEquals(1.0, HouseDailyModel.calculateFoodDeficitStress(0.0, 2.0), 1.0e-12);
    }

    @Test
    void temperatureStressUsesInclusiveSafeRangeAndBoundedQuadraticTails() {
        assertEquals(0.0, temperatureStress(0.0), 1.0e-12);
        assertEquals(0.0, temperatureStress(24.0), 1.0e-12);
        assertEquals(0.0, temperatureStress(40.0), 1.0e-12);
        assertEquals(0.25, temperatureStress(-10.0), 1.0e-12);
        assertEquals(0.25, temperatureStress(50.0), 1.0e-12);
        assertEquals(1.0, temperatureStress(-20.0), 1.0e-12);
        assertEquals(1.0, temperatureStress(60.0), 1.0e-12);
        assertEquals(1.0, temperatureStress(-100.0), 1.0e-12);
    }

    @Test
    void smallFoodDeficitIsTolerableButLargeDeficitIsDangerous() {
        HouseDailyModel.ResidentEffects eightyPercent = controlledEffects(24.0, 0.8);
        HouseDailyModel.ResidentEffects fortyPercent = controlledEffects(24.0, 0.4);

        assertEquals(0.04, eightyPercent.foodStress(), 1.0e-12);
        assertEquals(0.32, eightyPercent.healthFoodPenalty(), 1.0e-12);
        assertTrue(eightyPercent.healthDelta() > 0.0);
        assertTrue(eightyPercent.mentalDelta() > 0.0);
        assertTrue(fortyPercent.healthDelta() < 0.0);
        assertTrue(fortyPercent.mentalDelta() < 0.0);
    }

    @Test
    void coldAndHotHousesDirectlyDamageFullyFedResidents() {
        HouseDailyModel.ResidentEffects cold = controlledEffects(-10.0, 1.0);
        HouseDailyModel.ResidentEffects hot = controlledEffects(50.0, 1.0);

        assertEquals(0.25, cold.temperatureStress(), 1.0e-12);
        assertEquals(2.5, cold.healthTemperaturePenalty(), 1.0e-12);
        assertEquals(1.25, cold.mentalTemperaturePenalty(), 1.0e-12);
        assertTrue(cold.healthDelta() < 0.0);
        assertTrue(cold.mentalDelta() < 0.0);
        assertEquals(cold.temperatureStress(), hot.temperatureStress(), 1.0e-12);
        assertTrue(hot.healthDelta() < 0.0);
        assertTrue(hot.mentalDelta() < 0.0);
    }

    private static double temperatureStress(double temperatureCelsius) {
        TownModelParameters.HousingParameters housing = TownModelParameters.currentDefaults().housing();
        return HouseDailyModel.calculateTemperatureStress(
                temperatureCelsius,
                housing.minimumTemperatureCelsius(),
                housing.maximumTemperatureCelsius(),
                housing.temperatureFullStressDistanceCelsius(),
                housing.temperatureStressPenaltyExponent());
    }

    private static HouseDailyModel.ResidentEffects controlledEffects(
            double temperatureCelsius,
            double foodSatisfaction
    ) {
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        double requiredFood = parameters.housing().foodConsumptionPerResidentDay();
        double consumedFood = requiredFood * foodSatisfaction;
        HouseDailyModel.SettlementReport report = HouseDailyModel.evaluateSettlement(
                new HouseDailyModel.SettlementInput(
                        1,
                        consumedFood,
                        temperatureCelsius,
                        16,
                        48,
                        0.75),
                TownStageOneTwoTheory.houseParameters(parameters));
        return HouseDailyModel.calculateResidentEffects(
                50.0,
                50.0,
                report.foodSatisfaction(),
                1.0,
                1.0,
                report.effectiveTemperature(),
                report.temperatureRating(),
                report.comfortRating(),
                TownStageOneTwoTheory.residentEffectParameters(parameters));
    }
}
