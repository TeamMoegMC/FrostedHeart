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
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when data update
public class FHChangeActiveResearchPacket implements CMessage {
    private final String id;

    public FHChangeActiveResearchPacket() {
        this.id = "";
    }

    public FHChangeActiveResearchPacket(String rid) {
        this.id = rid == null ? "" : rid;
    }

    public FHChangeActiveResearchPacket(FriendlyByteBuf buffer) {
        id = ResearchNetworkCodec.readOptionalId(buffer, "active research");
    }

    public FHChangeActiveResearchPacket(Research rs) {
        this.id = rs.getId();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id, ResearchNetworkCodec.MAX_ID_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!id.isBlank() && FHResearch.getResearch(id) == null) {
                ResearchNetworkCodec.reject("active research: unknown target " + id);
                return;
            }
            ClientResearchDataAPI.getData().get().setCurrentResearch(id.isBlank() ? null : id);
            Research current = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
            ResearchUtils.notifyActiveResearchChanged(current == null ? null : current.getId());
        });
        context.get().setPacketHandled(true);
    }
}
