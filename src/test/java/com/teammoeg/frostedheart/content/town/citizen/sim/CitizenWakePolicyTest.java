/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitizenWakePolicyTest {

    private static final long UUID_HI = 0x0123456789ABCDEFL;
    private static final long UUID_LO = 0xFEDCBA9876543210L;

    @Test
    void wakeOffsetIsStableAndBounded() {
        assertEquals(152, BehaviorSystem.wakeOffset(UUID_HI, UUID_LO, 42));

        for (long day = -100; day <= 100; day++) {
            int offset = BehaviorSystem.wakeOffset(UUID_HI, UUID_LO, day);
            assertTrue(offset >= 0 && offset < BehaviorSystem.WAKE_WINDOW);
            assertEquals(offset, BehaviorSystem.wakeOffset(UUID_HI, UUID_LO, day));
        }
    }

    @Test
    void citizenWakesAtItsMorningBoundary() {
        long dayStart = 42L * 24000L;

        assertFalse(BehaviorSystem.shouldWake(UUID_HI, UUID_LO, dayStart + 151));
        assertTrue(BehaviorSystem.shouldWake(UUID_HI, UUID_LO, dayStart + 152));
        assertTrue(BehaviorSystem.shouldWake(UUID_HI, UUID_LO, dayStart + 10000));
        assertFalse(BehaviorSystem.shouldWake(UUID_HI, UUID_LO, dayStart + 13000));
        assertFalse(BehaviorSystem.shouldWake(UUID_HI, UUID_LO, dayStart + 23500));
    }
}
