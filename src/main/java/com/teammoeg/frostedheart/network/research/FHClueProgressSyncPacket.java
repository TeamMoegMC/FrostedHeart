/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.network.research;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

// send when player join
public class FHClueProgressSyncPacket {
    private final boolean data;
    private final int id;

    public FHClueProgressSyncPacket(UUID team, Clue rs) {
        TeamResearchData rd = FHResearchDataManager.INSTANCE.getData(team);
        this.data = rd.isClueTriggered(rs);
        this.id = rs.getRId();
    }

    public FHClueProgressSyncPacket(PacketBuffer buffer) {
        data = buffer.readBoolean();
        id = buffer.readVarInt();
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
