package com.teammoeg.frostedheart.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.network.FHMessage;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ServerSenarioActPacket implements FHMessage {
    private final String title;
    private final String subtitle;
    public ServerSenarioActPacket(PacketBuffer buffer) {
    	title =  SerializeUtil.readOptional(buffer, PacketBuffer::readString).orElse(null);
    	subtitle = SerializeUtil.readOptional(buffer, PacketBuffer::readString).orElse(null);
    }
	public ServerSenarioActPacket(String title, String subtitle) {
		super();
		this.title = title;
		this.subtitle = subtitle;
	}
	public void encode(PacketBuffer buffer) {
        SerializeUtil.writeOptional2(buffer, title, PacketBuffer::writeString);
        SerializeUtil.writeOptional2(buffer, subtitle, PacketBuffer::writeString);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null)
        		ClientScene.INSTANCE.setActHud(title,subtitle);
        });
        context.get().setPacketHandled(true);
    }
}
