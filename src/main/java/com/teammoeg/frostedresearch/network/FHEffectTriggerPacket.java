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

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class FHEffectTriggerPacket implements CMessage {
    private final String researchID;

    public FHEffectTriggerPacket(FriendlyByteBuf buffer) {
        researchID = ResearchNetworkCodec.readId(buffer, "effect trigger target");

    }

    public FHEffectTriggerPacket(Research r) {
        this.researchID = r.getId();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(researchID, ResearchNetworkCodec.MAX_ID_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {
            Research r = FHResearch.getResearch(researchID);

            ServerPlayer spe = context.get().getSender();
            if (r == null || spe == null) {
                ResearchNetworkCodec.reject("effect trigger: invalid target or missing sender");
                return;
            }
            TeamDataHolder data = CTeamDataManager.get(spe);
            TeamResearchData trd = data.getData(FRSpecialDataTypes.RESEARCH_DATA);
            if (trd.getData(r).isCompleted()) {

                trd.grantEffects(data, spe, r);

            }
        });
        context.get().setPacketHandled(true);
    }
}
