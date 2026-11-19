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

package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoilThermometerUpdatePacket(float temperature, net.minecraft.core.BlockPos pos,
        @org.jetbrains.annotations.Nullable net.minecraft.network.chat.Component plantInfo) implements CMessage {

    public SoilThermometerUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readBlockPos(), buffer.readNullable(FriendlyByteBuf::readComponent));
    }

    /** Server-authoritative snapshot for the thermometer overlay, without tree height or seed. */
    public static SoilThermometerUpdatePacket sample(net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos) {
        var info = level.getBlockEntity(pos) instanceof com.teammoeg.frostedheart.content.agriculture.tree.CultivatedOakBlockEntity oak
                ? oak.status() : null;
        return new SoilThermometerUpdatePacket(
                com.teammoeg.frostedheart.content.climate.WorldTemperature.block(level, pos), pos, info);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(temperature);
        buffer.writeBlockPos(pos);
        buffer.writeNullable(plantInfo, FriendlyByteBuf::writeComponent);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            TemperatureGoogleRenderer.cachedTemperature = temperature;
            TemperatureGoogleRenderer.cachedPlantInfoPos = pos;
            TemperatureGoogleRenderer.cachedPlantInfo = plantInfo;
        });
        context.get().setPacketHandled(true);
    }
}
