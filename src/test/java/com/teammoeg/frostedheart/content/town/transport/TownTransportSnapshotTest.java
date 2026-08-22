/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTransportSnapshotTest {
    @Test
    void codecCarriesWarehousePresentationMetadata() {
        TownTransportSnapshot snapshot = new TownTransportSnapshot(
                TownTransportState.DailyReport.EMPTY,
                128.0,
                4,
                0.075,
                List.of());

        var encoded = TownTransportSnapshot.CODEC
                .encodeStart(JsonOps.INSTANCE, snapshot).result().orElseThrow();
        assertEquals(snapshot, TownTransportSnapshot.CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
    }

    @Test
    void codecRejectsInvalidWarehousePresentationMetadata() {
        assertTrue(TownTransportSnapshot.CODEC.encodeStart(
                JsonOps.INSTANCE,
                new TownTransportSnapshot(
                        TownTransportState.DailyReport.EMPTY,
                        0.0,
                        -1,
                        0.05,
                        List.of())).error().isPresent());
        assertTrue(TownTransportSnapshot.CODEC.encodeStart(
                JsonOps.INSTANCE,
                new TownTransportSnapshot(
                        TownTransportState.DailyReport.EMPTY,
                        0.0,
                        0,
                        Double.NaN,
                        List.of())).error().isPresent());
    }
}
