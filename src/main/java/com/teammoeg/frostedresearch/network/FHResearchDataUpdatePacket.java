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
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.events.ClientResearchStatusEvent;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

// send when data update
public record FHResearchDataUpdatePacket(CompoundTag rd, String researchId) implements CMessage {


    public FHResearchDataUpdatePacket(FriendlyByteBuf buffer) {
        this(ResearchNetworkCodec.readPayload(buffer, "research update"),
                ResearchNetworkCodec.readId(buffer, "research update target"));
    }

    public FHResearchDataUpdatePacket(Research rs, ResearchData rd) {
        this(ResearchNetworkCodec.encode(ResearchData.NETWORK_CODEC, rd), rs.getId());
                                                 
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(rd);
        buffer.writeUtf(researchId, ResearchNetworkCodec.MAX_ID_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            try {
                Research rs = FHResearch.getResearch(researchId);
                if (rs == null) {
                    ResearchNetworkCodec.reject("research update: unknown target " + researchId);
                    return;
                }
                ResearchData datax = ResearchNetworkCodec.decode(
                        ResearchData.NETWORK_CODEC, rd, "research update " + researchId);
                if (datax == null) return;
                ResearchData old = rs.getData();
                boolean status = old.isCompleted();
                old.copyFrom(datax);
                ResearchUtils.notifyResearchProgressChanged(rs.getId());
                MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs, old.isCompleted(), status != old.isCompleted()));
            } catch (RuntimeException e) {
                FRMain.LOGGER.warn("Discarded malformed research update for {}", researchId, e);
            }


        });
        context.get().setPacketHandled(true);
    }
}
