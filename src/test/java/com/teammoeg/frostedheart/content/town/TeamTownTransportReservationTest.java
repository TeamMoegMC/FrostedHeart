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
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
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
    private static final BlockPos WAREHOUSE_POS = new BlockPos(8, 64, 0);

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
                request(endpoint, 20));
        assertEquals(TransportReservationDecision.ACCEPTED, created.decision());
        TransportReservation beforeFailure = created.reservationAfter().orElseThrow();
        assertEquals(28.0, beforeFailure.reservedTransportCapacity(), EPSILON);

        data.getDataSyncCache().clearChanged();
        TransportReservationResult rejected = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 100));
        TransportReservation afterFailure = rejected.reservationAfter().orElseThrow();
        assertEquals(TransportReservationDecision.INSUFFICIENT_CAPACITY, rejected.decision());
        assertEquals(beforeFailure, afterFailure);
        assertEquals(20, afterFailure.rateItemsPerSecond());
        assertEquals(28.0, afterFailure.reservedTransportCapacity(), EPSILON);
        assertEquals(TransportAdmissionStatus.ACTIVE, afterFailure.admissionStatus());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());

        data.getDataSyncCache().clearChanged();
        TransportReservationResult confirmed = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 20));
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
                request(endpoint, 20));

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
        town.registerOrUpdateTransportEndpoint(request(endpoint, 20));

        BlockPos farWarehouse = new BlockPos(64, 64, 0);
        data.buildings.put(farWarehouse, warehouse(farWarehouse, 3_000.0));
        data.markWarehouseTopologyDirty();
        town.prepareWarehouseTopology(Level.OVERWORLD);

        TransportReservationResult refreshed = town.refreshTransportEndpointMetric(endpoint);
        assertEquals(TransportReservationDecision.ACCEPTED, refreshed.decision());
        assertEquals(70.0, refreshed.reservationAfter().orElseThrow().reservedTransportCapacity(), EPSILON);
        assertEquals(40.0, refreshed.townSummaryAfter().shortfall(), EPSILON);
        assertEquals(30.0 / 70.0, refreshed.townSummaryAfter().effectiveRateScale(), EPSILON);

        TransportReservationResult disabled = town.registerOrUpdateTransportEndpoint(
                request(endpoint, 0));
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
    void requestDerivesMetricAndEquivalentRequestDoesNotMarkDirty() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);
        TransportEndpointRequest request = request(endpoint, 20);
        town.registerOrUpdateTransportEndpoint(request);
        data.getDataSyncCache().clearChanged();

        town.registerOrUpdateTransportEndpoint(request);
        assertFalse(data.getDataSyncCache().hasTransportStateChange());

        assertEquals(8.0, town.getTransportReservation(endpoint).orElseThrow().scaleMetric(), EPSILON);
    }

    @Test
    void p2pKindCannotEnterTheWarehouseSpecificRegistrationPath() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);

        TransportReservationResult result = town.registerOrUpdateTransportEndpoint(
                new TransportEndpointRequest(endpoint,
                        TransportEndpointKind.P2P_DIRECT_LINK, 20));

        assertEquals(TransportReservationDecision.INVALID_BINDING, result.decision());
        assertTrue(result.reservationAfter().isEmpty());
        assertTrue(town.getTransportReservation(endpoint).isEmpty());
        assertEquals(0.0, town.getTransportSummary().reservedCapacity(), EPSILON);
    }

    @Test
    void explicitWarehouseEntryPreservesLegacyRegistrationContract() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId endpoint = endpoint(0);

        TransportReservationResult result = town.registerOrUpdateWarehouseInterface(
                request(endpoint, 20));

        assertEquals(TransportReservationDecision.ACCEPTED, result.decision());
        assertEquals(8.0, result.reservationAfter().orElseThrow().scaleMetric(), EPSILON);
        assertEquals(28.0, result.reservationAfter().orElseThrow()
                .reservedTransportCapacity(), EPSILON);
        assertEquals(result.reservationAfter(), town.getTransportReservation(endpoint));
    }

    @Test
    void p2pAdmissionUsesDirectEndpointFactsWithoutMutatingTownState() {
        TeamTownData data = townData(100.0);
        data.buildings.clear();
        data.markWarehouseTopologyDirty();
        data.createTeamTown().prepareWarehouseTopology(Level.OVERWORLD);
        TeamTown town = data.createTeamTown();
        GlobalPos sender = GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 64, 0));
        GlobalPos receiver = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 70, -4));

        TeamTown.ResolvedTransportAdmission admission = town.evaluateP2PTransportAdmission(
                sender, receiver, 20);

        assertEquals(TransportReservationDecision.ACCEPTED, admission.decision());
        TransportReservation candidate = admission.acceptedReservation().orElseThrow();
        assertEquals(TransportEndpointKind.P2P_DIRECT_LINK, candidate.endpointKind());
        assertEquals(20.0, candidate.scaleMetric(), EPSILON);
        assertEquals(40.0, candidate.reservedTransportCapacity(), EPSILON);
        assertTrue(town.getTransportReservations().isEmpty(),
                "fact evaluation must not create a binding or reservation");
        assertFalse(data.getDataSyncCache().hasTransportStateChange());
    }

    @Test
    void invalidOrRejectedP2pAdmissionNeverPersistsAnUnboundTarget() {
        TeamTownData data = townData(10.0);
        TeamTown town = data.createTeamTown();
        GlobalPos sender = GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 64, 0));

        TeamTown.ResolvedTransportAdmission crossDimension = town.evaluateP2PTransportAdmission(
                sender, GlobalPos.of(Level.NETHER, new BlockPos(0, 64, 0)), 20);
        assertEquals(TransportReservationDecision.INVALID_BINDING, crossDimension.decision());

        TeamTown.ResolvedTransportAdmission insufficient = town.evaluateP2PTransportAdmission(
                sender, GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 0)), 20);
        assertEquals(TransportReservationDecision.INSUFFICIENT_CAPACITY,
                insufficient.decision());
        assertTrue(insufficient.acceptedReservation().isEmpty());
        assertTrue(town.getTransportReservations().isEmpty());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());
    }

    @Test
    void endpointRemovalDoesNotAffectOtherTownOwnedInterfaces() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId first = endpoint(0);
        TransportEndpointId second = endpoint(1);
        TransportEndpointId third = endpoint(2);

        town.registerOrUpdateTransportEndpoint(request(first, 20));
        town.registerOrUpdateTransportEndpoint(request(second, 20));
        town.registerOrUpdateTransportEndpoint(request(third, 20));
        assertEquals(81.0, town.getTransportSummary().reservedCapacity(), EPSILON);

        data.createTeamTown().getResourceHolder().applySyncEntry(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 40.5);
        assertEquals(0.5, town.getTransportSummary().effectiveRateScale(), EPSILON);
        assertEquals(20, town.getTransportReservation(first).orElseThrow().rateItemsPerSecond());

        data.createTeamTown().getResourceHolder().applySyncEntry(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 100.0);
        assertEquals(1.0, town.getTransportSummary().effectiveRateScale(), EPSILON);
        town.unregisterTransportEndpoint(first);
        assertEquals(List.of(second, third), new ArrayList<>(town.getTransportReservations().keySet()));
        assertEquals(53.0, town.getTransportSummary().reservedCapacity(), EPSILON);
    }

    @Test
    void unavailableTopologyPreservesExistingRateButNewEndpointStartsAtZero() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        TransportEndpointId existing = endpoint(0);
        town.registerOrUpdateTransportEndpoint(request(existing, 20));

        data.buildings.clear();
        data.markWarehouseTopologyDirty();
        town.prepareWarehouseTopology(Level.OVERWORLD);
        TransportReservationResult unavailable = town.refreshTransportEndpointMetric(existing);
        assertEquals(TransportAdmissionStatus.UNAVAILABLE,
                unavailable.reservationAfter().orElseThrow().admissionStatus());
        assertEquals(20, unavailable.reservationAfter().orElseThrow().rateItemsPerSecond());
        assertEquals(0.0, unavailable.townSummaryAfter().reservedCapacity(), EPSILON);
        assertEquals(TransportReservationDecision.INVALID_BINDING,
                town.registerOrUpdateTransportEndpoint(request(existing, 30)).decision());

        TransportEndpointId addedWithoutWarehouse = endpoint(1);
        TransportReservationResult created = town.registerOrUpdateTransportEndpoint(
                request(addedWithoutWarehouse, 20));
        assertEquals(TransportReservationDecision.ACCEPTED, created.decision());
        assertEquals(TransportAdmissionStatus.UNAVAILABLE,
                created.reservationAfter().orElseThrow().admissionStatus());
        assertEquals(0, created.reservationAfter().orElseThrow().rateItemsPerSecond());

        data.buildings.put(WAREHOUSE_POS, warehouse(WAREHOUSE_POS, 1_000.0));
        data.markWarehouseTopologyDirty();
        town.prepareWarehouseTopology(Level.OVERWORLD);
        TransportReservationResult restored = town.refreshTransportEndpointMetric(existing);
        assertEquals(TransportAdmissionStatus.ACTIVE,
                restored.reservationAfter().orElseThrow().admissionStatus());
        assertEquals(28.0, restored.reservationAfter().orElseThrow().reservedTransportCapacity(), EPSILON);
    }

    @Test
    void snapshotLimitOfIdleEndpointReadsKeepsTransportSyncClean() {
        TeamTownData data = townData(1_000_000.0);
        TeamTown town = data.createTeamTown();
        List<TransportEndpointId> endpoints = new ArrayList<>(TownTransportSnapshot.MAX_RESERVATIONS);
        for (int index = 0; index < TownTransportSnapshot.MAX_RESERVATIONS; index++) {
            TransportEndpointId endpoint = endpoint(index);
            endpoints.add(endpoint);
            data.getTransportState().replaceReservation(endpoint, new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE,
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
        TeamTownData data = new TeamTownData(
                "Reservation Test",
                new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), capacity)),
                Map.of(WAREHOUSE_POS, warehouse(WAREHOUSE_POS, 1_000.0)),
                Map.of(), Map.of(), 0, 0, List.of(), TownStaffingPlan.EMPTY, -1L);
        data.createTeamTown().prepareWarehouseTopology(Level.OVERWORLD);
        return data;
    }

    private static TransportEndpointId endpoint(int x) {
        return new TransportEndpointId(GlobalPos.of(Level.OVERWORLD, new BlockPos(x, 64, 0)));
    }

    private static WarehouseBuilding warehouse(BlockPos pos, double capacity) {
        return new WarehouseBuilding(pos, true, OccupiedVolume.EMPTY, true,
                false, capacity, 1, 1, 0);
    }

    private static TransportEndpointRequest request(TransportEndpointId endpoint, int rate) {
        return new TransportEndpointRequest(
                endpoint, TransportEndpointKind.WAREHOUSE_INTERFACE, rate);
    }
}
