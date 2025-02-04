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

import com.lowdragmc.lowdraglib.LDLib;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public class FHNotifyChunkHeatUpdatePacket implements CMessage {
    private final ChunkPos chunkPos;

    public FHNotifyChunkHeatUpdatePacket(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
    }

    public FHNotifyChunkHeatUpdatePacket(FriendlyByteBuf buffer) {
        this.chunkPos = new ChunkPos(buffer.readVarLong());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarLong(chunkPos.toLong());
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	FHMain.LOGGER.info("Notifing player for chunk heat update...");
            if (LDLib.isClient()) {
                InfraredViewRenderer.notifyChunkDataUpdate();
            }
        });
        context.get().setPacketHandled(true);
    }
}
