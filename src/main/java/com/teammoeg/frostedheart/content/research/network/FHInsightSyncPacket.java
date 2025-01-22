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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FHInsightSyncPacket(int insight,int insightLevel,int usedInsightLevel) implements CMessage {


    public FHInsightSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt());

    }


    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(insight);
        buffer.writeVarInt(insightLevel);
        buffer.writeVarInt(usedInsightLevel);
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
