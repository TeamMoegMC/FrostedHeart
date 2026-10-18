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

package com.teammoeg.frostedheart.content.town.observation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTowerTipThrottleTest {
    @Test
    void firstLossIsImmediateAndFinalRecoveryIsDeferred() {
        TownTowerTipThrottle.Result lost = TownTowerTipThrottle.onCrossing(
                TownTowerTipThrottle.INITIAL, 100, false);
        assertEquals(TownSignalEvent.Type.TOWER_SERVICE_LOST, lost.emitted().get(0).type());

        TownTowerTipThrottle.Result recovered = TownTowerTipThrottle.onCrossing(lost.state(), 120, true);
        assertTrue(recovered.emitted().isEmpty());
        assertTrue(TownTowerTipThrottle.onTick(recovered.state(), 300).emitted().isEmpty());

        TownTowerTipThrottle.Result due = TownTowerTipThrottle.onTick(recovered.state(), 301);
        assertEquals(TownSignalEvent.Type.TOWER_SERVICE_RESTORED, due.emitted().get(0).type());
    }

    @Test
    void flappingBackToNotifiedStateProducesNoSecondTip() {
        TownTowerTipThrottle.Result lost = TownTowerTipThrottle.onCrossing(
                TownTowerTipThrottle.INITIAL, 100, false);
        TownTowerTipThrottle.Result recovered = TownTowerTipThrottle.onCrossing(lost.state(), 120, true);
        TownTowerTipThrottle.Result lostAgain = TownTowerTipThrottle.onCrossing(recovered.state(), 140, false);

        TownTowerTipThrottle.Result due = TownTowerTipThrottle.onTick(lostAgain.state(), 301);
        assertTrue(due.emitted().isEmpty());
        assertEquals(Boolean.FALSE, due.state().lastNotifiedActive());
    }

    @Test
    void aNewCrossingAfterWindowIsImmediate() {
        TownTowerTipThrottle.Result lost = TownTowerTipThrottle.onCrossing(
                TownTowerTipThrottle.INITIAL, 100, false);
        TownTowerTipThrottle.Result recovered = TownTowerTipThrottle.onCrossing(lost.state(), 301, true);

        assertEquals(TownSignalEvent.Type.TOWER_SERVICE_RESTORED, recovered.emitted().get(0).type());
    }

    @Test
    void expiredDeferredFlapEndingAtNotifiedStateDoesNotRepeat() {
        TownTowerTipThrottle.Result lost = TownTowerTipThrottle.onCrossing(
                TownTowerTipThrottle.INITIAL, 100, false);
        TownTowerTipThrottle.Result recovered = TownTowerTipThrottle.onCrossing(lost.state(), 120, true);
        TownTowerTipThrottle.Result finalLoss = TownTowerTipThrottle.onCrossing(
                recovered.state(), 301, false);

        assertTrue(finalLoss.emitted().isEmpty());
        assertEquals(Boolean.FALSE, finalLoss.state().lastNotifiedActive());
    }
}
