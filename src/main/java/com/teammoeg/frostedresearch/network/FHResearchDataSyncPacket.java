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

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.compat.ResearchJeiBridge;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.gui.InsightOverlay;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// send when player join
public class FHResearchDataSyncPacket implements CMessage {
    CompoundTag dat;


    public FHResearchDataSyncPacket(FriendlyByteBuf buffer) {
        this.dat = ResearchNetworkCodec.readPayload(buffer, "full team state");
    }

    public FHResearchDataSyncPacket(TeamResearchData team) {
        try {
            this.dat = ResearchNetworkCodec.encode(TeamResearchData.NETWORK_CODEC, team);
            //System.out.println(dat);
        } catch (Exception e) {
            FRMain.LOGGER.error("Failed to save research data when syncing research data", e);
        }
    }


    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            try {
                // Sync Server Data to Client
                TeamResearchData decoded = ResearchNetworkCodec.decode(
                        TeamResearchData.NETWORK_CODEC, dat, "full team state");
                if (decoded == null) return;
                CClientTeamDataManager.INSTANCE.getInstance().setData(FRSpecialDataTypes.RESEARCH_DATA, decoded);
                // Grant Effects on Client
                

                 TeamDataClosure<TeamResearchData> closure = CClientTeamDataManager.INSTANCE.getInstance().getDataHolder(FRSpecialDataTypes.RESEARCH_DATA);
                 closure.get().initResearch(closure.team());
                 DistExecutor.safeRunWhenOn(Dist.CLIENT, ()->InsightOverlay::initOverlay);
                 DistExecutor.safeRunWhenOn(Dist.CLIENT, ()->ResearchUtils::notifyResearchDataReplaced);
            } catch (Exception e) {
                FRMain.LOGGER.error("Failed to load data when syncing research data", e);
            }
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ResearchJeiBridge::sync);
        });
        context.get().setPacketHandled(true);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(dat);
    }
}
