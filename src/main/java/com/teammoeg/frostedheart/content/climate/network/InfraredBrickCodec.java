/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;

import java.util.Arrays;

/** Flat changed-Brick wire codec shared by server encoding and client decode. */
public final class InfraredBrickCodec {
    public static final int BLOCKS_PER_BRICK = 64;
    public static final int MAX_LOCAL_BRICKS = 729 * 64;
    public static final int MAX_PAYLOAD_BYTES = 960 * 1024;
    public static final short INVALID_TEMPERATURE = Short.MIN_VALUE;

    private static final int INVALID = 0;
    private static final int UNIFORM = 1;
    private static final int INDEXED = 2;
    private static final int RAW = 3;

    private InfraredBrickCodec() {
    }

    public static final class Builder implements AutoCloseable {
        private final FriendlyByteBuf output = new FriendlyByteBuf(
                Unpooled.buffer(1024, MAX_PAYLOAD_BYTES));
        private final short[] dictionary = new short[BLOCKS_PER_BRICK];
        private final int[] indexes = new int[BLOCKS_PER_BRICK];
        private final SimpleBitStorage[] storageByBits = storages();

        public void reset() {
            output.clear();
        }

        public boolean writeBrick(
                int localBrickIndex,
                short[] values,
                boolean omitInvalid
        ) {
            requireLocalBrick(localBrickIndex);
            requireValues(values);
            int dictionarySize = 0;
            for (int index = 0; index < BLOCKS_PER_BRICK; index++) {
                short value = values[index];
                int dictionaryIndex = 0;
                while (dictionaryIndex < dictionarySize
                        && dictionary[dictionaryIndex] != value) {
                    dictionaryIndex++;
                }
                if (dictionaryIndex == dictionarySize) {
                    dictionary[dictionarySize++] = value;
                }
                indexes[index] = dictionaryIndex;
            }

            if (dictionarySize == 1) {
                return dictionary[0] == INVALID_TEMPERATURE
                        ? writeInvalid(localBrickIndex, omitInvalid)
                        : writeUniform(localBrickIndex, dictionary[0]);
            }

            int bits = Mth.ceillog2(dictionarySize);
            SimpleBitStorage storage = storageByBits[bits];
            int indexedBytes = 2
                    + storage.getRaw().length * Long.BYTES
                    + dictionarySize * Short.BYTES;
            if (indexedBytes < 1 + BLOCKS_PER_BRICK * Short.BYTES) {
                for (int index = 0; index < BLOCKS_PER_BRICK; index++) {
                    storage.set(index, indexes[index]);
                }
                writeIdentity(localBrickIndex, INDEXED);
                output.writeByte(dictionarySize);
                for (int index = 0; index < dictionarySize; index++) {
                    output.writeShort(dictionary[index]);
                }
                for (long packed : storage.getRaw()) {
                    output.writeLong(packed);
                }
            } else {
                writeIdentity(localBrickIndex, RAW);
                for (short value : values) {
                    output.writeShort(value);
                }
            }
            return true;
        }

        public boolean writeInvalid(
                int localBrickIndex,
                boolean omitInvalid
        ) {
            requireLocalBrick(localBrickIndex);
            if (omitInvalid) {
                return false;
            }
            writeIdentity(localBrickIndex, INVALID);
            return true;
        }

        public boolean writeUniform(int localBrickIndex, short value) {
            requireLocalBrick(localBrickIndex);
            if (value == INVALID_TEMPERATURE) {
                return writeInvalid(localBrickIndex, false);
            }
            writeIdentity(localBrickIndex, UNIFORM);
            output.writeShort(value);
            return true;
        }

        public int size() {
            return output.writerIndex();
        }

        public byte[] toByteArray() {
            return ByteBufUtil.getBytes(output, 0, output.writerIndex());
        }

        private void writeIdentity(int localBrickIndex, int mode) {
            output.writeShort(localBrickIndex);
            output.writeByte(mode);
        }

        @Override
        public void close() {
            output.release();
        }
    }

    public static final class Decoder {
        private final short[] dictionary = new short[BLOCKS_PER_BRICK];
        private final SimpleBitStorage[] storageByBits = storages();

        /** Returns {@code -1} after the final record. */
        public int readBrick(FriendlyByteBuf input, short[] values) {
            requireValues(values);
            if (!input.isReadable()) {
                return -1;
            }
            int localBrickIndex = input.readUnsignedShort();
            requireLocalBrick(localBrickIndex);
            int mode = input.readUnsignedByte();
            switch (mode) {
                case INVALID -> Arrays.fill(values, INVALID_TEMPERATURE);
                case UNIFORM -> Arrays.fill(values, input.readShort());
                case INDEXED -> readIndexed(input, values);
                case RAW -> {
                    for (int index = 0; index < BLOCKS_PER_BRICK; index++) {
                        values[index] = input.readShort();
                    }
                }
                default -> throw new IllegalArgumentException(
                        "unknown infrared Brick mode: " + mode);
            }
            return localBrickIndex;
        }

        private void readIndexed(FriendlyByteBuf input, short[] values) {
            int dictionarySize = input.readUnsignedByte();
            if (dictionarySize < 2 || dictionarySize > BLOCKS_PER_BRICK) {
                throw new IllegalArgumentException(
                        "invalid infrared temperature dictionary");
            }
            for (int index = 0; index < dictionarySize; index++) {
                dictionary[index] = input.readShort();
            }
            int bits = Mth.ceillog2(dictionarySize);
            SimpleBitStorage storage = storageByBits[bits];
            long[] packed = storage.getRaw();
            for (int index = 0; index < packed.length; index++) {
                packed[index] = input.readLong();
            }
            for (int index = 0; index < BLOCKS_PER_BRICK; index++) {
                int dictionaryIndex = storage.get(index);
                if (dictionaryIndex >= dictionarySize) {
                    throw new IllegalArgumentException(
                            "infrared temperature index is outside its dictionary");
                }
                values[index] = dictionary[dictionaryIndex];
            }
        }
    }

    private static SimpleBitStorage[] storages() {
        SimpleBitStorage[] result = new SimpleBitStorage[7];
        for (int bits = 1; bits <= 6; bits++) {
            result[bits] = new SimpleBitStorage(bits, BLOCKS_PER_BRICK);
        }
        return result;
    }

    private static void requireLocalBrick(int localBrickIndex) {
        if (localBrickIndex < 0 || localBrickIndex >= MAX_LOCAL_BRICKS) {
            throw new IllegalArgumentException("infrared local Brick is out of range");
        }
    }

    private static void requireValues(short[] values) {
        if (values == null || values.length != BLOCKS_PER_BRICK) {
            throw new IllegalArgumentException(
                    "infrared Brick requires 64 block temperatures");
        }
    }
}
