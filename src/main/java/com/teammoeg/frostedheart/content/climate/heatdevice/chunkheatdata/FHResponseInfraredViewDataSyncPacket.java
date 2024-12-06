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

package com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata;

import com.lowdragmc.lowdraglib.LDLib;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;


public class FHResponseInfraredViewDataSyncPacket implements FHMessage {
    private final ChunkPos chunkPos;
    private final int[] data;

    public FHResponseInfraredViewDataSyncPacket(ChunkPos chunkPos, List<IHeatArea> heatAreas) {
        this.chunkPos = chunkPos;
        data = new int[heatAreas.size() * 8];
        var index = 0;
        for (var heatArea: heatAreas) {
            for (float value : heatArea.getStructData()) {
                // we convert float to int for better serialization
                int intBits = Float.floatToIntBits(value);
                data[index++] = intBits;
            }
        }
    }

    public FHResponseInfraredViewDataSyncPacket(FriendlyByteBuf buffer) {
        this.chunkPos = new ChunkPos(buffer.readVarLong());
        this.data = buffer.readVarIntArray();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarLong(chunkPos.toLong());
        buffer.writeVarIntArray(data);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (LDLib.isClient()) {
                if (!RenderSystem.isOnRenderThread()) {
                    RenderSystem.recordRenderCall(() -> InfraredViewRenderer.updateData(chunkPos, data));
                } else {
                    InfraredViewRenderer.updateData(chunkPos, data);
                }

            }
        });
        context.get().setPacketHandled(true);
    }
}
