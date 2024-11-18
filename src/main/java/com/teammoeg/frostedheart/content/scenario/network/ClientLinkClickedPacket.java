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

package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.scenario.FHScenario;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ClientLinkClickedPacket implements FHMessage {
    final String link;
    public ClientLinkClickedPacket(FriendlyByteBuf buffer) {
    	link=buffer.readUtf();
    }



	public ClientLinkClickedPacket(String link) {
		super();
		this.link = link;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(link);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	FHScenario.get(context.get().getSender()).onLinkClicked(link);
        });
        context.get().setPacketHandled(true);
    }
}
