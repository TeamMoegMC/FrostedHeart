/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
                new TownTransportState.DailyReport(true, 128.0, 0.0));
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
}
