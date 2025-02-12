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
				ClientScene.INSTANCE.onTransitionComplete.addListener(()->()->FHNetwork.sendToServer(new C2SRenderingStatusMessage(thread,true)));
			}else {
				ClientScene.INSTANCE.onRenderComplete.addListener(()->()->FHNetwork.sendToServer(new C2SRenderingStatusMessage(thread,false)));
			}
		});
		context.get().setPacketHandled(true);
	}

}
