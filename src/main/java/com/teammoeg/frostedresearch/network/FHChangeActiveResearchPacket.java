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

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.ResearchUtils;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

// send when data update
public class FHChangeActiveResearchPacket implements CMessage {
    private final int id;

    public FHChangeActiveResearchPacket() {
        this.id = -1;
    }

    public FHChangeActiveResearchPacket(int rid) {
        this.id = rid;
    }

    public FHChangeActiveResearchPacket(FriendlyByteBuf buffer) {
        id = buffer.readVarInt();
    }

    public FHChangeActiveResearchPacket(Research rs) {
        this.id = FHResearch.researches.getIntId(rs);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientResearchDataAPI.getData().get().setCurrentResearch(id);
            ResearchUtils.refreshResearchGui();
        });
        context.get().setPacketHandled(true);
    }
}
