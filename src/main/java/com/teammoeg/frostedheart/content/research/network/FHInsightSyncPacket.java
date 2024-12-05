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

package com.teammoeg.frostedheart.content.research.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.base.team.FHClientTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.io.codec.DataOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FHInsightSyncPacket implements FHMessage {
    int insight;
    int insightLevel;
    int usedInsightLevel;

    public FHInsightSyncPacket(FriendlyByteBuf buffer) {
        insight = buffer.readInt();
        insightLevel = buffer.readInt();
        usedInsightLevel = buffer.readInt();
    }

    public FHInsightSyncPacket(TeamDataHolder team) {
        TeamResearchData data = team.getData(SpecialDataTypes.RESEARCH_DATA);
        this.insight = data.getInsight();
        this.insightLevel = data.getInsightLevel();
        this.usedInsightLevel = data.getUsedInsightLevel();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(insight);
        buffer.writeInt(insightLevel);
        buffer.writeInt(usedInsightLevel);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        TeamResearchData clientData = ClientResearchDataAPI.getData().get();
        clientData.setInsightOnly(this.insight);
        clientData.setInsightLevelOnly(this.insightLevel);
        clientData.setUsedInsightLevel(this.usedInsightLevel);
        context.get().setPacketHandled(true);
    }
}
