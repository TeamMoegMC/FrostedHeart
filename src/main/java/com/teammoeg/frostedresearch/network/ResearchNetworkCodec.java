/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.network;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedresearch.ResearchCatalog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import com.teammoeg.frostedresearch.FRMain;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded, defensive helpers shared by research packets. */
final class ResearchNetworkCodec {
    static final int MAX_ID_LENGTH = ResearchCatalog.MAX_STABLE_ID_LENGTH;
    private static final String PAYLOAD = "payload";
    private static final int DIAGNOSTIC_LIMIT = 20;
    private static final AtomicInteger DIAGNOSTICS = new AtomicInteger();

    private ResearchNetworkCodec() {
    }

    static <T> CompoundTag encode(Codec<T> codec, T value) {
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value)
                .getOrThrow(false, message -> reject("encode: " + message));
        CompoundTag envelope = new CompoundTag();
        envelope.put(PAYLOAD, encoded);
        return envelope;
    }

    @Nullable
    static <T> T decode(Codec<T> codec, @Nullable CompoundTag envelope, String label) {
        if (envelope == null || !envelope.contains(PAYLOAD)) {
            reject(label + ": missing payload");
            return null;
        }
        try {
            return codec.parse(NbtOps.INSTANCE, envelope.get(PAYLOAD))
                    .resultOrPartial(message -> reject(label + ": " + message))
                    .orElse(null);
        } catch (RuntimeException e) {
            reject(label + ": " + e.getMessage());
            return null;
        }
    }

    @Nullable
    static CompoundTag readPayload(FriendlyByteBuf buffer, String label) {
        try {
            return buffer.readNbt();
        } catch (RuntimeException e) {
            reject(label + ": malformed or oversized NBT payload");
            return null;
        }
    }

    @Nullable
    static String readId(FriendlyByteBuf buffer, String label) {
        try {
            String id = buffer.readUtf(MAX_ID_LENGTH);
            if (id.isBlank()) {
                reject(label + ": blank id");
                return null;
            }
            return id;
        } catch (RuntimeException e) {
            reject(label + ": malformed or oversized id");
            return null;
        }
    }

    static String readOptionalId(FriendlyByteBuf buffer, String label) {
        try {
            return buffer.readUtf(MAX_ID_LENGTH);
        } catch (RuntimeException e) {
            reject(label + ": malformed or oversized id");
            return "";
        }
    }

    static int readNonNegativeVarInt(FriendlyByteBuf buffer, String label) {
        try {
            int value = buffer.readVarInt();
            if (value < 0) {
                reject(label + ": negative value");
                return -1;
            }
            return value;
        } catch (RuntimeException e) {
            reject(label + ": malformed integer");
            return -1;
        }
    }

    static void reject(String message) {
        int occurrence = DIAGNOSTICS.incrementAndGet();
        if (occurrence <= DIAGNOSTIC_LIMIT) {
            FRMain.LOGGER.warn("Discarded research network input: {}", message);
            if (occurrence == DIAGNOSTIC_LIMIT) {
                FRMain.LOGGER.warn("Further research network diagnostics will be suppressed");
            }
        }
    }
}
