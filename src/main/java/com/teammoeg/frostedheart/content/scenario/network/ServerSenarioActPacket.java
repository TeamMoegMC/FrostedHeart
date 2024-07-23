package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ServerSenarioActPacket implements FHMessage {
    private final String title;
    private final String subtitle;
    public ServerSenarioActPacket(PacketBuffer buffer) {
    	title =  SerializeUtil.readOptional(buffer, PacketBuffer::readUtf).orElse(null);
    	subtitle = SerializeUtil.readOptional(buffer, PacketBuffer::readUtf).orElse(null);
    }
	public ServerSenarioActPacket(String title, String subtitle) {
		super();
		this.title = title;
		this.subtitle = subtitle;
	}
	public void encode(PacketBuffer buffer) {
        SerializeUtil.writeOptional2(buffer, title, PacketBuffer::writeUtf);
        SerializeUtil.writeOptional2(buffer, subtitle, PacketBuffer::writeUtf);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null)
        		ClientScene.INSTANCE.setActHud(title,subtitle);
        });
        context.get().setPacketHandled(true);
    }
}
