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
 *
 */

package com.teammoeg.frostedresearch.network;

import com.teammoeg.frostedresearch.gui.drawdesk.game.CardPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FHDrawingDeskOperationPacketTest {
    @Test
    void authorizationRequiresEveryServerSideCheck() {
        assertTrue(FHDrawingDeskOperationPacket.passesAuthorizationChecks(
                true, true, true, true, true));
        assertFalse(FHDrawingDeskOperationPacket.passesAuthorizationChecks(
                false, true, true, true, true));
        assertFalse(FHDrawingDeskOperationPacket.passesAuthorizationChecks(
                true, false, true, true, true));
        assertFalse(FHDrawingDeskOperationPacket.passesAuthorizationChecks(
                true, true, false, true, true));
        assertFalse(FHDrawingDeskOperationPacket.passesAuthorizationChecks(
                true, true, true, false, true));
        assertFalse(FHDrawingDeskOperationPacket.passesAuthorizationChecks(
                true, true, true, true, false));
    }

    @Test
    void operationShapeRejectsMissingOrOutOfBoundsCardPositions() {
        CardPos valid = CardPos.valueOf(0, 8);
        CardPos invalid = CardPos.valueOf(-1, 9);

        assertTrue(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 0, null, null));
        assertTrue(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 1, valid, null));
        assertTrue(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 2, valid, valid));
        assertTrue(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 3, null, null));

        assertFalse(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 1, null, null));
        assertFalse(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 1, invalid, null));
        assertFalse(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 2, valid, invalid));
        assertFalse(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 3, valid, null));
        assertFalse(FHDrawingDeskOperationPacket.isOperationShapeValid((byte) 4, null, null));
    }

    @Test
    void cardPositionFactoryHandlesNegativeCoordinatesWithoutIndexingTheCache() {
        assertFalse(CardPos.valueOf(-1, 0).isWithinBoard());
        assertFalse(CardPos.valueOf(Byte.MIN_VALUE, Byte.MAX_VALUE).isWithinBoard());
    }
}
