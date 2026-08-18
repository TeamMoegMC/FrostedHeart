/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.observation;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TownHistoryModelTest {
    @Test
    void duplicateNetworkEntryReplacesAndRetentionKeepsNewestDays() {
        TownHistoryEntry dayOne = entry(1, 10);
        TownHistoryEntry replacement = entry(1, 11);
        List<TownHistoryEntry> history = TownHistoryModel.upsert(List.of(dayOne), replacement, 2);
        assertEquals(1, history.size());
        assertEquals(11, history.get(0).population());
        history = TownHistoryModel.upsert(history, entry(2, 12), 2);
        history = TownHistoryModel.upsert(history, entry(3, 13), 2);
        assertEquals(List.of(2L, 3L), history.stream().map(TownHistoryEntry::day).toList());
    }

    @Test
    void everySettlementAdvancesTownDayWithoutChangingWorldTime() {
        assertEquals(100L, TownHistoryModel.nextSettlementDay(List.of(), 100L));
        List<TownHistoryEntry> first = List.of(entry(100L, 10));
        assertEquals(101L, TownHistoryModel.nextSettlementDay(first, 100L));
        List<TownHistoryEntry> manuallyAdvanced = List.of(entry(100L, 10), entry(101L, 10));
        assertEquals(102L, TownHistoryModel.nextSettlementDay(manuallyAdvanced, 100L));
        assertEquals(105L, TownHistoryModel.nextSettlementDay(manuallyAdvanced, 105L));
    }

    @Test
    void oldCodecDataKeepsOperationalMetricsUnavailable() {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("day", 5);
        legacy.addProperty("population", 8);
        legacy.addProperty("avgHealth", 50.0);
        legacy.addProperty("avgMental", 50.0);
        legacy.addProperty("buildings", 3);
        TownHistoryEntry decoded = TownHistoryEntry.CODEC.parse(JsonOps.INSTANCE, legacy)
                .getOrThrow(false, message -> fail(message));
        assertFalse(decoded.operational().foodReserveDays().available());
        assertFalse(decoded.operational().minimumHouseTemperatureCelsius().available());
        assertFalse(decoded.nutrition().available());
    }

    @Test
    void newCodecRoundTripPreservesAvailabilityAndTowerState() {
        TownOperationalHistory operational = new TownOperationalHistory(
                new TownOperationalHistory.Metric(true, 6.5),
                new TownOperationalHistory.Metric(true, 4.0),
                new TownOperationalHistory.Metric(true, 10.0),
                TownOperationalHistory.Metric.UNAVAILABLE,
                0, 1,
                new TownOperationalHistory.Tower(TownOperationalStatus.TowerKind.T1,
                        true, true, false, true, 0.5));
        TownNutritionHistory nutrition = new TownNutritionHistory(true,
                70, 55, 68, 54, 72, 60, 65, 40);
        TownHistoryEntry source = new TownHistoryEntry(2, 8, 50, 50, 4,
                40, 30, 42, 32, 1, 0, true, -1, List.of(), operational, nutrition);
        var encoded = TownHistoryEntry.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .getOrThrow(false, message -> fail(message));
        TownHistoryEntry decoded = TownHistoryEntry.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, message -> fail(message));
        assertEquals(source, decoded);
    }

    @Test
    void previousHuntingTemperatureFieldMigratesToGenericBuildingMinimum() {
        JsonObject metric = new JsonObject();
        metric.addProperty("available", true);
        metric.addProperty("value", -7.5);
        JsonObject legacy = new JsonObject();
        legacy.add("minimumHuntingTemperatureCelsius", metric);
        TownOperationalHistory decoded = TownOperationalHistory.CODEC.parse(JsonOps.INSTANCE, legacy)
                .getOrThrow(false, message -> fail(message));
        assertTrue(decoded.minimumBuildingTemperatureCelsius().available());
        assertEquals(-7.5, decoded.minimumBuildingTemperatureCelsius().value(), 1.0e-12);
    }

    private static TownHistoryEntry entry(long day, int population) {
        return new TownHistoryEntry(day, population, 50, 50, 1);
    }
}
