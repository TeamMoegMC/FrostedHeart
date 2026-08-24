/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void reservationCodecPersistsInputsAndRecalculatesTheDerivedCapacity() {
        TransportEndpointId endpoint = endpoint(8, 64, 4);
        TransportReservation active = reservation(20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);
        TransportReservation disabled = reservation(0, 0.0, 0.0, TransportAdmissionStatus.DISABLED);
        TransportReservation unavailable = reservation(20, 0.0, 0.0, TransportAdmissionStatus.UNAVAILABLE);
        TransportReservation paused = reservation(
                TransportEndpointKind.P2P_DIRECT_LINK,
                20, 8.0, 0.0, TransportAdmissionStatus.REDSTONE_PAUSED);
        TownTransportState source = new TownTransportState(TownTransportState.DailyReport.EMPTY, Map.of(
                endpoint, active,
                endpoint(0, 64, 0), disabled,
                endpoint(1, 64, 0), unavailable,
                endpoint(2, 64, 0), paused));

        var encoded = TownTransportState.CODEC.encodeStart(JsonOps.INSTANCE, source)
                .result().orElseThrow();
        assertFalse(encoded.toString().contains("reservedTransportCapacity"));
        assertTrue(encoded.toString().contains("rateItemsPerSecond"));
        assertFalse(encoded.toString().contains("requestedRateItemsPerSecond"));
        assertFalse(encoded.toString().contains("activeRateItemsPerSecond"));
        assertFalse(encoded.toString().contains("boundWarehouseCorePos"));
        TownTransportState decoded = TownTransportState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(4, decoded.getReservations().size());
        assertEquals(0.0, decoded.getReservation(endpoint).reservedTransportCapacity());
        assertTrue(decoded.recalculateReservedCapacities(
                TownModelParameters.currentDefaults().transportConsumers()));
        assertEquals(28.0, decoded.getReservation(endpoint).reservedTransportCapacity());
        assertEquals(28.0, decoded.getReservedTransportCapacity());
        assertEquals(0.0, decoded.getReservation(endpoint(1, 64, 0)).reservedTransportCapacity());
        TransportReservation decodedPaused = decoded.getReservation(endpoint(2, 64, 0));
        assertEquals(TransportAdmissionStatus.REDSTONE_PAUSED, decodedPaused.admissionStatus());
        assertEquals(20, decodedPaused.rateItemsPerSecond());
        assertEquals(8.0, decodedPaused.scaleMetric());
        assertEquals(0.0, decodedPaused.reservedTransportCapacity());
    }

    @Test
    void reservationStateInvariantsRejectAmbiguousCombinations() {
        assertThrows(IllegalArgumentException.class, () -> reservation(
                0, 1.0, 0.0, TransportAdmissionStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                1, 1.0, 0.0, TransportAdmissionStatus.DISABLED));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                0, 1.0, 1.0, TransportAdmissionStatus.DISABLED));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                20, 1.0, 0.0, TransportAdmissionStatus.UNAVAILABLE));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                20, 0.0, 1.0, TransportAdmissionStatus.UNAVAILABLE));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                TransportEndpointKind.P2P_DIRECT_LINK,
                0, 8.0, 0.0, TransportAdmissionStatus.REDSTONE_PAUSED));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                TransportEndpointKind.P2P_DIRECT_LINK,
                20, 8.0, 1.0, TransportAdmissionStatus.REDSTONE_PAUSED));
        assertThrows(IllegalArgumentException.class, () -> reservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                20, 8.0, 0.0, TransportAdmissionStatus.REDSTONE_PAUSED));

        TransportReservation unavailable = reservation(
                2_000, 0.0, 0.0, TransportAdmissionStatus.UNAVAILABLE);
        assertEquals(2_000, unavailable.rateItemsPerSecond());
        assertEquals(0.0, unavailable.recalculateReservedCapacity(
                TownModelParameters.currentDefaults().transportConsumers()).reservedTransportCapacity());
    }

    @Test
    void unknownAdmissionStatusDropsOnlyTheDamagedEntry() {
        TransportEndpointId damagedEndpoint = endpoint(0, 64, 0);
        TransportEndpointId validEndpoint = endpoint(1, 64, 0);
        JsonObject damaged = encodedEntry(damagedEndpoint,
                reservation(20, 1.0, 0.0, TransportAdmissionStatus.ACTIVE));
        damaged.getAsJsonObject("reservation").addProperty("admissionStatus", "UNKNOWN");
        JsonArray entries = new JsonArray();
        entries.add(damaged);
        entries.add(encodedEntry(validEndpoint,
                reservation(0, 1.0, 0.0, TransportAdmissionStatus.DISABLED)));

        TownTransportState decoded = TownTransportState.CODEC.parse(
                JsonOps.INSTANCE, stateJson(entries)).result().orElseThrow();
        assertEquals(Map.of(validEndpoint, decoded.getReservation(validEndpoint)), decoded.getReservations());
    }

    @Test
    void codecDropsEntireDuplicateEndpointGroupsAndRetainsOtherValidEntries() {
        TransportEndpointId duplicatedEndpoint = endpoint(0, 64, 0);
        TransportEndpointId validEndpoint = endpoint(1, 64, 0);
        JsonObject duplicated = encodedEntry(duplicatedEndpoint,
                reservation(20, 1.0, 0.0, TransportAdmissionStatus.ACTIVE));
        JsonObject valid = encodedEntry(validEndpoint,
                reservation(20, 1.0, 0.0, TransportAdmissionStatus.ACTIVE));

        JsonArray duplicateEntries = new JsonArray();
        duplicateEntries.add(duplicated);
        duplicateEntries.add(duplicated.deepCopy());
        duplicateEntries.add(valid);
        TownTransportState withoutDuplicates = TownTransportState.CODEC.parse(
                        JsonOps.INSTANCE, stateJson(duplicateEntries))
                .result().orElseThrow();
        assertEquals(Map.of(validEndpoint, withoutDuplicates.getReservation(validEndpoint)),
                withoutDuplicates.getReservations());

        JsonArray partiallyDamagedEntries = new JsonArray();
        partiallyDamagedEntries.add(valid);
        partiallyDamagedEntries.add(new JsonObject());
        TownTransportState recovered = TownTransportState.CODEC.parse(
                        JsonOps.INSTANCE, stateJson(partiallyDamagedEntries))
                .result().orElseThrow();
        assertEquals(Map.of(validEndpoint, recovered.getReservation(validEndpoint)),
                recovered.getReservations());
    }

    @Test
    void statePreservesStoredRatesWhenConfigMaximumFallsAndExposesStableReadOnlyMap() {
        TransportEndpointId first = endpoint(12, 64, 0);
        TransportEndpointId second = endpoint(-4, 64, 0);
        TransportReservation valid = reservation(20, 8.0, 0.0, TransportAdmissionStatus.ACTIVE);
        TransportReservation outOfRange = reservation(1281, 8.0, 0.0,
                TransportAdmissionStatus.ACTIVE);
        TownTransportState state = new TownTransportState(TownTransportState.DailyReport.EMPTY, Map.of(
                first, valid, second, outOfRange));

        assertEquals(List.of(second, first), new ArrayList<>(state.getReservations().keySet()));
        assertThrows(UnsupportedOperationException.class,
                () -> state.getReservations().clear());
        assertTrue(state.recalculateReservedCapacities(
                TownModelParameters.currentDefaults().transportConsumers()));
        assertEquals(2, state.getReservations().size());
        assertEquals(1821.4, state.getReservedTransportCapacity(), 1.0e-9);

        TransportConsumerParameters changedFactor = new TransportConsumerParameters(
                20, 1, 1280, 0.1, 0.05);
        assertTrue(state.recalculateReservedCapacities(changedFactor));
        assertEquals(2341.8, state.getReservedTransportCapacity(), 1.0e-9);
    }

    private static TransportEndpointId endpoint(int x, int y, int z) {
        return new TransportEndpointId(GlobalPos.of(Level.OVERWORLD, new BlockPos(x, y, z)));
    }

    private static TransportReservation reservation(
            int rate,
            double scaleMetric,
            double reservedCapacity,
            TransportAdmissionStatus status
    ) {
        return reservation(TransportEndpointKind.WAREHOUSE_INTERFACE,
                rate, scaleMetric, reservedCapacity, status);
    }

    private static TransportReservation reservation(
            TransportEndpointKind kind,
            int rate,
            double scaleMetric,
            double reservedCapacity,
            TransportAdmissionStatus status
    ) {
        return new TransportReservation(kind,
                rate, scaleMetric, reservedCapacity, status);
    }

    private static JsonObject encodedEntry(TransportEndpointId endpoint, TransportReservation reservation) {
        return TownTransportState.ReservationEntry.CODEC.encodeStart(
                        JsonOps.INSTANCE, new TownTransportState.ReservationEntry(endpoint, reservation))
                .result().orElseThrow().getAsJsonObject();
    }

    private static JsonObject stateJson(JsonArray entries) {
        JsonObject root = new JsonObject();
        root.add("reservations", entries);
        return root;
    }
}
