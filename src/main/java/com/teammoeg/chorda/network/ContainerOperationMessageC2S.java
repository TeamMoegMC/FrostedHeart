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

package com.teammoeg.chorda.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.menu.CBaseMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record ContainerOperationMessageC2S(int containerId, short buttonId, int state) implements CMessage {

	public ContainerOperationMessageC2S(FriendlyByteBuf buf) {
		this(buf.readVarInt(),buf.readShort(),buf.readVarInt());
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(containerId);
		buffer.writeShort(buttonId);
		buffer.writeVarInt(state);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			Context ctx=context.get();
			ServerPlayer player=ctx.getSender();
			//System.out.print("received operation packet "+this);
			//System.out.print("player container "+player.containerMenu.containerId+ ":"+player.containerMenu);
			if(player.containerMenu.containerId==containerId&&player.containerMenu instanceof CBaseMenu container) {
				//System.out.println("calling message received");
				container.receiveMessage(buttonId, state);
			}
		});
	}

}
