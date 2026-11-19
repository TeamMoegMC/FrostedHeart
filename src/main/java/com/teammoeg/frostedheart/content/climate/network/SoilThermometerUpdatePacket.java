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
import com.teammoeg.frostedheart.content.agriculture.tree.CultivatedOakBlockEntity;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Server-authoritative growth readout for a crop or cultivated sapling. */
public record SoilThermometerUpdatePacket(BlockPos pos, float temperature, boolean exposedToBadWeather,
        @Nullable Component plantInfo) implements CMessage {

    /** Samples the growth temperature and the information displayed for the plant at {@code pos}. */
    public static SoilThermometerUpdatePacket sample(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return new SoilThermometerUpdatePacket(pos, Float.NaN, false, null);
        if (level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling) {
            return new SoilThermometerUpdatePacket(pos, WorldTemperature.getTempAtBlock(level, pos), false,
                    sapling.status());
        }
        PlantTempData data = PlantTempData.getPlantData(level.getBlockState(pos).getBlock());
        if (data == null) return new SoilThermometerUpdatePacket(pos, Float.NaN, false, null);
        return new SoilThermometerUpdatePacket(pos, WorldTemperature.getTempAtBlock(level, pos),
                WorldTemperature.plantWeatherBlocksGrowth(level, pos, data), null);
    }

    public SoilThermometerUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readNullable(FriendlyByteBuf::readComponent));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeFloat(temperature);
        buffer.writeBoolean(exposedToBadWeather);
        buffer.writeNullable(plantInfo, FriendlyByteBuf::writeComponent);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TemperatureGoogleRenderer.receivePlantReadout(
                pos, temperature, exposedToBadWeather, plantInfo));
        context.get().setPacketHandled(true);
    }
}
