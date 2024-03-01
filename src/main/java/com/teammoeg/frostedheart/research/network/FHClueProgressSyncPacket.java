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

package com.teammoeg.frostedheart.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.research.clues.Clue;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.team.TeamDataHolder;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHClueProgressSyncPacket implements FHMessage {
    private final boolean data;
    private final int id;

    public FHClueProgressSyncPacket(PacketBuffer buffer) {
        data = buffer.readBoolean();
        id = buffer.readVarInt();
    }

    public FHClueProgressSyncPacket(TeamDataHolder team, Clue rs) {
        TeamResearchData rd = team.getData(SpecialDataTypes.RESEARCH_DATA);
        this.data = rd.isClueTriggered(rs);
        this.id = rs.getRId();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeBoolean(data);
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            FHResearch.clues.getById(id).setCompleted(data);
        });
        context.get().setPacketHandled(true);
    }
}
