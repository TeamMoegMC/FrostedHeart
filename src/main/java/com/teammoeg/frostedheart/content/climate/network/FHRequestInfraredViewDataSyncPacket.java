/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FHRequestInfraredViewDataSyncPacket implements CMessage {
    public static final int PRESENCE_WORDS = 12;
    private static final int DENSE_DORMANT_PRESENCE = 48;
    private static final long[] EMPTY_DORMANT_PRESENCE = new long[PRESENCE_WORDS];

    private final int requestId;
    private final boolean forceFull;
    private final int lastInfraredEpoch;
    private final long[] knownPresence;
    private final long lastDormantRevision;
    private final long[] knownDormantPresence;

    public FHRequestInfraredViewDataSyncPacket(
            int requestId,
            boolean forceFull,
            int lastInfraredEpoch,
            long[] knownPresence,
            long lastDormantRevision,
            long[] knownDormantPresence
    ) {
        if (requestId < 0 || lastInfraredEpoch < 0
                || knownPresence == null
                || knownPresence.length != PRESENCE_WORDS
                || lastDormantRevision < 0L || knownDormantPresence == null
                || knownDormantPresence.length != PRESENCE_WORDS) {
            throw new IllegalArgumentException("invalid infrared request");
        }
        this.requestId = requestId;
        this.forceFull = forceFull;
        this.lastInfraredEpoch = lastInfraredEpoch;
        this.knownPresence = knownPresence.clone();
        this.lastDormantRevision = lastDormantRevision;
        this.knownDormantPresence = forceFull
                ? EMPTY_DORMANT_PRESENCE : knownDormantPresence.clone();
    }

    public FHRequestInfraredViewDataSyncPacket(FriendlyByteBuf buffer) {
        requestId = buffer.readVarInt();
        forceFull = buffer.readBoolean();
        lastInfraredEpoch = buffer.readVarInt();
        knownPresence = new long[PRESENCE_WORDS];
        for (int index = 0; index < PRESENCE_WORDS; index++) {
            knownPresence[index] = buffer.readLong();
        }
        int count = forceFull ? 0 : buffer.readUnsignedByte();
        if (count == 0) {
            knownDormantPresence = EMPTY_DORMANT_PRESENCE;
            lastDormantRevision = 0L;
        } else {
            knownDormantPresence = new long[PRESENCE_WORDS];
            if (count == DENSE_DORMANT_PRESENCE) {
                for (int index = 0; index < PRESENCE_WORDS; index++) {
                    knownDormantPresence[index] = buffer.readLong();
                }
            } else if (count < DENSE_DORMANT_PRESENCE) {
                for (int index = 0; index < count; index++) {
                    int section = buffer.readUnsignedShort();
                    if (section >= 729) {
                        throw new IllegalArgumentException("invalid dormant section index");
                    }
                    knownDormantPresence[section >>> 6] |= 1L << (section & 63);
                }
            } else {
                throw new IllegalArgumentException("invalid dormant presence encoding");
            }
            lastDormantRevision = buffer.readVarLong();
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeBoolean(forceFull);
        buffer.writeVarInt(lastInfraredEpoch);
        for (long word : knownPresence) {
            buffer.writeLong(word);
        }
        if (forceFull) {
            return;
        }
        int count = 0;
        for (long word : knownDormantPresence) {
            count += Long.bitCount(word);
        }
        buffer.writeByte(Math.min(count, DENSE_DORMANT_PRESENCE));
        if (count >= DENSE_DORMANT_PRESENCE) {
            for (long word : knownDormantPresence) {
                buffer.writeLong(word);
            }
        } else {
            for (int index = 0; index < PRESENCE_WORDS; index++) {
                long remaining = knownDormantPresence[index];
                while (remaining != 0L) {
                    buffer.writeShort(index * 64 + Long.numberOfTrailingZeros(remaining));
                    remaining &= remaining - 1L;
                }
            }
        }
        if (count != 0) {
            buffer.writeVarLong(lastDormantRevision);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player == null) {
                return;
            }
            MinecraftThermalInput.InfraredSnapshot snapshot =
                    MinecraftThermalInput.gameplayInfraredSnapshot(
                            player,
                            forceFull,
                            lastInfraredEpoch,
                            knownPresence,
                            lastDormantRevision,
                            knownDormantPresence);
            if (snapshot != null) {
                FHNetwork.INSTANCE.sendPlayer(
                        player,
                        new FHResponseInfraredViewDataSyncPacket(
                                requestId, snapshot));
            }
        });
        context.get().setPacketHandled(true);
    }
}
