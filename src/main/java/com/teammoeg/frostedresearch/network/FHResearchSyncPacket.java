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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when player join
public record FHResearchSyncPacket(CompoundTag data, String key) implements CMessage {
    public FHResearchSyncPacket(Research r) {
        this(ResearchNetworkCodec.encode(Research.CODEC, r), r.getId());

    }

    public FHResearchSyncPacket(FriendlyByteBuf buffer) {
        this(ResearchNetworkCodec.readPayload(buffer, "research definition"),
                ResearchNetworkCodec.readId(buffer, "research definition id"));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(data);
        // LogUtils.getLogger().debug("Encoded research "+key+":"+data);
        buffer.writeUtf(key, ResearchNetworkCodec.MAX_ID_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {
            // LogUtils.getLogger().debug("Decoded research "+key+":"+data);
            Research decoded = ResearchNetworkCodec.decode(Research.CODEC, data, "research definition " + key);
            if (decoded != null && key != null) FHResearch.readOne(key, decoded);
        });
        context.get().setPacketHandled(true);
    }
}
