/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.research.Research;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHResearchControlPacket implements FHMessage {
    public enum Operator {
        COMMIT_ITEM,
        START,
        PAUSE
    }

    public final Operator status;
    private final int researchID;


    public FHResearchControlPacket(Operator status, Research research) {
        super();
        this.status = status;
        this.researchID = research.getRId();
    }

    public FHResearchControlPacket(PacketBuffer buffer) {
        researchID = buffer.readVarInt();
        status = Operator.values()[buffer.readVarInt()];
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeVarInt(researchID);
        buffer.writeVarInt(status.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {
            Research r = FHResearch.researches.getById(researchID);
            ServerPlayerEntity spe = context.get().getSender();
            TeamResearchData trd = ResearchDataAPI.getData(spe);
            switch (status) {
                case COMMIT_ITEM:

                    ResearchData rd = trd.getData(r);
                    if (rd.canResearch()) return;
                    if (rd.commitItem(spe)) {
                        trd.setCurrentResearch(r);
                    }
                    return;
                case START:
                    trd.setCurrentResearch(r);
                    return;
                case PAUSE:
                    trd.clearCurrentResearch(r);
                    return;
            }
        });
        context.get().setPacketHandled(true);
    }
}
