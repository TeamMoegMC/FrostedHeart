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

import java.util.function.Supplier;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public final class FHBodyDataSyncPacket implements CMessage {
    private static final byte VERSION = 1;

    private final short environmentDeciC;
    private final short coreCentiC;
    private final byte statusFlags;

    public FHBodyDataSyncPacket(Player player) {
        PlayerTemperatureData data = PlayerTemperatureData
                .getCapability(player).orElse(null);
        environmentDeciC = quantize(
                data == null ? -20.0F : data.getEnvTemp(), 10.0F);
        coreCentiC = quantize(
                data == null ? 37.0F : data.getAbsoluteCoreBodyTemp(),
                100.0F);
        statusFlags = data == null ? 0 : data.getThermalStatusFlags();
    }

    public FHBodyDataSyncPacket(FriendlyByteBuf buffer) {
        buffer.readByte();
        environmentDeciC = buffer.readShort();
        coreCentiC = buffer.readShort();
        statusFlags = buffer.readByte();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(VERSION);
        buffer.writeShort(environmentDeciC);
        buffer.writeShort(coreCentiC);
        buffer.writeByte(statusFlags);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Player player = ClientUtils.getPlayer();
            if (player == null) return;
            PlayerTemperatureData.getCapability(player).ifPresent(data ->
                    data.applyClientThermalSync(
                            environmentDeciC / 10.0F,
                            coreCentiC / 100.0F,
                            statusFlags));
        });
        context.get().setPacketHandled(true);
    }

    private static short quantize(float value, float scale) {
        if (!Float.isFinite(value)) return 0;
        int quantized = Math.round(value * scale);
        return (short) Mth.clamp(
                quantized, Short.MIN_VALUE, Short.MAX_VALUE);
    }
}
