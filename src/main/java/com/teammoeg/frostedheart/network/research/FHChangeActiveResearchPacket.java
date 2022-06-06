/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.network.research;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
// send when data update
public class FHChangeActiveResearchPacket {
    private final int id;
    public FHChangeActiveResearchPacket(Research rs) {
        this.id=rs.getRId();
    }
    public FHChangeActiveResearchPacket(int rid) {
        this.id=rid;
    }
    public FHChangeActiveResearchPacket() {
        this.id=0;
    }
    public FHChangeActiveResearchPacket(PacketBuffer buffer) {
        id=buffer.readVarInt();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	TeamResearchData.setActiveResearch(id);
        	ClientUtils.refreshResearchGui();
        });
        context.get().setPacketHandled(true);
    }
}
