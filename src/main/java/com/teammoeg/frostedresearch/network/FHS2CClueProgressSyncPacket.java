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

package com.teammoeg.frostedresearch.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when clue progress updated
public record FHS2CClueProgressSyncPacket(boolean data, int id, int index) implements CMessage {

    public FHS2CClueProgressSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());

    }

    public FHS2CClueProgressSyncPacket(TeamDataHolder team, Research rch, Clue clue) {
        this(team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData(rch).isClueTriggered(clue), FHResearch.researches.getIntId(rch), rch.getClues().indexOf(clue));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(data);
        buffer.writeVarInt(id);
        buffer.writeVarInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research rch = FHResearch.researches.get(id);
            if (rch != null) {
                Clue cl = rch.getClues().get(index);
                ClientResearchDataAPI.getData().get().getData(id()).setClueTriggered(cl, data);
            }

        });
        context.get().setPacketHandled(true);
    }
}
