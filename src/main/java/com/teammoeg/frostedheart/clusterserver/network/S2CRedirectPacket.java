package com.teammoeg.frostedheart.clusterserver.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent.Context;

public class S2CRedirectPacket implements CMessage{
	String ip;
	boolean temporarily;

	public S2CRedirectPacket(String ip, boolean temporarily) {
		super();
		this.ip = ip;
		this.temporarily = temporarily;
	}
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
		
		//ClientConnectionHelper.handleDisconnect();
		context.get().getNetworkManager().disconnect(Component.translatable("multiplayer.status.quitting"));
		//context.get().getNetworkManager().
		ClientConnectionHelper.joinNewServer(ip,temporarily);
		
	}

}
