/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraredPacketCodecTest {
    private static final int INVALID_MODE = 0;
    private static final int UNIFORM_MODE = 1;
    private static final int INDEXED_MODE = 2;
    private static final int RAW_MODE = 3;

    @Test
    void requestRoundTripsTheExactPresenceWords() {
        long[] presence =
                new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
        for (int index = 0; index < presence.length; index++) {
            presence[index] = 0x0102030405060708L + index;
        }
        FHRequestInfraredViewDataSyncPacket original =
                new FHRequestInfraredViewDataSyncPacket(
                        17, false, 91, presence);

        assertRoundTrip(
                original,
                FHRequestInfraredViewDataSyncPacket::new);
    }

    @Test
    void brickModesRoundTripWithoutExpandingIndexedRecords() {
        short[] invalid = values(InfraredBrickCodec.INVALID_TEMPERATURE);
        byte[] invalidRecord = encodeBrick(7, invalid, false);
        assertEquals(INVALID_MODE, Byte.toUnsignedInt(invalidRecord[2]));
        assertArrayEquals(invalid, decodeSingle(invalidRecord, 7));

        try (InfraredBrickCodec.Builder builder =
                     new InfraredBrickCodec.Builder()) {
            assertFalse(builder.writeBrick(7, invalid, true));
            assertEquals(0, builder.size());
        }

        short[] uniform = values((short) 73);
        byte[] uniformRecord = encodeBrick(8, uniform, false);
        assertEquals(UNIFORM_MODE, Byte.toUnsignedInt(uniformRecord[2]));
        assertArrayEquals(uniform, decodeSingle(uniformRecord, 8));

        try (InfraredBrickCodec.Builder builder =
                     new InfraredBrickCodec.Builder()) {
            assertTrue(builder.writeInvalid(7, false));
            assertArrayEquals(invalidRecord, builder.toByteArray());
            builder.reset();
            assertFalse(builder.writeInvalid(7, true));
            assertEquals(0, builder.size());
            assertTrue(builder.writeUniform(8, (short) 73));
            assertArrayEquals(uniformRecord, builder.toByteArray());
        }

        short[] indexed = new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
        for (int index = 0; index < indexed.length; index++) {
            indexed[index] = (short) (index & 1);
        }
        byte[] indexedRecord = encodeBrick(9, indexed, false);
        assertEquals(INDEXED_MODE, Byte.toUnsignedInt(indexedRecord[2]));
        assertTrue(indexedRecord.length
                < 3 + InfraredBrickCodec.BLOCKS_PER_BRICK * Short.BYTES);
        assertArrayEquals(indexed, decodeSingle(indexedRecord, 9));

        short[] raw = new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
        for (int index = 0; index < raw.length; index++) {
            raw[index] = (short) index;
        }
        byte[] rawRecord = encodeBrick(10, raw, false);
        assertEquals(RAW_MODE, Byte.toUnsignedInt(rawRecord[2]));
        assertEquals(3 + InfraredBrickCodec.BLOCKS_PER_BRICK * Short.BYTES,
                rawRecord.length);
        assertArrayEquals(raw, decodeSingle(rawRecord, 10));
        assertArrayEquals(indexedRecord, encodeBrick(9, indexed, false));
    }

    @Test
    void structuralMaximumResponseStaysBelowOneMiB() {
        byte[] records;
        try (InfraredBrickCodec.Builder builder =
                     new InfraredBrickCodec.Builder()) {
            short[] values = values(InfraredBrickCodec.INVALID_TEMPERATURE);
            for (int brick = 0;
                    brick < InfraredBrickCodec.MAX_LOCAL_BRICKS;
                    brick++) {
                values[0] = 0;
                values[1] = brick < 18_880
                        ? (short) 1
                        : InfraredBrickCodec.INVALID_TEMPERATURE;
                assertTrue(builder.writeBrick(brick, values, false));
            }
            assertEquals(935_296, builder.size());
            records = builder.toByteArray();
        }
        long[] presence =
                new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
        Arrays.fill(presence, -1L);
        FHResponseInfraredViewDataSyncPacket original =
                new FHResponseInfraredViewDataSyncPacket(
                        23,
                        new MinecraftThermalInput.InfraredSnapshot(
                                -4, 7, 2, 101, true,
                                presence, records));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        original.encode(encoded);
        assertTrue(encoded.readableBytes() < 1024 * 1024);

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

    private static short[] values(short value) {
        short[] values = new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
        Arrays.fill(values, value);
        return values;
    }

    private static byte[] encodeBrick(
            int localBrickIndex,
            short[] values,
            boolean omitInvalid
    ) {
        try (InfraredBrickCodec.Builder builder =
                     new InfraredBrickCodec.Builder()) {
            assertTrue(builder.writeBrick(
                    localBrickIndex, values, omitInvalid));
            return builder.toByteArray();
        }
    }

    private static short[] decodeSingle(
            byte[] record,
            int expectedLocalBrickIndex
    ) {
        FriendlyByteBuf input = new FriendlyByteBuf(
                Unpooled.wrappedBuffer(record));
        try {
            short[] decoded =
                    new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
            InfraredBrickCodec.Decoder decoder =
                    new InfraredBrickCodec.Decoder();
            assertEquals(expectedLocalBrickIndex,
                    decoder.readBrick(input, decoded));
            assertEquals(-1, decoder.readBrick(input, decoded));
            return decoded;
        } finally {
            input.release();
        }
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
