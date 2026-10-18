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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TownSignalTipPresentationModelTest {
    @Test
    void sortsBySeverityShowsFiveAndReportsOverflow() {
        TownSignalTipPresentationModel.Presentation result =
                TownSignalTipPresentationModel.create(List.of(
                        notice(TownSignalEvent.Type.FOOD_RESERVE_RECOVERED),
                        notice(TownSignalEvent.Type.FOOD_RESERVE_WARNING),
                        notice(TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE),
                        notice(TownSignalEvent.Type.RESIDENT_EXIT_HEALTH),
                        notice(TownSignalEvent.Type.WORK_CAPACITY_LOST),
                        notice(TownSignalEvent.Type.FUEL_SHORTAGE),
                        notice(TownSignalEvent.Type.TOWER_SERVICE_RESTORED)));

        assertEquals(TownSignalEvent.Severity.IRREVERSIBLE, result.highestSeverity());
        assertEquals(5, result.visibleEvents().size());
        assertEquals(2, result.overflowCount());
        assertEquals(10_000, result.displayTimeMillis());
        assertTrue(result.preemptsTutorial());
        assertEquals(TownSignalEvent.Type.RESIDENT_EXIT_HEALTH,
                result.visibleEvents().get(0).type());
    }

    @Test
    void countsOnlyAppearForConcreteBuildingsAndResidents() {
        assertFalse(TownSignalTipPresentationModel.usesAffectedCount(
                TownSignalEvent.Type.TOWER_SERVICE_LOST));
        assertFalse(TownSignalTipPresentationModel.usesAffectedCount(
                TownSignalEvent.Type.FOOD_SHORTAGE));
        assertTrue(TownSignalTipPresentationModel.usesAffectedCount(
                TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE));
        assertTrue(TownSignalTipPresentationModel.usesAffectedCount(
                TownSignalEvent.Type.RESIDENT_EXIT_MENTAL));
    }

    @Test
    void dailyWorkAndHuntingSeverityMatchesRealtimeWarnings() {
        assertEquals(TownSignalEvent.Severity.WARNING,
                TownSignalEventModel.defaultSeverity(TownSignalEvent.Type.WORK_CAPACITY_LOST));
        assertEquals(TownSignalEvent.Severity.WARNING,
                TownSignalEventModel.defaultSeverity(TownSignalEvent.Type.HUNTING_TEMPERATURE_STOP));
        assertEquals(TownSignalEvent.Severity.CRITICAL,
                TownSignalEventModel.defaultSeverity(TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE));
        assertEquals(TownSignalEvent.Severity.CRITICAL,
                TownSignalEventModel.defaultSeverity(TownSignalEvent.Type.EXIT_RISK_ENTERED));
    }

    private static TownSignalNotice notice(TownSignalEvent.Type type) {
        return new TownSignalNotice(type, TownSignalEventModel.defaultSeverity(type), 1);
    }
}
