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

package com.teammoeg.frostedheart.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.research.events.ClientResearchStatusEvent;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

// send when data update
public class FHResearchDataUpdatePacket {
    private final CompoundNBT data;
    private final int id;

    public FHResearchDataUpdatePacket(ResearchData rd) {
        this.data = rd.serialize();
        this.id = rd.getResearch().getRId();
    }

    public FHResearchDataUpdatePacket(int rid) {
        this.data = null;
        this.id = rid;
    }

    public FHResearchDataUpdatePacket(PacketBuffer buffer) {
        data = SerializeUtil.readOptional(buffer, PacketBuffer::readCompoundTag).orElse(null);
        id = buffer.readVarInt();
    }

    public void encode(PacketBuffer buffer) {
        SerializeUtil.writeOptional2(buffer, data, PacketBuffer::writeCompoundTag);
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research rs = FHResearch.researches.getById(id);
            if (data == null) {
                rs.resetData();
                MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs, false, true));
                return;
            }
            ResearchData datax = rs.getData();
            boolean status = datax.isCompleted();
            datax.deserialize(data);
            ClientUtils.refreshResearchGui();

            MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs, datax.isCompleted(), status != datax.isCompleted()));


        });
        context.get().setPacketHandled(true);
    }
}
