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

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when clue progress updated
public record FHS2CClueProgressSyncPacket(boolean data, String researchId, String clueId) implements CMessage {

    public FHS2CClueProgressSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.isReadable() && buffer.readBoolean(),
                ResearchNetworkCodec.readId(buffer, "clue progress research"),
                ResearchNetworkCodec.readId(buffer, "clue progress clue"));

    }

    public FHS2CClueProgressSyncPacket(TeamDataHolder team, Research rch, Clue clue) {
        this(team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData(rch).isClueTriggered(clue),
                rch.getId(), clue.getNonce());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(data);
        buffer.writeUtf(researchId, ResearchNetworkCodec.MAX_ID_LENGTH);
        buffer.writeUtf(clueId, ResearchNetworkCodec.MAX_ID_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research rch = FHResearch.getResearch(researchId);
            if (rch != null) {
                Clue cl = rch.getClues().stream()
                        .filter(candidate -> candidate.getNonce().equals(clueId))
                        .findFirst().orElse(null);
                if (cl == null) {
                    ResearchNetworkCodec.reject("clue progress: unknown clue " + researchId + "/" + clueId);
                    return;
                }
                ClientResearchDataAPI.getData().get().getData(rch).setClueTriggered(cl, data);
                ResearchUtils.notifyClueProgressChanged(rch.getId(), clueId);
            } else ResearchNetworkCodec.reject("clue progress: unknown research " + researchId);

        });
        context.get().setPacketHandled(true);
    }
}
