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

import com.teammoeg.frostedheart.content.town.resource.TownFoodProcessingModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownStageOneTwoTheoryTest {
    private static final List<TownStageZeroModel.WeightedLootEntry> LOOT = List.of(
            new TownStageZeroModel.WeightedLootEntry("beef", 3, 1, 3),
            new TownStageZeroModel.WeightedLootEntry("bone", 1, 0, 2));
    private static final List<TownFoodProcessingModel.MeatDefinition> MEATS = List.of(
            new TownFoodProcessingModel.MeatDefinition(
                    "beef", "cooked_beef", 4.8, 20.8, 3000, 6000, 1, 2));

    @Test
    void exactLootMomentEnumeratesWeightsCountsAndProcessing() {
        TownStageOneTwoTheory.Moment raw =
                TownStageOneTwoTheory.lootMoment(LOOT, MEATS, 0.0);
        TownStageOneTwoTheory.Moment cooked =
                TownStageOneTwoTheory.lootMoment(LOOT, MEATS, Double.POSITIVE_INFINITY);

        assertEquals(1.5, raw.meanMeatItemsPerRoll(), 1.0e-12);
        assertEquals(1.5 * 4.8, raw.meanFoodUnitsPerRoll(), 1.0e-12);
        assertEquals(1.5 * 20.8, cooked.meanFoodUnitsPerRoll(), 1.0e-12);
    }

    @Test
    void fixedSeedSamplingConvergesWithinThreeTheoryStandardErrors() {
        int runs = 100_000;
        TownStageOneTwoTheory.Moment theory =
                TownStageOneTwoTheory.lootMoment(LOOT, MEATS, 0.0);
        SplittableRandom random = new SplittableRandom(42L);
        double sum = 0.0;
        for (int index = 0; index < runs; index++) {
            TownStageOneTwoTheory.LootSample sample =
                    TownStageOneTwoTheory.sampleLoot(LOOT, random);
            if ("beef".equals(sample.item())) sum += sample.count();
        }
        double sampleMean = sum / runs;
        double standardError = Math.sqrt(theory.varianceMeatItemsPerRoll() / runs);
        assertTrue(Math.abs(sampleMean - theory.meanMeatItemsPerRoll()) <= 3.0 * standardError);
    }

    @Test
    void currentTowerExactCycleMatchesClosedForm() {
        TownStageOneTwoTheory.TowerFuelTheory tower = TownStageOneTwoTheory.towerFuel(
                1_600,
                TownModelParameters.currentDefaults().generatorT1(),
                false,
                0.0);

        assertEquals(21.428571428571427, tower.theoryItemsPerActiveDay(), 1.0e-12);
        assertEquals(tower.theoryItemsPerActiveDay(), tower.simulationItemsPerActiveDay(), 1.0e-12);
        assertEquals(0L, tower.exactCycleRemainingProcessTicks());
    }
}
