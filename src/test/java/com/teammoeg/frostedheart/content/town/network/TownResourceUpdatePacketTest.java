/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
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

import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TownResourceUpdatePacketTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void packetRoundTripsTransportDailyReportWithResourceChanges() {
        TownResourceUpdatePacket source = new TownResourceUpdatePacket(
                Map.<ITownResourceKey, Double>of(
                        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 128.0),
                17.0,
                new TownTransportSnapshot(
                        new TownTransportState.DailyReport(true, 128.0, 0.0),
                        128.0,
                        List.of()));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.encode(encoded);
            new TownResourceUpdatePacket(encoded).encode(reencoded);

            assertArrayEquals(ByteBufUtil.getBytes(encoded, 0, encoded.writerIndex()),
                    ByteBufUtil.getBytes(reencoded, 0, reencoded.writerIndex()));
        } finally {
            encoded.release();
            reencoded.release();
        }
    }

    @Test
    void clientAppliesResourcesAndTransportBeforeOneObserverCallback() {
        TeamTownData data = new TeamTownData(
                "Client Sync",
                new TeamTownResourceHolder(),
                Map.of(), Map.of(), Map.of(),
                0, 0, List.of(), TownStaffingPlan.EMPTY, -1L);
        TeamTown town = data.createTeamTown();
        ITownResourceKey capacityKey = VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0);
        TransportEndpointId endpoint = new TransportEndpointId(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 1)));
        TransportReservation reservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)),
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);
        TownTransportSnapshot snapshot = new TownTransportSnapshot(
                new TownTransportState.DailyReport(true, 64.0, 28.0),
                64.0,
                List.of(new TownTransportState.ReservationEntry(endpoint, reservation)));
        AtomicInteger callbacks = new AtomicInteger();
        AtomicReference<List<Double>> observed = new AtomicReference<>();
        ITownDataUpdateListener listener = new ITownDataUpdateListener() {
            @Override
            public void onResourcesChanged() {
                callbacks.incrementAndGet();
                observed.set(List.of(
                        town.getResourceHolder().get(capacityKey),
                        town.getResourceHolder().getOccupiedCapacity(),
                        town.getTransportState().getReservedTransportCapacity()));
            }
        };

        TeamTownData.addClientListener(listener);
        try {
            data.applyResourceUpdate(Map.of(capacityKey, 64.0), 17.0, snapshot);
        } finally {
            TeamTownData.removeClientListener(listener);
        }

        assertEquals(1, callbacks.get());
        assertEquals(List.of(64.0, 17.0, 28.0), observed.get());
        assertEquals(reservation, town.getTransportState().getReservation(endpoint));
    }
}
