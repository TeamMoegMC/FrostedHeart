/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownSignalEventModelTest {
    @Test
    void stateDomainsKeepLatestAndSeveritySortIsStable() {
        List<TownSignalNotice> compacted = TownSignalEventModel.compactNotifications(List.of(
                event(TownSignalEvent.Type.FOOD_RESERVE_WARNING, TownSignalEvent.Severity.WARNING, 24),
                event(TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE, TownSignalEvent.Severity.CRITICAL, 2),
                event(TownSignalEvent.Type.FOOD_RESERVE_RECOVERED, TownSignalEvent.Severity.INFORMATION, 24),
                event(TownSignalEvent.Type.HUNTING_TEMPERATURE_STOP, TownSignalEvent.Severity.WARNING, 1)));

        assertEquals(List.of(
                TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE,
                TownSignalEvent.Type.HUNTING_TEMPERATURE_STOP,
                TownSignalEvent.Type.FOOD_RESERVE_RECOVERED),
                compacted.stream().map(TownSignalNotice::type).toList());
    }

    @Test
    void irreversibleExitsAccumulateByCause() {
        List<TownSignalNotice> compacted = TownSignalEventModel.compactNotifications(List.of(
                event(TownSignalEvent.Type.RESIDENT_EXIT_HEALTH, TownSignalEvent.Severity.IRREVERSIBLE, 1),
                event(TownSignalEvent.Type.RESIDENT_EXIT_MENTAL, TownSignalEvent.Severity.IRREVERSIBLE, 1),
                event(TownSignalEvent.Type.RESIDENT_EXIT_HEALTH, TownSignalEvent.Severity.IRREVERSIBLE, 2)));

        assertEquals(2, compacted.size());
        assertEquals(3, compacted.get(0).affectedCount());
        assertEquals(TownSignalEvent.Type.RESIDENT_EXIT_HEALTH, compacted.get(0).type());
        assertEquals(TownSignalEvent.Type.RESIDENT_EXIT_MENTAL, compacted.get(1).type());
    }

    @Test
    void dailyHistoryCombinesResidentsWithoutCombiningStateCrossings() {
        List<TownSignalEvent> history = new ArrayList<>();
        TownSignalEventModel.addToHistory(history, exit("first"));
        TownSignalEventModel.addToHistory(history, exit("second"));
        TownSignalEventModel.addToHistory(history,
                event(TownSignalEvent.Type.FOOD_RESERVE_WARNING, TownSignalEvent.Severity.WARNING, 24));
        TownSignalEventModel.addToHistory(history,
                event(TownSignalEvent.Type.FOOD_RESERVE_WARNING, TownSignalEvent.Severity.WARNING, 24));

        assertEquals(2, history.size());
        assertEquals(2, history.get(0).affectedCount());
        assertEquals("first,second", history.get(0).detail());
    }

    @Test
    void everyRealtimeTowerCrossingRemainsInDailyHistory() {
        List<TownSignalEvent> history = new ArrayList<>();
        TownSignalEvent lost = new TownSignalEvent(10, 5,
                TownSignalEvent.Type.TOWER_SERVICE_LOST,
                TownSignalEvent.Severity.CRITICAL, 1, "crossing");
        TownSignalEventModel.addToHistory(history, lost);
        TownSignalEventModel.addToHistory(history, lost);

        assertEquals(2, history.size());
    }

    private static TownSignalEvent event(
            TownSignalEvent.Type type,
            TownSignalEvent.Severity severity,
            int affected
    ) {
        return new TownSignalEvent(10, 0, type, severity, affected, "");
    }

    private static TownSignalEvent exit(String resident) {
        return new TownSignalEvent(10, 0, TownSignalEvent.Type.RESIDENT_EXIT_HEALTH,
                TownSignalEvent.Severity.IRREVERSIBLE, 1, resident);
    }
}
