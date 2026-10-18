/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

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
