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

package com.teammoeg.frostedheart.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.events.ClientResearchStatusEvent;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;
// send when player join
public class FHResearchProgressSyncPacket {
    private final CompoundNBT data;
    private final int id;
    public FHResearchProgressSyncPacket(UUID team,Research rs) {
    	TeamResearchData rd=ResearchDataManager.INSTANCE.getData(team);
        this.data = rd.getData(rs).serialize();
        this.id=rs.getRId();
    }

    FHResearchProgressSyncPacket(PacketBuffer buffer) {
        data = buffer.readCompoundTag();
        id=buffer.readVarInt();
    }

    void encode(PacketBuffer buffer) {
        buffer.writeCompoundTag(data);
        buffer.writeVarInt(id);
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	Research rs=FHResearch.researches.getById(id);
        	ResearchData datax=rs.getData();
        	boolean status=datax.isCompleted();
        	datax.deserialize(data);
        	if(status!=datax.isCompleted())
        		MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs,datax.isCompleted()));
        });
        context.get().setPacketHandled(true);
    }
}
