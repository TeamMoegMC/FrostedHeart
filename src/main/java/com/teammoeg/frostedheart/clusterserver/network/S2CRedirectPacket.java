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
