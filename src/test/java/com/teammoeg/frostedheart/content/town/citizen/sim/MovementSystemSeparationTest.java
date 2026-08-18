/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSystemSeparationTest {

    @Test
    void exactOverlapGetsStableOppositeForces() {
        long first = MovementSystem.pairSeparation(17, 4096, -2048, 42, 4096, -2048);
        long repeated = MovementSystem.pairSeparation(17, 4096, -2048, 42, 4096, -2048);
        long swapped = MovementSystem.pairSeparation(42, 4096, -2048, 17, 4096, -2048);

        assertNotEquals(0L, first);
        assertEquals(first, repeated);
        assertEquals(-x(first), x(swapped));
        assertEquals(-z(first), z(swapped));
    }

    @Test
    void pairContributionIsBoundedAndStopsAtRadius() {
        long close = MovementSystem.pairSeparation(1, 0, 0, 2, 1, 1);
        long outside = MovementSystem.pairSeparation(
                1, 0, 0, 2, 1537, 0);

        assertTrue(Math.abs(x(close)) <= MovementSystem.SEP_MAX);
        assertTrue(Math.abs(z(close)) <= MovementSystem.SEP_MAX);
        assertEquals(0L, outside);
    }

    @Test
    void everyCoincidentPairGetsAForce() {
        for (int id = 2; id < 64; id++) {
            assertNotEquals(0L, MovementSystem.pairSeparation(1, 100, 200, id, 100, 200));
        }
    }

    private static int x(long packed) {
        return (int) (packed >> 32);
    }

    private static int z(long packed) {
        return (int) packed;
    }
}
