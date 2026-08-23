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

import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.effects.Effect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when player join
public record FHEffectProgressSyncPacket(boolean data, String researchId, String effectId) implements CMessage {

    public FHEffectProgressSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.isReadable() && buffer.readBoolean(),
                ResearchNetworkCodec.readId(buffer, "effect progress research"),
                ResearchNetworkCodec.readId(buffer, "effect progress effect"));
    }

    public FHEffectProgressSyncPacket(TeamDataHolder team, Research rs, Effect eff) {
        this(team.getData(FRSpecialDataTypes.RESEARCH_DATA).isEffectGranted(rs, eff), rs.getId(), eff.getNonce());
    }


    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(data);
        buffer.writeUtf(researchId, ResearchNetworkCodec.MAX_ID_LENGTH);
        buffer.writeUtf(effectId, ResearchNetworkCodec.MAX_ID_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research r = FHResearch.getResearch(researchId);
            if (r == null) {
                ResearchNetworkCodec.reject("effect progress: unknown research " + researchId);
                return;
            }
            Effect e = r.getEffects().stream()
                    .filter(candidate -> candidate.getNonce().equals(effectId))
                    .findFirst().orElse(null);
            if (e == null) {
                ResearchNetworkCodec.reject("effect progress: unknown effect " + researchId + "/" + effectId);
                return;
            }
            TeamDataClosure<TeamResearchData> trd = ClientResearchDataAPI.getData();
            if (data)
                e.grant(null, trd.get(), null, false);
            else
                e.revoke(trd.get());
            
            trd.get().getData(r).setEffectGranted(e, data);
            if (!data)
                trd.get().restoreGrantedUnlocks(null);
            ResearchUtils.notifyResearchProgressChanged(r.getId());
        });
        context.get().setPacketHandled(true);
    }
}
