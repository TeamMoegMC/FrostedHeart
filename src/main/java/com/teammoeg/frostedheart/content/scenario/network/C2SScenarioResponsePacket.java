/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.FHScenario;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SScenarioResponsePacket(int runid,boolean isSkipped,int status) implements CMessage {
    

    public C2SScenarioResponsePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(),buffer.readBoolean(),buffer.readVarInt());
    }


    public void encode(FriendlyByteBuf buffer) {
    	buffer.writeVarInt(runid);
        buffer.writeBoolean(isSkipped);
        buffer.writeVarInt(status);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
        	//System.out.println("client responded");
            FHScenario.get(context.get().getSender()).notifyClientResponse(runid,isSkipped, status);
        });
        context.get().setPacketHandled(true);
    }
}
