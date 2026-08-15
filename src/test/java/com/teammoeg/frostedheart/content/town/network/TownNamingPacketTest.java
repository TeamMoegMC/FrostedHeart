/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TownNamingPacketTest {
    @Test
    void townNameRequestRoundTrips() {
        assertRoundTrip(new TownNameEditRequestPacket("新伦敦"), TownNameEditRequestPacket::new);
    }

    @Test
    void townNameUpdateRoundTrips() {
        assertRoundTrip(new TownNameUpdatePacket("新伦敦"), TownNameUpdatePacket::new);
    }

    @Test
    void residentNameRequestRoundTripsEmptyLastName() {
        assertRoundTrip(new TownResidentNameEditRequestPacket(UUID.randomUUID(), "艾达", ""),
                TownResidentNameEditRequestPacket::new);
    }

    private static <T extends com.teammoeg.chorda.network.CMessage> void assertRoundTrip(
            T source,
            java.util.function.Function<FriendlyByteBuf, T> decoder
    ) {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.encode(encoded);
            decoder.apply(encoded).encode(reencoded);
            assertArrayEquals(ByteBufUtil.getBytes(encoded, 0, encoded.writerIndex()),
                    ByteBufUtil.getBytes(reencoded, 0, reencoded.writerIndex()));
        } finally {
            encoded.release();
            reencoded.release();
        }
    }
}
