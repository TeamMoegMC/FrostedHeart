package com.teammoeg.frostedheart.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.runner.RunStatus;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ServerSceneStatusPacket {

    private final boolean resetScene;
    private final RunStatus status;

    public ServerSceneStatusPacket(PacketBuffer buffer) {
        status = RunStatus.values()[buffer.readByte()];
        resetScene=buffer.readBoolean();
    }



    public ServerSceneStatusPacket( RunStatus status, boolean resetScene) {
		super();
		this.status = status;
		this.resetScene = resetScene;
	}



	public void encode(PacketBuffer buffer) {
        buffer.writeByte(status.ordinal());
        buffer.writeBoolean(resetScene);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(resetScene) {
        		ClientScene.cls();
        	}
        });
        context.get().setPacketHandled(true);
    }
}
