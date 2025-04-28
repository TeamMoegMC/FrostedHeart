package com.teammoeg.frostedheart.clusterserver.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class S2CTokenPacket implements CMessage{
	String token;

	public S2CTokenPacket(String token) {
		super();
		this.token = token;
	}
	public S2CTokenPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf());
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(token);
	}

	@Override
	public void handle(Supplier<Context> context) {
		
		ClientConnectionHelper.token=token;
		
	}

}
