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

package com.teammoeg.frostedheart.clusterserver.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent.Context;

public record S2CRedirectPacket(String ip, boolean temporarily) implements CMessage{
	public S2CRedirectPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf(),buffer.readBoolean());
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(ip);
		buffer.writeBoolean(temporarily);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().getNetworkManager().disconnect(Component.translatable("multiplayer.status.quitting"));
		context.get().enqueueWork(()->{
			//ClientConnectionHelper.handleDisconnect();
			
			//context.get().getNetworkManager().
			ClientConnectionHelper.back2ServerScreen();
			
			ClientConnectionHelper.joinNewServer(ip,temporarily);
		});
		
		
	}

}
