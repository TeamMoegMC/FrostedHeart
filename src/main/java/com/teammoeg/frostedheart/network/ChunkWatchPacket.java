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

package com.teammoeg.frostedheart.network;

import com.teammoeg.frostedheart.client.util.FHClientUtils;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.climate.chunkdata.ITemperatureAdjust;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from server -> client on chunk watch, partially syncs chunk data and updates the client cache
 */
public class ChunkWatchPacket {
    private final int chunkX;
    private final int chunkZ;
    private final List<ITemperatureAdjust> tempMatrix;

    public ChunkWatchPacket(int chunkX, int chunkZ,List<ITemperatureAdjust> tempMatrix) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.tempMatrix = tempMatrix;
    }

    ChunkWatchPacket(PacketBuffer buffer) {
        chunkX = buffer.readVarInt();
        chunkZ = buffer.readVarInt();
        tempMatrix = new LinkedList<>();
        int len=buffer.readVarInt();
        for(int i=0;i<len;i++)
        	tempMatrix.add(ITemperatureAdjust.valueOf(buffer));
    }

    void encode(PacketBuffer buffer) {
        buffer.writeVarInt(chunkX);
        buffer.writeVarInt(chunkZ);
        buffer.writeVarInt(tempMatrix.size());
        for(ITemperatureAdjust adjust:tempMatrix)
        	adjust.serialize(buffer);;
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ChunkPos pos = new ChunkPos(chunkX, chunkZ);
            // Update client-side chunk data capability
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> FHClientUtils::getWorld);
            if (world != null) {
                // First, synchronize the chunk data in the capability and cache.
                // Then, update the single data instance with the packet data
                IChunk chunk = world.chunkExists(chunkX, chunkZ) ? world.getChunk(chunkX, chunkZ) : null;
                ChunkData data = ChunkData.getCapability(chunk)
                        .map(dataIn -> {
                            ChunkDataCache.CLIENT.update(pos, dataIn);
                            return dataIn;
                        }).orElseGet(() -> ChunkDataCache.CLIENT.getOrCreate(pos));
                data.onUpdatePacket(tempMatrix);
            }
        });
        context.get().setPacketHandled(true);
    }
}