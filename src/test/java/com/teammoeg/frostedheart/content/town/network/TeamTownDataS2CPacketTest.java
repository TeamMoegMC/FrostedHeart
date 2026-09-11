/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.chorda.io.codec.DataOps;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamTownDataS2CPacketTest {
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
    void fullSyncRestoresServerDerivedTransportCapacityBeforeReplacingClientData() throws Exception {
        ITownResourceKey capacityKey = VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0);
        TeamTownData source = new TeamTownData(
                "Full Sync",
                new TeamTownResourceHolder(Map.of(capacityKey, 64.0)),
                Map.of(), Map.of(), Map.of(),
                0, 0, List.of(), TownStaffingPlan.EMPTY, -1L);
        TransportEndpointId endpoint = new TransportEndpointId(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 1)));
        TransportReservation reservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);
        TownTransportSnapshot fullSnapshot = new TownTransportSnapshot(
                new TownTransportState.DailyReport(true, 64.0, 28.0),
                64.0,
                1,
                0.05,
                List.of(new TownTransportState.ReservationEntry(endpoint, reservation)));
        source.getTransportState().applySnapshot(fullSnapshot);

        Object persisted = FHSpecialDataTypes.TOWN_DATA.saveData(DataOps.COMPRESSED, source);
        TeamTownData persistenceOnly = FHSpecialDataTypes.TOWN_DATA.loadData(DataOps.COMPRESSED, persisted);
        assertEquals(0.0, persistenceOnly.getTransportState().getReservedTransportCapacity(), EPSILON);

        TeamTownDataS2CPacket packet = new TeamTownDataS2CPacket(source);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(encoded);
            TeamTownDataS2CPacket decodedPacket = new TeamTownDataS2CPacket(encoded);
            decodedPacket.encode(reencoded);
            TeamTownData decoded = decodedPacket.decodeTownData();

            assertArrayEquals(ByteBufUtil.getBytes(encoded, 0, encoded.writerIndex()),
                    ByteBufUtil.getBytes(reencoded, 0, reencoded.writerIndex()));
            assertEquals(64.0, decoded.createTeamTown().getResourceHolder().get(capacityKey), EPSILON);
            assertEquals(28.0, decoded.getTransportState().getReservedTransportCapacity(), EPSILON);
            assertEquals(reservation, decoded.getTransportState().getReservation(endpoint));
            assertEquals(new TownTransportState.DailyReport(true, 64.0, 28.0),
                    decoded.getTransportState().getDailyReport());
            assertEquals(1, decoded.getTransportState().getEffectiveWarehouseCount());
            assertEquals(0.05,
                    decoded.getTransportState().getWarehouseDistanceCostPerBlock(), EPSILON);
        } finally {
            encoded.release();
            reencoded.release();
        }
    }

    @Test
    void fullThenIncrementalPacketsConvergeWithoutZeroingDerivedReservations() throws Exception {
        ITownResourceKey capacityKey = VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0);
        TeamTownData source = new TeamTownData(
                "Interleaved Sync",
                new TeamTownResourceHolder(Map.of(capacityKey, 64.0)),
                Map.of(), Map.of(), Map.of(),
                0, 0, List.of(), TownStaffingPlan.EMPTY, -1L);
        TransportEndpointId endpoint = new TransportEndpointId(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 1)));
        TransportReservation fullReservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);
        source.getTransportState().applySnapshot(new TownTransportSnapshot(
                TownTransportState.DailyReport.EMPTY,
                64.0,
                1,
                0.05,
                List.of(new TownTransportState.ReservationEntry(endpoint, fullReservation))));

        FriendlyByteBuf fullBytes = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf incrementalBytes = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf replacementFullBytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new TeamTownDataS2CPacket(source).encode(fullBytes);
            TeamTownData clientData = new TeamTownDataS2CPacket(fullBytes).decodeTownData();
            assertEquals(28.0, clientData.getTransportState().getReservedTransportCapacity(), EPSILON);
            assertEquals(fullReservation, clientData.getTransportState().getReservation(endpoint));
            assertEquals(1, clientData.getTransportState().getEffectiveWarehouseCount());
            assertEquals(0.05,
                    clientData.getTransportState().getWarehouseDistanceCostPerBlock(), EPSILON);

            TransportReservation incrementalReservation = new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE,
                    40, 8.0, 56.0, TransportAdmissionStatus.ACTIVE);
            TownTransportSnapshot incrementalSnapshot = new TownTransportSnapshot(
                    new TownTransportState.DailyReport(true, 128.0, 56.0),
                    128.0,
                    3,
                    0.075,
                    List.of(new TownTransportState.ReservationEntry(endpoint, incrementalReservation)));
            new TownResourceUpdatePacket(
                    Map.of(capacityKey, 128.0), 17.0, incrementalSnapshot).encode(incrementalBytes);
            new TownResourceUpdatePacket(incrementalBytes).applyTo(clientData);

            assertEquals(128.0, clientData.createTeamTown().getResourceHolder().get(capacityKey), EPSILON);
            assertEquals(17.0, clientData.createTeamTown().getResourceHolder().getOccupiedCapacity(), EPSILON);
            assertEquals(56.0, clientData.getTransportState().getReservedTransportCapacity(), EPSILON);
            assertEquals(incrementalReservation, clientData.getTransportState().getReservation(endpoint));
            assertEquals(incrementalSnapshot.dailyReport(), clientData.getTransportState().getDailyReport());
            assertEquals(3, clientData.getTransportState().getEffectiveWarehouseCount());
            assertEquals(0.075,
                    clientData.getTransportState().getWarehouseDistanceCostPerBlock(), EPSILON);

            source.createTeamTown().getResourceHolder().applySyncEntry(capacityKey, 128.0);
            source.createTeamTown().getResourceHolder().setOccupiedCapacity(17.0);
            source.getTransportState().applySnapshot(incrementalSnapshot);
            new TeamTownDataS2CPacket(source).encode(replacementFullBytes);
            clientData = new TeamTownDataS2CPacket(replacementFullBytes).decodeTownData();

            assertEquals(128.0, clientData.createTeamTown().getResourceHolder().get(capacityKey), EPSILON);
            assertEquals(17.0, clientData.createTeamTown().getResourceHolder().getOccupiedCapacity(), EPSILON);
            assertEquals(56.0, clientData.getTransportState().getReservedTransportCapacity(), EPSILON);
            assertEquals(incrementalReservation, clientData.getTransportState().getReservation(endpoint));
            assertEquals(incrementalSnapshot.dailyReport(), clientData.getTransportState().getDailyReport());
            assertEquals(3, clientData.getTransportState().getEffectiveWarehouseCount());
            assertEquals(0.075,
                    clientData.getTransportState().getWarehouseDistanceCostPerBlock(), EPSILON);
        } finally {
            fullBytes.release();
            incrementalBytes.release();
            replacementFullBytes.release();
        }
    }
}
