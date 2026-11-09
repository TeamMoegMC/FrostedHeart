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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client-carried display baseline and previous analytic-field footprint. */
public record FHRequestInfraredViewDataSyncPacket(
        int requestId, boolean forceFull, long knownGeneration, long knownCenter, int lastEpoch,
        long[] knownPresence, boolean knownReadable, long[] knownFieldPages, long knownStoredEpoch,
        long knownStoredSampleTick, int knownDisplayRequestId) implements CMessage {
    public static final int PRESENCE_WORDS = 12;

    public FHRequestInfraredViewDataSyncPacket {
        if (knownPresence.length != PRESENCE_WORDS) throw new IllegalArgumentException("infrared presence length");
    }

    public FHRequestInfraredViewDataSyncPacket(int requestId, boolean forceFull, long knownGeneration, long knownCenter,
            int lastEpoch, long[] knownPresence, boolean knownReadable, long[] knownFieldPages, long knownStoredEpoch,
            long knownStoredSampleTick) {
        this(requestId, forceFull, knownGeneration, knownCenter, lastEpoch, knownPresence, knownReadable,
                knownFieldPages, knownStoredEpoch, knownStoredSampleTick, -1);
    }

    public FHRequestInfraredViewDataSyncPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readBoolean(), b.readVarLong(), b.readLong(), b.readVarInt(),
                readPresence(b), b.readBoolean(), readFieldPages(b), b.readVarLong(), b.readLong(), b.readVarInt());
    }

    static long[] readPresence(FriendlyByteBuf b) {
        return readPresence(b, false);
    }

    private static long[] readPresence(FriendlyByteBuf b, boolean emptyArray) {
        int mode = b.readUnsignedByte();
        if (mode == 0) return new long[emptyArray ? 0 : PRESENCE_WORDS];
        long[] result = new long[PRESENCE_WORDS];
        if (mode == 1) {
            for (int i = 0; i < result.length; i++) result[i] = b.readLong();
        } else if (mode == 2) {
            int count = b.readVarInt(), page = -1;
            if (count < 0 || count > PRESENCE_WORDS * 64) throw new IllegalArgumentException("infrared mask count");
            for (int i = 0; i < count; i++) {
                page += b.readVarInt();
                if (page < 0 || page >= PRESENCE_WORDS * 64) throw new IllegalArgumentException("infrared mask index");
                result[page >>> 6] |= 1L << (page & 63);
            }
        } else throw new IllegalArgumentException("infrared mask mode");
        return result;
    }

    static long[] readFieldPages(FriendlyByteBuf b) {
        return readPresence(b, true);
    }

    static void writeFieldPages(FriendlyByteBuf b, long[] pages) {
        int count = 0;
        for (long word : pages) count += Long.bitCount(word);
        if (count == 0) { b.writeByte(0); return; }
        int sparseBytes = 2, previous = -1;
        if (count <= 95) for (int word = 0; word < pages.length; word++) {
            long remaining = pages[word];
            while (remaining != 0) {
                int page = word * 64 + Long.numberOfTrailingZeros(remaining);
                sparseBytes += page - previous < 128 ? 1 : 2;
                previous = page;
                remaining &= remaining - 1;
            }
        } else sparseBytes = Integer.MAX_VALUE;
        if (sparseBytes < 1 + PRESENCE_WORDS * 8) {
            b.writeByte(2); b.writeVarInt(count); previous = -1;
            for (int word = 0; word < pages.length; word++) {
                long remaining = pages[word];
                while (remaining != 0) {
                    int page = word * 64 + Long.numberOfTrailingZeros(remaining);
                    b.writeVarInt(page - previous); previous = page;
                    remaining &= remaining - 1;
                }
            }
        } else {
            b.writeByte(1);
            for (long word : pages) b.writeLong(word);
        }
    }

    @Override public void encode(FriendlyByteBuf b) {
        b.writeVarInt(requestId);
        b.writeBoolean(forceFull);
        b.writeVarLong(knownGeneration);
        b.writeLong(knownCenter);
        b.writeVarInt(lastEpoch);
        writeFieldPages(b, knownPresence);
        b.writeBoolean(knownReadable);
        writeFieldPages(b, forceFull ? new long[0] : knownFieldPages);
        b.writeVarLong(knownStoredEpoch);
        b.writeLong(knownStoredSampleTick);
        b.writeVarInt(knownDisplayRequestId);
    }

    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player == null) return;
            InfraredDiagnostics.request(player,this);
            var snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(player, this);
            InfraredDiagnostics.response(player,requestId,snapshot);
            InfraredTransfers.start(player,requestId,snapshot);
        });
        context.get().setPacketHandled(true);
    }
}
