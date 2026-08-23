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
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class FHResearchControlPacket implements CMessage {
    public final Operator status;
    private final String researchID;
    public FHResearchControlPacket(Operator status, Research research) {
        super();
        this.status = status;
        this.researchID = research.getId();
    }


    public FHResearchControlPacket(FriendlyByteBuf buffer) {
        researchID = ResearchNetworkCodec.readId(buffer, "research control target");
        int ordinal = buffer.isReadable() ? buffer.readUnsignedByte() : 255;
        status = ordinal < Operator.values().length ? Operator.values()[ordinal] : null;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(researchID, ResearchNetworkCodec.MAX_ID_LENGTH);
        buffer.writeByte(status == null ? 255 : status.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {
            Research r = FHResearch.getResearch(researchID);
            if (r == null || status == null) {
                ResearchNetworkCodec.reject("research control: invalid target or operation");
                return;
            }
            ServerPlayer spe = context.get().getSender();
            if (spe == null) return;
            TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(spe);
            switch (status) {
                case COMMIT_ITEM:

                    ResearchData rd = trd.get().getData(r);
                    if (rd.canResearch()) return;
                    if (trd.get().commitItem(spe, trd.team(), r)) {
                        trd.get().setCurrentResearch(trd.team(), r);
                    }
                    return;
                case START:
                    trd.get().setCurrentResearch(trd.team(), r);
                    return;
                case PAUSE:
                    trd.get().clearCurrentResearch(trd.team(), true);
            }
        });
        context.get().setPacketHandled(true);
    }

    public enum Operator {
        COMMIT_ITEM,
        START,
        PAUSE
    }
}
