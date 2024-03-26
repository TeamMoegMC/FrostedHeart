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

package com.teammoeg.frostedheart.content.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHResearchSyncPacket implements FHMessage {
    final Research r;
    public FHResearchSyncPacket(Research r) {
    	this.r=r;
    }

    public FHResearchSyncPacket(PacketBuffer buffer) {
        r = CodecUtil.readCodec(buffer, Research.CODEC);
        r.setId(buffer.readString());
    }

    public void encode(PacketBuffer buffer) {
        CodecUtil.writeCodec(buffer, Research.CODEC, r);
        buffer.writeString(r.getId());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> FHResearch.readOne(r));
        context.get().setPacketHandled(true);
    }
}
