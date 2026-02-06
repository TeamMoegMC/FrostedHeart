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

package com.teammoeg.frostedresearch.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when player join
public record FHResearchSyncPacket(Object data, String key) implements CMessage {
    public FHResearchSyncPacket(Research r) {
        this(CodecUtil.encodeOrThrow(Research.CODEC.encodeStart(DataOps.COMPRESSED, r)), r.getId());

    }

    public FHResearchSyncPacket(FriendlyByteBuf buffer) {
        this(ObjectWriter.readObject(buffer), buffer.readUtf());
    }

    public void encode(FriendlyByteBuf buffer) {
        ObjectWriter.writeObject(buffer, data);
        // LogUtils.getLogger().debug("Encoded research "+key+":"+data);
        buffer.writeUtf(key);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {
            // LogUtils.getLogger().debug("Decoded research "+key+":"+data);
            FHResearch.readOne(key, CodecUtil.decodeOrThrow(Research.CODEC.decode(DataOps.COMPRESSED, data)));
        });
        context.get().setPacketHandled(true);
    }
}
