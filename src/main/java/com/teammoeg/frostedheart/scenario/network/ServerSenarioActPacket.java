package com.teammoeg.frostedheart.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.runner.RunStatus;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ServerSenarioActPacket {
    private final String title;
    private final String subtitle;
    public ServerSenarioActPacket(PacketBuffer buffer) {
    	title = buffer.readString();
    	subtitle = buffer.readString();
    }
	public ServerSenarioActPacket(String title, String subtitle) {
		super();
		this.title = title;
		this.subtitle = subtitle;
	}
	public void encode(PacketBuffer buffer) {
        buffer.writeString(title);
        buffer.writeString(subtitle);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null)
        		ClientScene.INSTANCE.setActHud(title,subtitle);
        });
        context.get().setPacketHandled(true);
    }
}
