package com.teammoeg.frostedheart.trade;

import java.util.Map;

import com.google.gson.JsonObject;

import net.minecraft.network.PacketBuffer;

public class NopData extends BaseData {

	public NopData(JsonObject jo) {
		super(jo);
	}

	public NopData(PacketBuffer pb) {
		super(pb);
	}

	public NopData(String id, int maxstore, float recover, int price, PolicyAction... restock) {
		super(id, maxstore, recover, price, restock);
	}

	@Override
	public String getType() {
		return "n";
	}
	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeVarInt(3);
		super.write(buffer);
	}
	@Override
	public void fetch(PolicySnapshot shot, Map<String, Float> data) {
	}

}
