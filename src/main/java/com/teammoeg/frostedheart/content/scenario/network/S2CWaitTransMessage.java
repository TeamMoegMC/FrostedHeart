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
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record S2CWaitTransMessage(int thread,boolean isWaitTrans) implements CMessage{
	public S2CWaitTransMessage(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(),buffer.readBoolean());
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(thread);
		buffer.writeBoolean(isWaitTrans);
		
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			if(isWaitTrans) {
				ClientScene.INSTANCE.onTransitionComplete.addListener(()->()->FHNetwork.INSTANCE.sendToServer(new C2SRenderingStatusMessage(thread,true)));
			}else {
				ClientScene.INSTANCE.onRenderComplete.addListener(()->()->FHNetwork.INSTANCE.sendToServer(new C2SRenderingStatusMessage(thread,false)));
			}
		});
		context.get().setPacketHandled(true);
	}

}
