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

import com.lowdragmc.lowdraglib.LDLib;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FHResponseInfraredViewDataSyncPacket implements CMessage {
    private final int requestId;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final int centerSectionY;
    private final int infraredEpoch;
    private final boolean full;
    private final long[] presence;
    private final byte[] brickRecords;

    public FHResponseInfraredViewDataSyncPacket(
            int requestId,
            MinecraftThermalInput.InfraredSnapshot snapshot
    ) {
        this(
                requestId,
                snapshot.centerChunkX(),
                snapshot.centerChunkZ(),
                snapshot.centerSectionY(),
                snapshot.infraredEpoch(),
                snapshot.full(),
                snapshot.presence(),
                snapshot.brickRecords());
    }

    private FHResponseInfraredViewDataSyncPacket(
            int requestId,
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            int infraredEpoch,
            boolean full,
            long[] presence,
            byte[] brickRecords
    ) {
        if (requestId < 0 || infraredEpoch < 0
                || presence == null || brickRecords == null
                || presence.length != 0
                && presence.length
                        != FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS
                || full && presence.length
                        != FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS
                || brickRecords.length > InfraredBrickCodec.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("invalid infrared response");
        }
        this.requestId = requestId;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.centerSectionY = centerSectionY;
        this.infraredEpoch = infraredEpoch;
        this.full = full;
        this.presence = presence;
        this.brickRecords = brickRecords;
    }

    public FHResponseInfraredViewDataSyncPacket(FriendlyByteBuf buffer) {
        requestId = buffer.readVarInt();
        centerChunkX = buffer.readInt();
        centerChunkZ = buffer.readInt();
        centerSectionY = buffer.readInt();
        infraredEpoch = buffer.readVarInt();
        full = buffer.readBoolean();
        if (buffer.readBoolean()) {
            presence = new long[
                    FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
            for (int index = 0; index < presence.length; index++) {
                presence[index] = buffer.readLong();
            }
        } else {
            presence = new long[0];
        }
        brickRecords = buffer.readByteArray(
                InfraredBrickCodec.MAX_PAYLOAD_BYTES);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeInt(centerChunkX);
        buffer.writeInt(centerChunkZ);
        buffer.writeInt(centerSectionY);
        buffer.writeVarInt(infraredEpoch);
        buffer.writeBoolean(full);
        buffer.writeBoolean(presence.length != 0);
        if (presence.length != 0) {
            for (long word : presence) {
                buffer.writeLong(word);
            }
        }
        buffer.writeByteArray(brickRecords);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!LDLib.isClient()) {
                return;
            }
            Runnable update = () -> InfraredViewRenderer.updateData(
                    requestId,
                    centerChunkX,
                    centerChunkZ,
                    centerSectionY,
                    infraredEpoch,
                    full,
                    presence,
                    brickRecords);
            if (RenderSystem.isOnRenderThread()) {
                update.run();
            } else {
                RenderSystem.recordRenderCall(update::run);
            }
        });
        context.get().setPacketHandled(true);
    }
}
