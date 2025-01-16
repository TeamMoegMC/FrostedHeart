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

package com.teammoeg.frostedheart.content.research.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.team.TeamDataClosure;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when player join
public record FHEffectProgressSyncPacket(boolean data,int id,int index) implements CMessage {
    
    public FHEffectProgressSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(),buffer.readVarInt(),buffer.readVarInt());
    }

    public FHEffectProgressSyncPacket(TeamDataHolder team,Research rs,Effect eff) {
    	this(team.getData(FHSpecialDataTypes.RESEARCH_DATA).isEffectGranted(rs, eff),FHResearch.researches.getIntId(rs),rs.getEffects().indexOf(eff));
    }


    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(data);
        buffer.writeVarInt(id);
        buffer.writeVarInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research r = FHResearch.getResearch(id);
            Effect e=r.getEffects().get(index);
            TeamDataClosure<TeamResearchData> trd=ClientResearchDataAPI.getData();
            if (data)
                e.grant(null,trd.get(), null, false);
            else
                e.revoke(trd.get());
            trd.get().getData(r).setEffectGranted(e, data);
        });
        context.get().setPacketHandled(true);
    }
}
