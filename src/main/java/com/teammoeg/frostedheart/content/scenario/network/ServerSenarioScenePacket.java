package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.FHMessage;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ServerSenarioScenePacket implements FHMessage {
    private final String text;
    private final boolean isReline;
    private final boolean isNowait;
    private final boolean resetScene;
    private final RunStatus status;
    private final boolean isWaitClick;
    public ServerSenarioScenePacket(FriendlyByteBuf buffer) {
        text = buffer.readUtf(1024 * 300);
        isReline = buffer.readBoolean();
        isNowait = buffer.readBoolean();
        resetScene=buffer.readBoolean();
        status=RunStatus.values()[buffer.readByte()];
        isWaitClick=buffer.readBoolean();
    }



    public ServerSenarioScenePacket(String text, boolean isReline, boolean isNowait, boolean resetScene,RunStatus status,boolean isWC) {
		super();
		this.text = text;
		this.isReline = isReline;
		this.isNowait = isNowait;
		this.resetScene = resetScene;
		this.status=status;
		isWaitClick=isWC;
	}



	public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(text, 1024 * 300);
        buffer.writeBoolean(isReline);
        buffer.writeBoolean(isNowait);
        buffer.writeBoolean(resetScene);
        buffer.writeByte(status.ordinal());
        buffer.writeBoolean(isWaitClick);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
        	if(ClientScene.INSTANCE!=null) {
        		//System.out.println("scene sent");
        		ClientScene.INSTANCE.process(text, isReline, isNowait,resetScene,status);
        		ClientScene.INSTANCE.sendImmediately=!isWaitClick;
        	}
        });
        
        context.get().setPacketHandled(true);
    }
}
