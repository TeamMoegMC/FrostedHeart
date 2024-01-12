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

package com.teammoeg.frostedheart.scenario.network;

import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClientScenarioResponsePacket {
    boolean isSkipped;
    int status;
    public ClientScenarioResponsePacket(PacketBuffer buffer) {
    	isSkipped=buffer.readBoolean();
    	status=buffer.readVarInt();
    }


    public ClientScenarioResponsePacket(boolean isSkipped, int status) {
		super();
		this.isSkipped = isSkipped;
		this.status = status;
	}


	public void encode(PacketBuffer buffer) {
		buffer.writeBoolean(isSkipped);
		buffer.writeVarInt(status);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
        });
        context.get().setPacketHandled(true);
    }
}
