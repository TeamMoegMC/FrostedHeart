/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftThermalInputRefreshBudgetTest {
    @Test
    void staleRefreshEntriesStillConsumeThePerTickPollBudget() {
        PriorityQueue<MinecraftThermalInput.PageEnvironmentRefresh> queue =
                new PriorityQueue<>(MinecraftThermalInput.PAGE_REFRESH_ORDER);
        for (int index = 0; index < 100; index++) {
            queue.add(new MinecraftThermalInput.PageEnvironmentRefresh(
                    index, index + 1L, 5L));
        }
        MinecraftThermalInput.PageEnvironmentRefresh[] batch =
                new MinecraftThermalInput.PageEnvironmentRefresh[16];

        int polled = MinecraftThermalInput.pollDuePageEnvironmentRefreshes(
                queue, 5L, batch);

        assertEquals(16, polled);
        assertEquals(84, queue.size());
    }
}
