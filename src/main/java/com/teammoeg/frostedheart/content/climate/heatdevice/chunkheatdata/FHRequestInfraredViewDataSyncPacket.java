/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.network.NetworkEvent;


public class FHRequestInfraredViewDataSyncPacket implements CMessage {
    private final ChunkPos chunkPos;
    private final int chunkRadius;

    public FHRequestInfraredViewDataSyncPacket(ChunkPos chunkPos, int chunkRadius) {
        this.chunkPos = chunkPos;
        this.chunkRadius = chunkRadius;
    }

    public FHRequestInfraredViewDataSyncPacket(FriendlyByteBuf buffer) {
        this.chunkPos = new ChunkPos(buffer.readVarLong());
        this.chunkRadius = buffer.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarLong(chunkPos.toLong());
        buffer.writeVarInt(chunkRadius);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player != null) {
                if (!player.level().getChunkSource().hasChunk(chunkPos.x, chunkPos.z) || chunkRadius > 10) {
                    // to avoid potential abuse / attacking from client
                    // make sure the chunk is loaded and the radius is not too large
                    return;
                }
                var heatAreas = new HashMap<BlockPos, IHeatArea>();
                for (int chunkOffsetX = -chunkRadius; chunkOffsetX <= chunkRadius; chunkOffsetX++) {
                    for (int chunkOffsetZ = -chunkRadius; chunkOffsetZ <= chunkRadius; chunkOffsetZ++) {
                        for (var heatArea : getChunkAdjust(player.level(),
                                new ChunkPos(chunkPos.x + chunkOffsetX, chunkPos.z + chunkOffsetZ))) {
                            heatAreas.put(heatArea.getCenter(), heatArea);
                        }
                    }
                }
                var heatAreaList = new ArrayList<>(heatAreas.values());

                FHNetwork.sendPlayer(player, new FHResponseInfraredViewDataSyncPacket(chunkPos, heatAreaList));
            }
        });
        context.get().setPacketHandled(true);
    }

    public static Collection<IHeatArea> getChunkAdjust(LevelReader world, ChunkPos chunkPos) {
        return new ArrayList<>(ChunkHeatData.get(world, chunkPos).map(ChunkHeatData::getAdjusters).orElseGet(Arrays::asList));
    }
}
