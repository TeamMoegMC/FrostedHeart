package com.teammoeg.frostedheart.base.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHBaseContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record FHContainerOperation(int containerId, short buttonId, int state) implements FHMessage {

	public FHContainerOperation(FriendlyByteBuf buf) {
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
			if(player.containerMenu.containerId==containerId&&player.containerMenu instanceof FHBaseContainer container) {
				container.reciveMessage(buttonId, state);
			}
		});
	}

}
