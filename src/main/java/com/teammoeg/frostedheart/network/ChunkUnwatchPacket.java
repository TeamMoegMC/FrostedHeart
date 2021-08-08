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

import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCache;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server -> client, clears the client side chunk data cache when a chunk is unwatched
 */
public class ChunkUnwatchPacket {
    private final int chunkX;
    private final int chunkZ;

    public ChunkUnwatchPacket(ChunkPos pos) {
        this.chunkX = pos.x;
        this.chunkZ = pos.z;
    }

    public ChunkUnwatchPacket(PacketBuffer buffer) {
        this.chunkX = buffer.readVarInt();
        this.chunkZ = buffer.readVarInt();
    }

    void encode(PacketBuffer buffer) {
        buffer.writeVarInt(chunkX);
        buffer.writeVarInt(chunkZ);
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ChunkDataCache.CLIENT.remove(new ChunkPos(chunkX, chunkZ)));
        context.get().setPacketHandled(true);
    }
}