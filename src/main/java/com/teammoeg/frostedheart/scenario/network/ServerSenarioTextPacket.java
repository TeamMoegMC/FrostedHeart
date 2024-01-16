package com.teammoeg.frostedheart.scenario.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.client.FHScenarioClient;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.network.NetworkEvent;

public class ServerSenarioTextPacket {
    private final String text;
    private final boolean isReline;
    private final boolean isNowait;
    private final boolean resetScene;

    public ServerSenarioTextPacket(PacketBuffer buffer) {
        text = buffer.readString(1024 * 300);
        isReline = buffer.readBoolean();
        isNowait = buffer.readBoolean();
        resetScene=buffer.readBoolean();
    }



    public ServerSenarioTextPacket(String text, boolean isReline, boolean isNowait, boolean resetScene) {
		super();
		this.text = text;
		this.isReline = isReline;
		this.isNowait = isNowait;
		this.resetScene = resetScene;
	}



	public void encode(PacketBuffer buffer) {
        buffer.writeString(text, 1024 * 300);
        buffer.writeBoolean(isReline);
        buffer.writeBoolean(isNowait);
        buffer.writeBoolean(resetScene);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {

            ClientScene.process(text, isReline, isNowait,resetScene);
        });
        context.get().setPacketHandled(true);
    }
}
