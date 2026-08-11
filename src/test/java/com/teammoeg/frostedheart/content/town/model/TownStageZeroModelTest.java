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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownStageZeroModelTest {
    private static final double EPSILON = 1.0e-7;

    @Test
    void currentSourcesProduceDocumentedStageZeroBaselines() {
        TownStageZeroModel.StageZeroMetrics metrics = analyze(TownModelParameters.currentDefaults());

        assertEquals(1.0, metrics.miningStandardWorkerSwe(), EPSILON);
        assertEquals(1.0, metrics.huntingStandardWorkerSwe(), EPSILON);
        assertEquals(1.1666667, metrics.coalPerMiningSweDay(), EPSILON);
        assertEquals(1.1973684, metrics.meatPerHuntingSweDay(), EPSILON);
        assertEquals(5.2070175, metrics.rawFoodUnitsPerHuntingSweDay(), EPSILON);
        assertEquals(22.5596491, metrics.cookedFoodUnitsPerHuntingSweDay(), EPSILON);
        assertEquals(6.5, metrics.foodUnitsPerResidentDay(), EPSILON);
        assertEquals(21.4285714, metrics.idealTowerCoalPerActiveDay(), EPSILON);
        assertEquals(10.7142857, metrics.idealTowerCokePerActiveDay(), EPSILON);
        assertEquals(18.3673469, metrics.idealTowerMiningSweUsingCoal(), EPSILON);
        assertEquals(9.1836735, metrics.idealTowerMiningSweUsingCoke(), EPSILON);
        assertEquals(1.2483154, metrics.rawDietHuntingSwePerResident(), EPSILON);
        assertEquals(0.2881250, metrics.cookedDietHuntingSwePerResident(), EPSILON);
        assertEquals(metrics.idealTowerCoalPerActiveDay(),
                metrics.currentTownBatchTowerCoalPerActiveDay(), EPSILON);
        assertEquals(metrics.idealTowerCokePerActiveDay(),
                metrics.currentTownBatchTowerCokePerActiveDay(), EPSILON);
        assertEquals(metrics.idealTowerMiningSweUsingCoal(),
                metrics.currentTownBatchMiningSweUsingCoal(), EPSILON);
        assertEquals(metrics.idealTowerMiningSweUsingCoke(),
                metrics.currentTownBatchMiningSweUsingCoke(), EPSILON);
    }

    @Test
    void miningOutputParameterDirectlyMovesCoalYield() {
        TownModelParameters defaults = TownModelParameters.currentDefaults();
        TownModelParameters.MiningParameters mining = defaults.mining();
        TownModelParameters changed = new TownModelParameters(
                new TownModelParameters.MiningParameters(
                        4.2,
                        mining.floorBlocksPerWorkerSlot(),
                        mining.minimumWorkerSlots(),
                        mining.connectionRadiusBlocks(),
                        mining.productivity(),
                        mining.assignmentBasePriority(),
                        mining.assignmentPenaltyPerWorker(),
                        mining.assignmentFillRatioBonus()),
                defaults.hunting(),
                defaults.housing(),
                defaults.residents(),
                defaults.buildingScoring(),
                defaults.terrainResources(),
                defaults.generatorT1(),
                defaults.meatFoods());

        assertEquals(1.4, analyze(changed).coalPerMiningSweDay(), EPSILON);
    }

    private static TownStageZeroModel.StageZeroMetrics analyze(TownModelParameters parameters) {
        return TownStageZeroModel.analyze(
                parameters,
                List.of(
                        new TownStageZeroModel.WeightedResource("minecraft:coal", 8),
                        new TownStageZeroModel.WeightedResource("minecraft:bone_block", 4),
                        new TownStageZeroModel.WeightedResource("frostedheart:biomass", 2),
                        new TownStageZeroModel.WeightedResource("minecraft:stone", 10)),
                List.of(
                        new TownStageZeroModel.WeightedLootEntry("minecraft:beef", 4, 1, 3),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:porkchop", 3, 1, 3),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:chicken", 2, 1, 3),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:leather", 3, 0, 2),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:bone", 3, 0, 2),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:feather", 2, 0, 2),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:rabbit_hide", 1, 1, 1),
                        new TownStageZeroModel.WeightedLootEntry("minecraft:mutton", 1, 1, 2)),
                1_600,
                3_200,
                0.0);
    }
}
