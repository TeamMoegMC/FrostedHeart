/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotice;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTransportShortageNotificationPacketTest {
    @Test
    void packetRoundTripsOnlyValidatedNumericNotices() {
        TownTransportShortageNotificationPacket source =
                new TownTransportShortageNotificationPacket(19L, List.of(
                        notice(64.0, 80.0),
                        notice(0.0, 20.0)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.encode(buffer);

            assertEquals(source, new TownTransportShortageNotificationPacket(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void decoderRejectsEmptyAndOversizedLists() {
        assertMalformedCount(0);
        assertMalformedCount(TownTransportShortageNotificationPacket.MAX_NOTICES + 1);
    }

    @Test
    void decoderRejectsNonFiniteNegativeAndInconsistentFields() {
        assertMalformedNotice(Double.NaN, 80.0, 16.0, 0.8);
        assertMalformedNotice(64.0, -80.0, 16.0, 0.8);
        assertMalformedNotice(64.0, 80.0, 15.0, 0.8);
        assertMalformedNotice(64.0, 80.0, 16.0, 0.9);
        assertMalformedNotice(64.0, 80.0, 16.0, 1.1);
    }

    @Test
    void packetHandlerAcceptsOnlyClientboundDirection() {
        assertTrue(TownTransportShortageNotificationPacket.isClientbound(
                NetworkDirection.PLAY_TO_CLIENT));
        assertFalse(TownTransportShortageNotificationPacket.isClientbound(
                NetworkDirection.PLAY_TO_SERVER));
    }

    private static TownTransportShortageNotice notice(double total, double reserved) {
        return TownTransportShortageNotice.from(total, reserved).orElseThrow();
    }

    private static void assertMalformedCount(int count) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeLong(1L);
            buffer.writeVarInt(count);
            assertThrows(DecoderException.class,
                    () -> new TownTransportShortageNotificationPacket(buffer));
        } finally {
            buffer.release();
        }
    }

    private static void assertMalformedNotice(
            double total, double reserved, double shortfall, double scale
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeLong(1L);
            buffer.writeVarInt(1);
            buffer.writeDouble(total);
            buffer.writeDouble(reserved);
            buffer.writeDouble(shortfall);
            buffer.writeDouble(scale);
            assertThrows(DecoderException.class,
                    () -> new TownTransportShortageNotificationPacket(buffer));
        } finally {
            buffer.release();
        }
    }
}
