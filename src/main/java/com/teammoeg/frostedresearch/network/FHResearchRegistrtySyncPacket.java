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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when player join
public class FHResearchRegistrtySyncPacket implements CMessage {
    private final CompoundTag data;

    public FHResearchRegistrtySyncPacket() {
        data = FHResearch.save(new CompoundTag());

    }

    public FHResearchRegistrtySyncPacket(FriendlyByteBuf buffer) {
        data = ResearchNetworkCodec.readPayload(buffer, "research registry");
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (data == null) {
                ResearchNetworkCodec.reject("research registry: missing payload");
                return;
            }
            FHResearch.initFromRegistry(data);
        });
        context.get().setPacketHandled(true);
    }
}
