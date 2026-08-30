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
    public static final int RECORD_SHORTS = 65;
    private static final int TEMPERATURES_PER_PAGE = 64;

    private final int requestId;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final int centerSectionY;
    private final long temperatureChangeId;
    private final boolean full;
    private final short[] pageRecords;

    public FHResponseInfraredViewDataSyncPacket(
            int requestId,
            MinecraftThermalInput.InfraredSnapshot snapshot
    ) {
        this(
                requestId,
                snapshot.centerChunkX(),
                snapshot.centerChunkZ(),
                snapshot.centerSectionY(),
                snapshot.temperatureChangeId(),
                snapshot.full(),
                snapshot.pageRecords());
    }

    private FHResponseInfraredViewDataSyncPacket(
            int requestId,
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            long temperatureChangeId,
            boolean full,
            short[] pageRecords
    ) {
        if (requestId < 0 || temperatureChangeId < 0L
                || pageRecords == null
                || pageRecords.length % RECORD_SHORTS != 0) {
            throw new IllegalArgumentException("invalid infrared response");
        }
        this.requestId = requestId;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.centerSectionY = centerSectionY;
        this.temperatureChangeId = temperatureChangeId;
        this.full = full;
        this.pageRecords = pageRecords;
    }

    public FHResponseInfraredViewDataSyncPacket(FriendlyByteBuf buffer) {
        requestId = buffer.readVarInt();
        centerChunkX = buffer.readInt();
        centerChunkZ = buffer.readInt();
        centerSectionY = buffer.readInt();
        temperatureChangeId = buffer.readVarLong();
        full = buffer.readBoolean();
        int pageCount = buffer.readVarInt();
        pageRecords = new short[Math.multiplyExact(pageCount, RECORD_SHORTS)];
        for (int page = 0; page < pageCount; page++) {
            int offset = page * RECORD_SHORTS;
            pageRecords[offset] = (short) buffer.readVarInt();
            for (int brick = 0; brick < TEMPERATURES_PER_PAGE; brick++) {
                pageRecords[offset + 1 + brick] = buffer.readShort();
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeInt(centerChunkX);
        buffer.writeInt(centerChunkZ);
        buffer.writeInt(centerSectionY);
        buffer.writeVarLong(temperatureChangeId);
        buffer.writeBoolean(full);
        int pageCount = pageRecords.length / RECORD_SHORTS;
        buffer.writeVarInt(pageCount);
        for (int page = 0; page < pageCount; page++) {
            int offset = page * RECORD_SHORTS;
            buffer.writeVarInt(Short.toUnsignedInt(pageRecords[offset]));
            for (int brick = 0; brick < TEMPERATURES_PER_PAGE; brick++) {
                buffer.writeShort(pageRecords[offset + 1 + brick]);
            }
        }
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
                    temperatureChangeId,
                    full,
                    pageRecords);
            if (RenderSystem.isOnRenderThread()) {
                update.run();
            } else {
                RenderSystem.recordRenderCall(update::run);
            }
        });
        context.get().setPacketHandled(true);
    }
}
