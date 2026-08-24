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
import com.teammoeg.frostedheart.content.town.transport.P2PBindingDecision;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingResult;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingState;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.TownPolicyState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTownP2PBindingTest {
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
    void bidirectionalPairCommitsTwoReservationsAsOneAtomicConnection() {
        TeamTownData data = townData(1_000.0);
        TeamTown town = data.createTeamTown();
        P2PTerminalEndpoint first = endpoint(P2PTerminalRole.BIDIRECTIONAL, 0);
        P2PTerminalEndpoint second = endpoint(P2PTerminalRole.BIDIRECTIONAL, 10);

        P2PBindingResult result = town.bindOrRebindP2PTerminals(first, second);

        assertEquals(P2PBindingDecision.ACCEPTED, result.decision());
        assertEquals(2, data.getP2PBindingState().bindings().size());
        assertEquals(2, town.getTransportReservations().size());
        assertEquals(60.0, town.getTransportSummary().reservedCapacity(), EPSILON);
        town.getTransportReservations().values().forEach(reservation ->
                assertEquals(TransportAdmissionStatus.ACTIVE, reservation.admissionStatus()));
    }

    @Test
    void failedRebindLeavesBindingIndexesAndReservationsByteForByteUnchanged() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        P2PTerminalEndpoint sender = endpoint(P2PTerminalRole.SHIPPING, 0);
        P2PBindingResult initial = town.bindOrRebindP2PTerminals(sender,
                endpoint(P2PTerminalRole.RECEIVING, 10));
        var bindingsBefore = data.getP2PBindingState().bindings();
        var reservationsBefore = town.getTransportReservations();
        data.createTeamTown().getResourceHolder().applySyncEntry(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 31.0);
        data.getDataSyncCache().clearChanged();

        P2PBindingResult rejected = town.bindOrRebindP2PTerminals(sender,
                endpoint(P2PTerminalRole.RECEIVING, 20));

        assertEquals(P2PBindingDecision.INSUFFICIENT_CAPACITY, rejected.decision());
        assertEquals(bindingsBefore, data.getP2PBindingState().bindings());
        assertEquals(reservationsBefore, town.getTransportReservations());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());
    }

    @Test
    void laterDuplicateConnectionReplacesStableIdWithoutDuplicateCapacity() {
        TeamTownData data = townData(100.0);
        TeamTown town = data.createTeamTown();
        P2PTerminalEndpoint sender = endpoint(P2PTerminalRole.SHIPPING, 0);
        P2PTerminalEndpoint receiver = endpoint(P2PTerminalRole.RECEIVING, 10);
        UUID oldId = town.bindOrRebindP2PTerminals(sender, receiver)
                .connectionId().orElseThrow();

        UUID replacementId = town.bindOrRebindP2PTerminals(receiver, sender)
                .connectionId().orElseThrow();

        assertFalse(oldId.equals(replacementId));
        assertTrue(data.getP2PBindingState().connection(oldId).isEmpty());
        assertEquals(1, data.getP2PBindingState().bindings().size());
        assertEquals(30.0, town.getTransportSummary().reservedCapacity(), EPSILON);
        assertEquals(P2PBindingDecision.STALE_CONNECTION,
                town.unbindP2PConnection(oldId).decision());
        assertEquals(P2PBindingDecision.ACCEPTED,
                town.unbindP2PConnection(replacementId).decision());
        assertTrue(data.getP2PBindingState().bindings().isEmpty());
        assertTrue(town.getTransportReservations().isEmpty());
    }

    @Test
    void loadRecoveryRebuildsDistanceFromBindingFactsAndDropsStoredMetric() {
        P2PTerminalEndpoint sender = endpoint(P2PTerminalRole.SHIPPING, 0);
        P2PTerminalEndpoint receiver = endpoint(P2PTerminalRole.RECEIVING, 10);
        P2PBindingState bindings = P2PBindingState.EMPTY.apply(
                P2PBindingState.EMPTY.planConnection(sender, receiver, 20,
                        new UUID(0L, 1L)));
        TownTransportState transport = new TownTransportState(
                TownTransportState.DailyReport.EMPTY,
                Map.of(new TransportEndpointId(sender.pos()), new TransportReservation(
                        TransportEndpointKind.P2P_DIRECT_LINK,
                        20, 999.0, 1_019.0, TransportAdmissionStatus.ACTIVE)));
        TeamTownData data = new TeamTownData(
                "P2P Recovery Test",
                new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 1_000.0)),
                Map.of(), Map.of(), Map.of(), 0, 0, List.of(), -1L,
                TownStaffingPlan.EMPTY, TownHousingPlan.EMPTY, TownPolicyState.DEFAULT,
                transport, bindings, -1L);

        TransportReservation recovered = data.createTeamTown().getTransportReservation(
                new TransportEndpointId(sender.pos())).orElseThrow();

        assertEquals(10.0, recovered.scaleMetric(), EPSILON);
        assertEquals(30.0, recovered.reservedTransportCapacity(), EPSILON);
        assertEquals(bindings.bindings(), data.getP2PBindingState().bindings());
    }

    @Test
    void rejectedRateIncreasePreservesAuthoritativeRateAndReservation() {
        TeamTownData data = townData(40.0);
        TeamTown town = data.createTeamTown();
        P2PTerminalEndpoint sender = endpoint(P2PTerminalRole.SHIPPING, 0);
        town.bindOrRebindP2PTerminals(sender,
                endpoint(P2PTerminalRole.RECEIVING, 10));

        P2PBindingResult rejected = town.setP2PTransportRate(sender.pos(), 30);

        assertEquals(P2PBindingDecision.INSUFFICIENT_CAPACITY, rejected.decision());
        assertEquals(15.0, rejected.requiredAdditionalCapacity(), EPSILON);
        assertEquals(20, data.getP2PBindingState().outgoing(sender.pos())
                .orElseThrow().rateItemsPerSecond());
        assertEquals(30.0, town.getTransportReservation(
                sender.transportEndpointId()).orElseThrow()
                .reservedTransportCapacity(), EPSILON);
    }

    @Test
    void redstonePauseReleasesReservationAndResumeMayCreateShortage() {
        TeamTownData data = townData(40.0);
        TeamTown town = data.createTeamTown();
        P2PTerminalEndpoint sender = endpoint(P2PTerminalRole.SHIPPING, 0);
        P2PTerminalEndpoint receiver = endpoint(P2PTerminalRole.RECEIVING, 10);
        town.bindOrRebindP2PTerminals(sender, receiver);

        assertEquals(P2PBindingDecision.ACCEPTED,
                town.setP2PEndpointRedstonePowered(receiver.pos(), true).decision());
        town.setP2PRedstonePaused(sender.pos(), true);
        town.setP2PRedstonePaused(sender.pos(), false);
        assertFalse(data.getP2PBindingState().outgoing(sender.pos())
                .orElseThrow().senderRedstonePowered());
        assertTrue(data.getP2PBindingState().outgoing(sender.pos())
                .orElseThrow().receiverRedstonePowered());
        TransportReservation paused = town.getTransportReservation(
                sender.transportEndpointId()).orElseThrow();
        assertEquals(TransportAdmissionStatus.REDSTONE_PAUSED, paused.admissionStatus());
        assertEquals(0.0, paused.reservedTransportCapacity(), EPSILON);

        town.getResourceHolder().applySyncEntry(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 10.0);
        assertEquals(P2PBindingDecision.ACCEPTED,
                town.setP2PEndpointRedstonePowered(receiver.pos(), false).decision());
        assertEquals(30.0, town.getTransportSummary().reservedCapacity(), EPSILON);
        assertEquals(20.0, town.getTransportSummary().shortfall(), EPSILON);
        assertEquals(TransportAdmissionStatus.ACTIVE, town.getTransportReservation(
                sender.transportEndpointId()).orElseThrow().admissionStatus());
    }

    @Test
    void poweredEndpointParticipatesInInitialBindingAdmissionAsPaused() {
        TeamTownData data = townData(0.0);
        TeamTown town = data.createTeamTown();
        P2PTerminalEndpoint sender = endpoint(P2PTerminalRole.SHIPPING, 0);
        P2PTerminalEndpoint receiver = endpoint(P2PTerminalRole.RECEIVING, 10);

        P2PBindingResult result = town.bindOrRebindP2PTerminals(
                sender, receiver, false, true);

        assertEquals(P2PBindingDecision.ACCEPTED, result.decision());
        TransportReservation paused = town.getTransportReservation(
                sender.transportEndpointId()).orElseThrow();
        assertEquals(TransportAdmissionStatus.REDSTONE_PAUSED, paused.admissionStatus());
        assertEquals(0.0, paused.reservedTransportCapacity(), EPSILON);

        town.setP2PEndpointRedstonePowered(receiver.pos(), false);
        assertEquals(30.0, town.getTransportSummary().shortfall(), EPSILON);
    }

    @Test
    void bindingCannotOverflowTheCombinedTransportSnapshotReservationLimit() {
        TeamTownData data = townData(1_000_000.0);
        TeamTown town = data.createTeamTown();
        for (int index = 0; index < TownTransportSnapshot.MAX_RESERVATIONS; index++) {
            TransportEndpointId endpointId = new TransportEndpointId(GlobalPos.of(
                    Level.OVERWORLD, new BlockPos(10_000 + index, 64, 0)));
            data.getTransportState().replaceReservation(endpointId, new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE, 0, 0.0, 0.0,
                    TransportAdmissionStatus.DISABLED));
        }

        P2PBindingResult result = town.bindOrRebindP2PTerminals(
                endpoint(P2PTerminalRole.SHIPPING, 0),
                endpoint(P2PTerminalRole.RECEIVING, 10));

        assertEquals(P2PBindingDecision.INVALID_REQUEST, result.decision());
        assertTrue(data.getP2PBindingState().bindings().isEmpty());
        assertEquals(TownTransportSnapshot.MAX_RESERVATIONS,
                data.getTransportState().getReservations().size());
    }

    private static TeamTownData townData(double capacity) {
        return new TeamTownData(
                "P2P Binding Test",
                new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), capacity)),
                Map.of(), Map.of(), Map.of(), 0, 0, List.of(),
                TownStaffingPlan.EMPTY, -1L);
    }

    private static P2PTerminalEndpoint endpoint(P2PTerminalRole role, int x) {
        return new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(x, 64, 0)), role);
    }
}
