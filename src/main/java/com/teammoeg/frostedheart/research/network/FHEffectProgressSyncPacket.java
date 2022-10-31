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

package com.teammoeg.frostedheart.research.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.effects.Effect;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHEffectProgressSyncPacket {
    private final boolean data;
    private final int id;

    public FHEffectProgressSyncPacket(UUID team, Effect rs) {
        TeamResearchData rd = FHResearchDataManager.INSTANCE.getData(team);
        this.data = rd.isEffectGranted(rs);
        this.id = rs.getRId();
    }

    public FHEffectProgressSyncPacket(PacketBuffer buffer) {
        data = buffer.readBoolean();
        id = buffer.readVarInt();
    }


    public void encode(PacketBuffer buffer) {
        buffer.writeBoolean(data);
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Effect e = FHResearch.effects.getById(id);
            if (data)
                e.grant(TeamResearchData.getClientInstance(), null, false);
            else
                e.revoke(TeamResearchData.getClientInstance());
            e.setGranted(data);
        });
        context.get().setPacketHandled(true);
    }
}
