package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;

import net.minecraft.network.PacketBuffer;

public class DataEntry {
	FHDataTypes type;
	String data;
	public DataEntry(FHDataTypes type, JsonObject data) {
		this.type = type;
		this.data = data.toString();
	}
	public DataEntry(PacketBuffer buffer) {
		this.type = FHDataTypes.values()[buffer.readVarInt()];
		this.data = buffer.readString();
	}
	public void encode(PacketBuffer buffer) {
		buffer.writeVarInt(type.ordinal());
		buffer.writeString(data);
	}
}
