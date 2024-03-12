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

import java.util.Optional;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.NullableNBTMessage;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.events.ClientResearchStatusEvent;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

// send when data update
public class FHResearchDataUpdatePacket extends NullableNBTMessage{
    private final int id;

    public FHResearchDataUpdatePacket(int rid) {
    	super(Optional.empty());
        this.id = rid;
    }

    public FHResearchDataUpdatePacket(PacketBuffer buffer) {
        super(buffer);
        id = buffer.readVarInt();
    }

    public FHResearchDataUpdatePacket(ResearchData rd) {
        super(Optional.of(rd.serialize()));
        this.id = rd.getResearch().getRId();
    }

    public void encode(PacketBuffer buffer) {
        super.encode(buffer);
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research rs = FHResearch.researches.getById(id);
            if (this.getTag() == null) {
                rs.resetData();
                MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs, false, true));
                return;
            }
            ResearchData datax = rs.getData();
            boolean status = datax.isCompleted();
            datax.deserialize(this.getTag());
            ClientUtils.refreshResearchGui();

            MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs, datax.isCompleted(), status != datax.isCompleted()));


        });
        context.get().setPacketHandled(true);
    }
}
