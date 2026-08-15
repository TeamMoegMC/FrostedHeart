/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.buildings.mine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningDailyModelTest {
    @Test
    void productionAndWeightedAllocationAreExact() {
        assertEquals(3.5, MiningDailyModel.requestedOutput(1.0, 3.5), 1.0e-12);
        assertEquals(3.5 / 3.0,
                MiningDailyModel.weightedShare(3.5, 8.0, 24.0), 1.0e-12);
    }

    @Test
    void infiniteRelocationChunkCountersUseCumulativeOre() {
        assertEquals(0L, MiningDailyModel.exhaustedChunks(999.9, 1000.0));
        assertEquals(1L, MiningDailyModel.enteredChunks(999.9, 1000.0));
        assertEquals(1L, MiningDailyModel.exhaustedChunks(1000.0, 1000.0));
        assertEquals(1L, MiningDailyModel.enteredChunks(1000.0, 1000.0));
        assertEquals(0L, MiningDailyModel.enteredChunks(0.0, 1000.0));
    }
}
