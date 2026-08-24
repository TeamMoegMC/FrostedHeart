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

import java.util.function.Supplier;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ForecastFrame;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.render.weather.ClientWeatherState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class FHClimatePacket implements CMessage {
    private final short[] data;
    private final long sec;
    private final long clockDayTime;
    private final ClimateType climate;
    private final ClimateType globalClimate;
    private final int wind;
    private final float humidity;

    public FHClimatePacket() {
        data = new short[0];
        sec = 0;
        clockDayTime = 0;
        climate = ClimateType.NONE;
        globalClimate = ClimateType.NONE;
        wind = 0;
        humidity = 0;
    }

    private FHClimatePacket(FriendlyByteBuf buffer) {
        data = SerializeUtil.readShortArray(buffer);
        sec = buffer.readVarLong();
        clockDayTime = buffer.readVarLong();
        climate = ClimateType.values()[buffer.readByte() & 0xff];
        globalClimate = ClimateType.values()[buffer.readByte() & 0xff];
        wind = buffer.readVarInt();
        humidity = buffer.readFloat();
    }

    public FHClimatePacket(WorldClimate climateData, ServerPlayer p) {
    	if(climateData==null) {
    		data = new short[0];
            sec = 0;
            clockDayTime = 0;
            climate = ClimateType.NONE;
            globalClimate = ClimateType.NONE;
            wind = 0;
            humidity = 0;
    	}else {
    		ChunkPos pos=new ChunkPos(p.blockPosition());
	        data = climateData.getForecastFrames(pos);
	        sec = climateData.getSec();
            clockDayTime = climateData.getClockDayTime();
	        climate = climateData.getClimate(pos);
	        globalClimate = climateData.getGlobalClimate();
            wind = climateData.getWind();
            humidity = climateData.getHumidity();
    	}
    }


	public void encode(FriendlyByteBuf buffer) {
        SerializeUtil.writeShortArray(buffer, data);
        buffer.writeVarLong(sec);
        buffer.writeVarLong(clockDayTime);
        buffer.writeByte((byte) climate.ordinal());
        buffer.writeByte((byte) globalClimate.ordinal());
        buffer.writeVarInt(wind);
        buffer.writeFloat(humidity);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Level level = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getWorld);
            if (level != null) {
                ClientWeatherState.INSTANCE.correctClock(sec, clockDayTime, level.getDayTime());
            }
            // Update client-side nbt
            if (data.length == 0) {
                ClientClimateData.clear();
                return;
            }
            int max = Math.min(ClientClimateData.forecastData.length, data.length);
            for (int i = 0; i < max; i++) {
                ClientClimateData.forecastData[i] = ForecastFrame.unpack(data[i]);
            }
            if (ClientClimateData.climate != climate) {
                ClientClimateData.climateChange = sec;
            }
            ClientClimateData.lastClimate = ClientClimateData.climate;
            ClientClimateData.climate = climate;
            ClientClimateData.globalClimate = globalClimate;

            ClientClimateData.secs = sec;
            ClientClimateData.wind = wind;
            ClientClimateData.humidity = humidity;
        });
        context.get().setPacketHandled(true);
    }
}
