package com.teammoeg.chorda.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.menu.CContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record CContainerOperation(int containerId, short buttonId, int state) implements CMessage {

	public CContainerOperation(FriendlyByteBuf buf) {
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
			if(player.containerMenu.containerId==containerId&&player.containerMenu instanceof CContainer container) {
				//System.out.println("calling message received");
				container.receiveMessage(buttonId, state);
			}
		});
	}

}
