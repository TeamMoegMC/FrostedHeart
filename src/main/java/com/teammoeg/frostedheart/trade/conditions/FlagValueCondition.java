package com.teammoeg.frostedheart.trade.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;

import net.minecraft.network.PacketBuffer;

public class FlagValueCondition extends WithFlagCondition {
	int value;
	public FlagValueCondition(JsonObject jo) {
		super(jo);
		value=jo.get("value").getAsInt();
	}

	public FlagValueCondition(PacketBuffer buffer) {
		super(buffer);
		value=buffer.readVarInt();
	}

	public FlagValueCondition(String name,int val) {
		super(name);
		value=val;
	}

	@Override
	public JsonElement serialize() {
		JsonObject jo=(JsonObject) super.serialize();
		jo.addProperty("value", value);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeVarInt(value);
	}

	@Override
	public boolean test(FHVillagerData ve) {
		return ve.flags.getOrDefault(name,0)==value;
	}

}
