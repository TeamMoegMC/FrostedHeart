/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.network.climate;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.client.ClientForecastData;
import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.climate.ClimateData.TemperatureFrame;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHClimatePacket {
    private final short[] data;
    private final long sec;
    public FHClimatePacket(ClimateData climateData) {
        data = climateData.getFrames();
        sec= climateData.getSec();
    }
    public FHClimatePacket() {
        data = new short[0];
        sec=0;
    }

    public FHClimatePacket(PacketBuffer buffer) {
        data=SerializeUtil.readShortArray(buffer);
        sec=buffer.readVarLong();
    }

    public void encode(PacketBuffer buffer) {
        SerializeUtil.writeShortArray(buffer, data);
        buffer.writeVarLong(sec);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
        	if(data.length==0) {
        		ClientForecastData.clear();
        		return;
        	}
        	int max=Math.min(ClientForecastData.tfs.length, data.length);
            for(int i=0;i<max;i++) {
            	ClientForecastData.tfs[i]=TemperatureFrame.unpack(data[i]);
            }
            ClientForecastData.secs=sec;
        });
        context.get().setPacketHandled(true);
    }
}
