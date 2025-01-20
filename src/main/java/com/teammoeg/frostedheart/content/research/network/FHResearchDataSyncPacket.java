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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.team.CClientTeamDataManager;
import com.teammoeg.chorda.team.TeamDataClosure;
import com.teammoeg.chorda.util.io.codec.DataOps;
import com.teammoeg.chorda.util.io.codec.ObjectWriter;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// send when player join
public class FHResearchDataSyncPacket implements CMessage {
    Object dat;


    public FHResearchDataSyncPacket(FriendlyByteBuf buffer) {
        this.dat = (ObjectWriter.readObject(buffer));
    }

    public FHResearchDataSyncPacket(TeamResearchData team) {
        try {
            this.dat = (FHSpecialDataTypes.RESEARCH_DATA.saveData(DataOps.COMPRESSED, team));
        } catch (Exception e) {
            FHMain.LOGGER.error("Failed to save research data when syncing research data", e);
        }
    }


    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            try {
                // Sync Server Data to Client
                CClientTeamDataManager.INSTANCE.getInstance().setData(FHSpecialDataTypes.RESEARCH_DATA, FHSpecialDataTypes.RESEARCH_DATA.loadData(DataOps.COMPRESSED, dat));
                // Grant Effects on Client
                
                // TODO: Why not just sync the UnlockLists through TeamResearchData.Codec, so that the above line is all we need?
                 TeamDataClosure<TeamResearchData> closure = CClientTeamDataManager.INSTANCE.getInstance().getDataHolder(FHSpecialDataTypes.RESEARCH_DATA);
                 closure.get().initResearch(closure.team());
            } catch (Exception e) {
                FHMain.LOGGER.error("Failed to load data when syncing research data", e);
            }
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::syncJEI);
        });
        context.get().setPacketHandled(true);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        ObjectWriter.writeObject(buffer, dat);
    }
}
