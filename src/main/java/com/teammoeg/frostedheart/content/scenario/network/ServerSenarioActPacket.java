package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ServerSenarioActPacket implements CMessage {
    private final String title;
    private final String subtitle;
    public ServerSenarioActPacket(FriendlyByteBuf buffer) {
    	title =  SerializeUtil.readOptional(buffer, FriendlyByteBuf::readUtf).orElse(null);
    	subtitle = SerializeUtil.readOptional(buffer, FriendlyByteBuf::readUtf).orElse(null);
    }
	public ServerSenarioActPacket(String title, String subtitle) {
		super();
		this.title = title;
		this.subtitle = subtitle;
	}
	public void encode(FriendlyByteBuf buffer) {
        SerializeUtil.writeOptional2(buffer, title, FriendlyByteBuf::writeUtf);
        SerializeUtil.writeOptional2(buffer, subtitle, FriendlyByteBuf::writeUtf);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null)
        		ClientScene.INSTANCE.setActHud(title,subtitle);
        });
        context.get().setPacketHandled(true);
    }
}
