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

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

// send when data update
public class FHChangeActiveResearchPacket implements FHMessage {
    private final int id;

    public FHChangeActiveResearchPacket() {
        this.id = 0;
    }

    public FHChangeActiveResearchPacket(int rid) {
        this.id = rid;
    }

    public FHChangeActiveResearchPacket(PacketBuffer buffer) {
        id = buffer.readVarInt();
    }

    public FHChangeActiveResearchPacket(Research rs) {
        this.id = rs.getRId();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientResearchDataAPI.getData().setCurrentResearch(id);
            ClientUtils.refreshResearchGui();
        });
        context.get().setPacketHandled(true);
    }
}
