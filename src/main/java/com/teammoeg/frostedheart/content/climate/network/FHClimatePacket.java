/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.TemperatureFrame;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.chorda.util.io.SerializeUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class FHClimatePacket implements CMessage {
    private final short[] data;
    private final long sec;
    private final ClimateType climate;

    public FHClimatePacket() {
        data = new short[0];
        sec = 0;
        climate = ClimateType.NONE;
    }

    public FHClimatePacket(FriendlyByteBuf buffer) {
        data = SerializeUtil.readShortArray(buffer);
        sec = buffer.readVarLong();
        climate = ClimateType.values()[buffer.readByte() & 0xff];
    }

    public FHClimatePacket(WorldClimate climateData) {
    	if(climateData==null) {
    		data = new short[0];
            sec = 0;
            climate = ClimateType.NONE;
    	}else {
	        data = climateData.getFrames();
	        sec = climateData.getSec();
	        climate = climateData.getClimate();
    	}
    }

    public void encode(FriendlyByteBuf buffer) {
        SerializeUtil.writeShortArray(buffer, data);
        buffer.writeVarLong(sec);
        buffer.writeByte((byte) climate.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            if (data.length == 0) {
                ClientClimateData.clear();
                return;
            }
            int max = Math.min(ClientClimateData.forecastData.length, data.length);
            for (int i = 0; i < max; i++) {
                ClientClimateData.forecastData[i] = TemperatureFrame.unpack(data[i]);
            }
            if (ClientClimateData.climate != climate) {
                ClientClimateData.climateChange = sec;
            }
            ClientClimateData.lastClimate = ClientClimateData.climate;
            ClientClimateData.climate = climate;

            ClientClimateData.secs = sec;
        });
        context.get().setPacketHandled(true);
    }
}
