/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.network;

import java.util.function.Supplier;

import blusunrize.immersiveengineering.client.ClientUtils;
import com.teammoeg.frostedheart.FHUtil;
import com.teammoeg.frostedheart.world.chunkdata.ChunkData;
import com.teammoeg.frostedheart.world.chunkdata.ChunkDataCache;
import com.teammoeg.frostedheart.world.chunkdata.LerpFloatLayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * Sent from server -> client on chunk watch, partially syncs chunk data and updates the client cache
 */
public class ChunkWatchPacket
{
    private final int chunkX;
    private final int chunkZ;
    private final LerpFloatLayer temperatureLayer;

    public ChunkWatchPacket(int chunkX, int chunkZ, LerpFloatLayer temperatureLayer)
    {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.temperatureLayer = temperatureLayer;
    }

    ChunkWatchPacket(PacketBuffer buffer)
    {
        chunkX = buffer.readVarInt();
        chunkZ = buffer.readVarInt();
        temperatureLayer = new LerpFloatLayer(buffer);
    }

    void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(chunkX);
        buffer.writeVarInt(chunkZ);
        temperatureLayer.serialize(buffer);
    }

    void handle(Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> {
            ChunkPos pos = new ChunkPos(chunkX, chunkZ);
            // Update client-side chunk data capability
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> FHUtil::getWorld);
            if (world != null)
            {
                // First, synchronize the chunk data in the capability and cache.
                // Then, update the single data instance with the packet data
                IChunk chunk = world.chunkExists(chunkX, chunkZ) ? world.getChunk(chunkX, chunkZ) : null;
                ChunkData data = ChunkData.getCapability(chunk)
                    .map(dataIn -> {
                        ChunkDataCache.CLIENT.update(pos, dataIn);
                        return dataIn;
                    }).orElseGet(() -> ChunkDataCache.CLIENT.getOrCreate(pos));
                data.onUpdatePacket(temperatureLayer);
            }
        });
        context.get().setPacketHandled(true);
    }
}