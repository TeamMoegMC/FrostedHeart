/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraredPacketCodecTest {
    @Test
    void requestRoundTripsTheExactPresenceWords() {
        long[] presence =
                new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
        for (int index = 0; index < presence.length; index++) {
            presence[index] = 0x0102030405060708L + index;
        }
        FHRequestInfraredViewDataSyncPacket original =
                new FHRequestInfraredViewDataSyncPacket(
                        17, false, 91L, presence);

        assertRoundTrip(
                original,
                FHRequestInfraredViewDataSyncPacket::new);
    }

    @Test
    void maximumResponseStaysBelowTheSinglePacketBound() {
        short[] records =
                new short[729 * FHResponseInfraredViewDataSyncPacket.RECORD_SHORTS];
        for (int page = 0; page < 729; page++) {
            int offset =
                    page * FHResponseInfraredViewDataSyncPacket.RECORD_SHORTS;
            records[offset] = (short) page;
            Arrays.fill(records, offset + 1, offset + 65, (short) (page - 300));
        }
        FHResponseInfraredViewDataSyncPacket original =
                new FHResponseInfraredViewDataSyncPacket(
                        23,
                        new MinecraftThermalInput.InfraredSnapshot(
                                -4, 7, 2, 101L, true, records));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        original.encode(encoded);
        assertTrue(encoded.readableBytes() < 96 * 1024);

        FriendlyByteBuf responseInput =
                new FriendlyByteBuf(encoded.copy());
        FHResponseInfraredViewDataSyncPacket decoded =
                new FHResponseInfraredViewDataSyncPacket(responseInput);
        responseInput.release();
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        decoded.encode(reencoded);
        assertTrue(ByteBufUtil.equals(encoded, reencoded));
        assertEquals(encoded.readableBytes(), reencoded.readableBytes());
        encoded.release();
        reencoded.release();
    }

    private static <T extends com.teammoeg.chorda.network.CMessage>
    void assertRoundTrip(
            T original,
            java.util.function.Function<FriendlyByteBuf, T> decoder
    ) {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        original.encode(encoded);
        FriendlyByteBuf input = new FriendlyByteBuf(encoded.copy());
        T decoded = decoder.apply(input);
        input.release();
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        decoded.encode(reencoded);
        assertTrue(ByteBufUtil.equals(encoded, reencoded));
        encoded.release();
        reencoded.release();
    }
}
