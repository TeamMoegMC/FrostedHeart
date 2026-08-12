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

package com.teammoeg.frostedheart.content.climate.block.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorFuelModelTest {
    @Test
    void effectiveDurationUsesDecimalFloorSemantics() {
        assertEquals(1_120, GeneratorFuelModel.effectiveFuelProcessTicks(1_600, 0.7, 0.0));
        assertEquals(1_280, GeneratorFuelModel.effectiveFuelProcessTicks(1_600, 0.7, 0.1));
        assertEquals(1_440, GeneratorFuelModel.effectiveFuelProcessTicks(1_600, 0.7, 0.2));
        assertEquals(2_880, GeneratorFuelModel.effectiveFuelProcessTicks(3_200, 0.7, 0.2));
        assertEquals(1, GeneratorFuelModel.effectiveFuelProcessTicks(3, 0.5, 0.0));
    }

    @Test
    void refillPredicateSpendsExactBalanceBeforeLoadingFuel() {
        assertFalse(GeneratorFuelModel.shouldLoadNextFuel(21, 20));
        assertFalse(GeneratorFuelModel.shouldLoadNextFuel(20, 20));
        assertTrue(GeneratorFuelModel.shouldLoadNextFuel(19, 20));
        assertEquals(1_139, GeneratorFuelModel.addFuelProcessTicks(19, 1_120));
    }

    @Test
    void carriedTownBatchRateMatchesIdealRate() {
        assertEquals(21.428571428571427,
                GeneratorFuelModel.idealFuelItemsPerDay(1_120, 1, 24_000), 1.0e-12);
        assertEquals(21.428571428571427,
                GeneratorFuelModel.currentTownBatchFuelItemsPerDay(1_120, 1, 20, 24_000),
                1.0e-12);
    }

    @Test
    void oneTickAndTwentyTickUpdatesHaveIdenticalFuelAccounting() {
        for (int fuelDuration : new int[]{1, 19, 20, 21, 1_120, 1_123, 1_440}) {
            FuelState perTick = runForGameTicks(fuelDuration, 24_000, 1);
            FuelState townBatch = runForGameTicks(fuelDuration, 24_000, 20);
            assertEquals(perTick, townBatch, "fuelDuration=" + fuelDuration);
        }
    }

    @Test
    void finiteExactCycleReturnsToZeroBalance() {
        GeneratorFuelModel.FuelSettlement settlement =
                GeneratorFuelModel.settleProcessDemand(1_120, 20, 24_000L * 7L, 0L);

        assertEquals(150L, settlement.loadedFuelItems());
        assertEquals(0L, settlement.remainingProcessTicks());
        assertEquals(24_000L * 7L, settlement.consumedProcessTicks());
    }

    private static FuelState runForGameTicks(int fuelDuration, int gameTicks, int batchTicks) {
        int remaining = 0;
        int loadedItems = 0;
        for (int elapsed = 0; elapsed < gameTicks; elapsed += batchTicks) {
            int requested = Math.min(batchTicks, gameTicks - elapsed);
            while (GeneratorFuelModel.shouldLoadNextFuel(remaining, requested)) {
                remaining = GeneratorFuelModel.addFuelProcessTicks(remaining, fuelDuration);
                loadedItems++;
            }
            remaining -= requested;
        }
        return new FuelState(loadedItems, remaining);
    }

    private record FuelState(int loadedItems, int remainingProcessTicks) {
    }
}
