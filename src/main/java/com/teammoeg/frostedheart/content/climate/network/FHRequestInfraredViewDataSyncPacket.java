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

    private final int requestId;
    private final boolean forceFull;
    private final long lastTemperatureChangeId;
    private final long[] knownPresence;

    public FHRequestInfraredViewDataSyncPacket(
            int requestId,
            boolean forceFull,
            long lastTemperatureChangeId,
            long[] knownPresence
    ) {
        if (requestId < 0 || lastTemperatureChangeId < 0L
                || knownPresence == null
                || knownPresence.length != PRESENCE_WORDS) {
            throw new IllegalArgumentException("invalid infrared request");
        }
        this.requestId = requestId;
        this.forceFull = forceFull;
        this.lastTemperatureChangeId = lastTemperatureChangeId;
        this.knownPresence = knownPresence.clone();
    }

    public FHRequestInfraredViewDataSyncPacket(FriendlyByteBuf buffer) {
        requestId = buffer.readVarInt();
        forceFull = buffer.readBoolean();
        lastTemperatureChangeId = buffer.readVarLong();
        knownPresence = new long[PRESENCE_WORDS];
        for (int index = 0; index < PRESENCE_WORDS; index++) {
            knownPresence[index] = buffer.readLong();
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeBoolean(forceFull);
        buffer.writeVarLong(lastTemperatureChangeId);
        for (long word : knownPresence) {
            buffer.writeLong(word);
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
                            lastTemperatureChangeId,
                            knownPresence);
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
