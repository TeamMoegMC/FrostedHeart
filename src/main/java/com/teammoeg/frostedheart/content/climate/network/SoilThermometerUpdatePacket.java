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

/** Server-authoritative crop temperature/weather and cultivated-tree progress snapshot. */
public record SoilThermometerUpdatePacket(BlockPos pos, float temperature, boolean exposedToBadWeather,
        @Nullable Component plantInfo, @Nullable TreeInfo treeInfo) implements CMessage {

    /** Uses the same material-priority temperature as growth, without exposing internal tree budgets. */
    public static SoilThermometerUpdatePacket sample(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return new SoilThermometerUpdatePacket(pos, Float.NaN, false, null, null);
        if (level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling) {
            return new SoilThermometerUpdatePacket(pos, WorldTemperature.getTempAtBlock(level, pos), false,
                    sapling.status(), sapling.thermometerInfo());
        }
        PlantTempData data = PlantTempData.getPlantData(level.getBlockState(pos).getBlock());
        if (data == null) return new SoilThermometerUpdatePacket(pos, Float.NaN, false, null, null);
        return new SoilThermometerUpdatePacket(pos, WorldTemperature.getTempAtBlock(level, pos),
                WorldTemperature.plantWeatherBlocksGrowth(level, pos, data), null, null);
    }

    public SoilThermometerUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readNullable(FriendlyByteBuf::readComponent), buffer.readNullable(TreeInfo::new));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos); buffer.writeFloat(temperature); buffer.writeBoolean(exposedToBadWeather);
        buffer.writeNullable(plantInfo, FriendlyByteBuf::writeComponent);
        buffer.writeNullable(treeInfo, (out, tree) -> tree.encode(out));
    }

    /** Only player-facing growth and environment data; no yield credit, target height or seed. */
    public record TreeInfo(int stage, float progress, float requiredProgress, String waiting,
            float minTemperature, float maxTemperature, float idealMin, float idealMax, int minLight) {
        public TreeInfo(FriendlyByteBuf in) {
            this(in.readVarInt(), in.readFloat(), in.readFloat(), in.readUtf(),
                    in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readVarInt());
        }
        public void encode(FriendlyByteBuf out) {
            out.writeVarInt(stage); out.writeFloat(progress); out.writeFloat(requiredProgress); out.writeUtf(waiting);
            out.writeFloat(minTemperature); out.writeFloat(maxTemperature); out.writeFloat(idealMin); out.writeFloat(idealMax);
            out.writeVarInt(minLight);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TemperatureGoogleRenderer.receivePlantReadout(
                pos, temperature, exposedToBadWeather, plantInfo, treeInfo));
        context.get().setPacketHandled(true);
    }
}
