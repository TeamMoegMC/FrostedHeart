package com.teammoeg.frostedheart.scenario.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.scenario.client.ClientTextProcessor;
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

	public ServerSenarioTextPacket(PacketBuffer buffer) {
		text=buffer.readString(1024*300);
		isReline = buffer.readBoolean();
	}

	public ServerSenarioTextPacket(String text, boolean isReline, Style st) {
		super();
		this.text = text;
		this.isReline = isReline;
	}

	public void encode(PacketBuffer buffer) {
		buffer.writeString(text,1024*300);
		buffer.writeBoolean(isReline);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ClientTextProcessor.setReline(isReline);
			ClientTextProcessor.process(text);
		});
		context.get().setPacketHandled(true);
	}
}
