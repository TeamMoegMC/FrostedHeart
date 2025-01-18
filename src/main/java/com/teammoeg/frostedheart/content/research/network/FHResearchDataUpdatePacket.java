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
import com.teammoeg.chorda.util.io.CodecUtil;
import com.teammoeg.chorda.util.io.codec.DataOps;
import com.teammoeg.chorda.util.io.codec.ObjectWriter;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.ResearchUtils;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.data.ResearchData.ResearchDataPacket;
import com.teammoeg.frostedheart.content.research.events.ClientResearchStatusEvent;
import com.teammoeg.frostedheart.content.research.research.Research;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// send when data update
public record FHResearchDataUpdatePacket(Object rd, int id) implements CMessage {


    public FHResearchDataUpdatePacket(FriendlyByteBuf buffer) {
        this(ObjectWriter.readObject(buffer), buffer.readVarInt());
    }

    public FHResearchDataUpdatePacket(Research rs, ResearchData rd) {
        this(CodecUtil.encodeOrThrow(ResearchData.NETWORK_CODEC.encodeStart(DataOps.COMPRESSED, rd.write(rs))), FHResearch.researches.getIntId(rs));
    }

    public void encode(FriendlyByteBuf buffer) {
        ObjectWriter.writeObject(buffer, rd);
        buffer.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Research rs = FHResearch.researches.getById(id);
            ResearchData old = rs.getData();
            ResearchDataPacket datax = CodecUtil.decodeOrThrow(ResearchData.NETWORK_CODEC.decode(DataOps.COMPRESSED, rd));
            boolean status = old.isCompleted();
            old.read(rs, datax);
            ResearchUtils.refreshResearchGui();
            MinecraftForge.EVENT_BUS.post(new ClientResearchStatusEvent(rs, old.isCompleted(), status != old.isCompleted()));


        });
        context.get().setPacketHandled(true);
    }
}
