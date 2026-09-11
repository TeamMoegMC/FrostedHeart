/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldClockSourceTest {
    @Test
    void freezeForwardAndBackwardUpdatesUseDayTimeSemantics() {
        WorldClockSource clock = new WorldClockSource();

        assertFalse(clock.update(20L));
        assertEquals(1L, clock.getTimeSecs());
        assertFalse(clock.update(20L));
        assertEquals(1L, clock.getTimeSecs());

        assertTrue(clock.update(4020L));
        assertEquals(201L, clock.getTimeSecs());

        assertTrue(clock.update(500L));
        assertEquals(1225L, clock.getTimeSecs());
    }

    @Test
    void backwardsJumpWrapsToTheNextMinecraftDay() {
        assertEquals(20500L, WorldClockSource.elapsedDayTimeTicks(4000L, 500L));
    }
}
