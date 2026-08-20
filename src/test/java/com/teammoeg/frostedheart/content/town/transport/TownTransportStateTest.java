/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTransportStateTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void stateCodecRoundTripsDailyAggregate() {
        TownTransportState source = new TownTransportState(
                new TownTransportState.DailyReport(true, 192.0, 0.0));

        var encoded = TownTransportState.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .result().orElseThrow();
        TownTransportState decoded = TownTransportState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(source.getDailyReport(), decoded.getDailyReport());
    }

    @Test
    void legacyTownDefaultsToEmptyTransportState() {
        TeamTownData decoded = TeamTownData.CODEC.parse(
                        JsonOps.INSTANCE, JsonParser.parseString("{\"name\":\"Legacy\"}"))
                .result().orElseThrow();

        assertEquals(TownTransportState.DailyReport.EMPTY,
                decoded.getTransportState().getDailyReport());
    }

    @Test
    void teamTownCodecPersistsTransportDailyReport() {
        TownTransportState.DailyReport report =
                new TownTransportState.DailyReport(true, 192.0, 0.0);
        TeamTownData source = new TeamTownData(
                "Transport Town",
                new TeamTownResourceHolder(),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0,
                List.of(),
                TownStaffingPlan.EMPTY,
                -1L);
        source.getTransportState().setDailyReport(report);

        var encoded = TeamTownData.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .result().orElseThrow();
        TeamTownData decoded = TeamTownData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(report, decoded.getTransportState().getDailyReport());
    }

    @Test
    void reportsSanitizeInvalidAmountsAndGuardEqualAssignments() {
        TownTransportState.DailyReport sanitized = new TownTransportState.DailyReport(
                true, Double.NaN, -5.0);
        TownTransportState state = new TownTransportState(sanitized);

        assertEquals(0.0, sanitized.totalCapacity());
        assertEquals(0.0, sanitized.reservedCapacity());
        assertFalse(state.setDailyReport(sanitized));
        assertTrue(state.setDailyReport(TownTransportState.DailyReport.EMPTY));
    }
}
