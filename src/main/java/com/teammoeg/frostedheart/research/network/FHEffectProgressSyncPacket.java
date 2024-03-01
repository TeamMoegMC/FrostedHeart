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
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.research.effects.Effect;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.team.TeamDataHolder;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when player join
public class FHEffectProgressSyncPacket implements FHMessage {
    private final boolean data;
    private final int id;

    public FHEffectProgressSyncPacket(PacketBuffer buffer) {
        data = buffer.readBoolean();
        id = buffer.readVarInt();
    }

    public FHEffectProgressSyncPacket(TeamDataHolder team, Effect rs) {
        TeamResearchData rd = team.getData(SpecialDataTypes.RESEARCH_DATA);
        this.data = rd.isEffectGranted(rs);
        this.id = rs.getRId();
    }


    public void encode(PacketBuffer buffer) {
        buffer.writeBoolean(data);
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Effect e = FHResearch.effects.getById(id);
            if (data)
                e.grant(ClientResearchDataAPI.getData(), null, false);
            else
                e.revoke(ClientResearchDataAPI.getData());
            e.setGranted(data);
        });
        context.get().setPacketHandled(true);
    }
}
