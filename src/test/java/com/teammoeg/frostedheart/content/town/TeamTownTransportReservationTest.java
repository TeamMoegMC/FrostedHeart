/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.transport.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTownTransportReservationTest {
    private static final double EPSILON = 1.0e-9;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void failedIncreaseKeepsTheAcceptedReservationUnchangedAndClean() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);

        TransportReservationResult created = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 20, 8.0));
        assertEquals(TransportReservationDecision.ACCEPTED, created.decision());
        TransportReservation beforeFailure = created.reservationAfter().orElseThrow();
        assertEquals(28.0, beforeFailure.reservedTransportCapacity(), EPSILON);

        data.getDataSyncCache().clearChanged();
        TransportReservationResult rejected = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 100, 8.0));
        TransportReservation afterFailure = rejected.reservationAfter().orElseThrow();
        assertEquals(TransportReservationDecision.INSUFFICIENT_CAPACITY, rejected.decision());
        assertEquals(beforeFailure, afterFailure);
        assertEquals(20, afterFailure.rateItemsPerSecond());
        assertEquals(28.0, afterFailure.reservedTransportCapacity(), EPSILON);
        assertEquals(TransportAdmissionStatus.ACTIVE, afterFailure.admissionStatus());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());

        data.getDataSyncCache().clearChanged();
        TransportReservationResult confirmed = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 20, 8.0));
        assertEquals(TransportReservationDecision.ACCEPTED, confirmed.decision());
        assertEquals(beforeFailure, confirmed.reservationAfter().orElseThrow());
        assertEquals(TransportAdmissionStatus.ACTIVE,
                confirmed.reservationAfter().orElseThrow().admissionStatus());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());
    }

    @Test
    void rejectedNewEndpointIsRecordedWithoutCapacityAndMustBeRetriedExplicitly() {
        TeamTownData data = townData(10.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);

        TransportReservationResult result = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 20, 8.0));

        assertEquals(TransportReservationDecision.INSUFFICIENT_CAPACITY, result.decision());
        TransportReservation rejected = town.getTransportReservation(endpoint).orElseThrow();
        assertEquals(0, rejected.rateItemsPerSecond());
        assertEquals(0.0, rejected.reservedTransportCapacity(), EPSILON);
        assertEquals(TransportAdmissionStatus.DISABLED, rejected.admissionStatus());
        assertEquals(0.0, town.getTransportSummary().reservedCapacity());
    }

    @Test
    void metricRefreshCanCreateShortageAndDisableAndUnregisterAlwaysSucceed() {
        TeamTownData data = townData(30.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);
        GlobalPos core = request(endpoint, 20, 8.0).boundWarehouseCorePos();
        town.registerOrUpdateTransportEndpoint(request(endpoint, 20, 8.0));

        TransportReservationResult refreshed = town.refreshTransportEndpointMetric(endpoint, core, 64.0);
        assertEquals(TransportReservationDecision.ACCEPTED, refreshed.decision());
        assertEquals(84.0, refreshed.reservationAfter().orElseThrow().reservedTransportCapacity(), EPSILON);
        assertEquals(54.0, refreshed.townSummaryAfter().shortfall(), EPSILON);
        assertEquals(30.0 / 84.0, refreshed.townSummaryAfter().effectiveRateScale(), EPSILON);

        TransportReservationResult disabled = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 0, 64.0));
        assertEquals(TransportAdmissionStatus.DISABLED,
                disabled.reservationAfter().orElseThrow().admissionStatus());
        assertEquals(0.0, disabled.townSummaryAfter().reservedCapacity());

        data.getDataSyncCache().clearChanged();
        assertEquals(TransportReservationDecision.ACCEPTED,
                town.unregisterTransportEndpoint(endpoint).decision());
        assertTrue(data.getDataSyncCache().hasTransportStateChange());
        data.getDataSyncCache().clearChanged();
        town.unregisterTransportEndpoint(endpoint);
        assertFalse(data.getDataSyncCache().hasTransportStateChange());
    }

    @Test
    void identityMismatchIsRejectedAndEquivalentRequestDoesNotMarkDirty() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);
        TransportEndpointRequest request = request(endpoint, 20, 8.0);
        town.registerOrUpdateTransportEndpoint(request);
        data.getDataSyncCache().clearChanged();

        town.registerOrUpdateTransportEndpoint(request);
        assertFalse(data.getDataSyncCache().hasTransportStateChange());

        TransportEndpointRequest rebound = new TransportEndpointRequest(
                endpoint, TransportEndpointKind.WAREHOUSE_INTERFACE,
                GlobalPos.of(Level.OVERWORLD, new BlockPos(99, 64, 99)), 20, 8.0);
        assertEquals(TransportReservationDecision.INVALID_BINDING,
                town.registerOrUpdateTransportEndpoint(rebound).decision());
        assertEquals(request.boundWarehouseCorePos(),
                town.getTransportReservation(endpoint).orElseThrow().boundWarehouseCorePos());
    }

    @Test
    void multipleWarehousesShareCapacityAndRecoverWithoutRewritingReservations() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        GlobalPos firstCore = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10));
        GlobalPos secondCore = GlobalPos.of(Level.OVERWORLD, new BlockPos(30, 64, 30));
        TransportEndpointId first = endpoint(0);
        TransportEndpointId second = endpoint(1);
        TransportEndpointId third = endpoint(2);

        town.registerOrUpdateTransportEndpoint(request(first, firstCore, 20, 8.0));
        town.registerOrUpdateTransportEndpoint(request(second, firstCore, 20, 8.0));
        town.registerOrUpdateTransportEndpoint(request(third, secondCore, 20, 2.0));
        assertEquals(78.0, town.getTransportSummary().reservedCapacity(), EPSILON);

        data.createTeamTown().getResourceHolder().applySyncEntry(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 39.0);
        assertEquals(0.5, town.getTransportSummary().effectiveRateScale(), EPSILON);
        assertEquals(20, town.getTransportReservation(first).orElseThrow().rateItemsPerSecond());

        data.createTeamTown().getResourceHolder().applySyncEntry(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 100.0);
        assertEquals(1.0, town.getTransportSummary().effectiveRateScale(), EPSILON);
        assertEquals(TransportReservationDecision.ACCEPTED,
                town.refreshTransportEndpointMetric(first, firstCore, 1.0).decision());
        assertEquals(71.0, town.getTransportSummary().reservedCapacity(), EPSILON);

        assertEquals(2, town.unregisterTransportEndpointsBoundTo(firstCore));
        assertEquals(Map.of(third, town.getTransportReservation(third).orElseThrow()),
                town.getTransportReservations());
        assertEquals(22.0, town.getTransportSummary().reservedCapacity(), EPSILON);
    }

    @Test
    void snapshotLimitOfIdleEndpointReadsKeepsTransportSyncClean() {
        TeamTownData data = townData(1_000_000.0);
        TeamTown town = data.createTeamTown();
        List<TransportEndpointId> endpoints = new ArrayList<>(TownTransportSnapshot.MAX_RESERVATIONS);
        GlobalPos core = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10));
        for (int index = 0; index < TownTransportSnapshot.MAX_RESERVATIONS; index++) {
            TransportEndpointId endpoint = endpoint(index);
            endpoints.add(endpoint);
            data.getTransportState().replaceReservation(endpoint, new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE, core,
                    20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE));
        }
        assertEquals(114_688.0, town.getTransportSummary().reservedCapacity(), EPSILON);
        data.getDataSyncCache().clearChanged();

        assertTimeout(Duration.ofSeconds(5), () -> {
            for (int tick = 0; tick < 20; tick++) {
                for (TransportEndpointId endpoint : endpoints) {
                    assertTrue(town.getTransportReservation(endpoint).isPresent());
                    town.getTransportSummary();
                }
            }
        });
        assertFalse(data.getDataSyncCache().hasChanges(),
                "idle reads must not schedule the transport packet flush path");
    }

    private static TeamTownData townData(double capacity) {
        return new TeamTownData(
                "Reservation Test",
                new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), capacity)),
                Map.of(), Map.of(), Map.of(), 0, 0, List.of(), TownStaffingPlan.EMPTY, -1L);
    }

    private static TransportEndpointId endpoint(int x) {
        return new TransportEndpointId(GlobalPos.of(Level.OVERWORLD, new BlockPos(x, 64, 0)));
    }

    private static TransportEndpointRequest request(TransportEndpointId endpoint, int rate, double metric) {
        return request(endpoint, GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)), rate, metric);
    }

    private static TransportEndpointRequest request(
            TransportEndpointId endpoint,
            GlobalPos core,
            int rate,
            double metric
    ) {
        return new TransportEndpointRequest(
                endpoint, TransportEndpointKind.WAREHOUSE_INTERFACE, core, rate, metric);
    }
}
