/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSystemCollisionTest {

    @Test
    void positiveWallContactKeepsTheWholeBodyOutside() {
        int wallCell = 1;

        int center = MovementSystem.clampAtBlockedCell(wallCell, 1);
        int wallMin = wallCell * CitizenState.FIXED_SCALE;

        assertEquals(wallMin - CitizenState.COLLISION_RADIUS, center);
        assertTrue(center + CitizenState.COLLISION_RADIUS <= wallMin);
    }

    @Test
    void negativeWallContactKeepsTheWholeBodyOutside() {
        int wallCell = 0;

        int center = MovementSystem.clampAtBlockedCell(wallCell, -1);
        int wallMax = (wallCell + 1) * CitizenState.FIXED_SCALE;

        assertEquals(wallMax + CitizenState.COLLISION_RADIUS, center);
        assertTrue(center - CitizenState.COLLISION_RADIUS >= wallMax);
    }

    @Test
    void wallClampIsSymmetricAcrossNegativeCoordinates() {
        int wallCell = -2;

        assertEquals(wallCell * CitizenState.FIXED_SCALE - CitizenState.COLLISION_RADIUS,
                MovementSystem.clampAtBlockedCell(wallCell, 1));
        assertEquals((wallCell + 1) * CitizenState.FIXED_SCALE + CitizenState.COLLISION_RADIUS,
                MovementSystem.clampAtBlockedCell(wallCell, -1));
    }

    @Test
    void leadingEdgeTouchesWithoutEnteringTheWallCell() {
        int positiveContact = CitizenState.FIXED_SCALE - CitizenState.COLLISION_RADIUS;
        int negativeContact = CitizenState.FIXED_SCALE + CitizenState.COLLISION_RADIUS;

        assertEquals(0, MovementSystem.leadingOccupiedCell(positiveContact, 1));
        assertEquals(1, MovementSystem.leadingOccupiedCell(positiveContact + 1, 1));
        assertEquals(1, MovementSystem.leadingOccupiedCell(negativeContact, -1));
        assertEquals(0, MovementSystem.leadingOccupiedCell(negativeContact - 1, -1));
    }
}
