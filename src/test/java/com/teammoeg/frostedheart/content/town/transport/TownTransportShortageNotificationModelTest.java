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

package com.teammoeg.frostedheart.content.town.transport;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TownTransportShortageNotificationModelTest {
    private static final double EPSILON = 1.0e-12;

    @Test
    void queuesOnlyMeaningfulMorningShortages() {
        assertFalse(settle(TownTransportShortageNotificationModel.INITIAL,
                1L, 64.0, 64.0).notice().isPresent());
        assertFalse(settle(TownTransportShortageNotificationModel.INITIAL,
                1L, 64.0, Math.nextUp(64.0)).notice().isPresent());

        TownTransportShortageNotificationModel.Result result = settle(
                TownTransportShortageNotificationModel.INITIAL,
                1L, 64.0, 80.0);

        TownTransportShortageNotice notice = result.notice().orElseThrow();
        assertEquals(64.0, notice.totalCapacity(), EPSILON);
        assertEquals(80.0, notice.reservedCapacity(), EPSILON);
        assertEquals(16.0, notice.shortfall(), EPSILON);
        assertEquals(0.8, notice.effectiveRateScale(), EPSILON);
    }

    @Test
    void evaluatesEachTownDayOnceEvenIfCapacityChangesAgain() {
        TownTransportShortageNotificationModel.Result first = settle(
                TownTransportShortageNotificationModel.INITIAL,
                7L, 100.0, 80.0);
        TownTransportShortageNotificationModel.Result sameDay = settle(
                first.state(), 7L, 0.0, 80.0);
        TownTransportShortageNotificationModel.Result nextDay = settle(
                sameDay.state(), 8L, 0.0, 80.0);

        assertFalse(first.notice().isPresent());
        assertFalse(sameDay.notice().isPresent());
        assertTrue(nextDay.notice().isPresent());
        assertEquals(8L, nextDay.state().lastEvaluatedTownDay());
    }

    @Test
    void shortageRepeatsOnANewDayButRecoveryDoesNotEmit() {
        TownTransportShortageNotificationModel.Result dayOne = settle(
                TownTransportShortageNotificationModel.INITIAL,
                1L, 40.0, 80.0);
        TownTransportShortageNotificationModel.Result dayTwo = settle(
                dayOne.state(), 2L, 60.0, 80.0);
        TownTransportShortageNotificationModel.Result recovered = settle(
                dayTwo.state(), 3L, 80.0, 80.0);

        assertTrue(dayOne.notice().isPresent());
        assertTrue(dayTwo.notice().isPresent());
        assertFalse(recovered.notice().isPresent());
    }

    @Test
    void deduplicationStateIsOwnedPerTown() {
        TownTransportShortageNotificationModel.Result firstTown = settle(
                TownTransportShortageNotificationModel.INITIAL,
                12L, 0.0, 20.0);
        TownTransportShortageNotificationModel.Result secondTown = settle(
                TownTransportShortageNotificationModel.INITIAL,
                12L, 0.0, 20.0);

        assertTrue(firstTown.notice().isPresent());
        assertTrue(secondTown.notice().isPresent());
    }

    private static TownTransportShortageNotificationModel.Result settle(
            TownTransportShortageNotificationModel.State state,
            long townDay,
            double total,
            double reserved
    ) {
        return TownTransportShortageNotificationModel.onMorningSettlement(
                state, townDay, total, reserved);
    }
}
