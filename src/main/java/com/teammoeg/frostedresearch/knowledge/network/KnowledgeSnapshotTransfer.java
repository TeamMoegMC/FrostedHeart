/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

/**
 * Full archives travel in compressed fragments without a per-archive transport size cap.
 */
public final class KnowledgeSnapshotTransfer {
    public static final int CHUNK_BYTES = 128 * 1024;

    private KnowledgeSnapshotTransfer() {
    }

    public record Fragment(UUID transfer, int index, int count, byte[] bytes) {
    }

    public static List<Fragment> split(CompoundTag snapshot) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(snapshot, output);
        byte[] compressed = output.toByteArray();
        int count = (compressed.length + CHUNK_BYTES - 1) / CHUNK_BYTES;
        UUID transfer = UUID.randomUUID();
        List<Fragment> fragments = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int start = index * CHUNK_BYTES;
            fragments.add(new Fragment(transfer, index, count,
                    Arrays.copyOfRange(compressed, start, Math.min(compressed.length, start + CHUNK_BYTES))));
        }
        return List.copyOf(fragments);
    }

    /**
     * The last installed view stays visible until one full replacement is assembled.
     */
    public static final class Accumulator {
        private UUID transfer;
        private byte[][] parts;
        private int received;

        public Optional<CompoundTag> accept(Fragment fragment) throws IOException {
            if (!fragment.transfer().equals(transfer)) {
                transfer = fragment.transfer();
                parts = new byte[fragment.count()][];
                received = 0;
            }
            if (parts[fragment.index()] == null) {
                parts[fragment.index()] = fragment.bytes();
                received++;
            }
            if (received != parts.length) return Optional.empty();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (byte[] part : parts) output.write(part);
            reset();
            return Optional.of(NbtIo.readCompressed(new ByteArrayInputStream(output.toByteArray())));
        }

        public void reset() {
            transfer = null;
            parts = null;
            received = 0;
        }
    }
}
