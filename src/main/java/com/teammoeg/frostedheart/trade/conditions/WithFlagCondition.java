package com.teammoeg.frostedheart.trade.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.Conditions;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.PolicyCondition;

import net.minecraft.network.PacketBuffer;

public class WithFlagCondition implements PolicyCondition{
	String name;
	
	public WithFlagCondition(String name) {
		super();
		this.name = name;
	}
	public WithFlagCondition(JsonObject jo) {
		this(jo.get("name").getAsString());
	}
	public WithFlagCondition(PacketBuffer buffer) {
		this(buffer.readString());
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Conditions.writeType(this, jo);
		jo.addProperty("name", name);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Conditions.writeId(this, buffer);
		buffer.writeString(name);
	}

	@Override
	public boolean test(FHVillagerData ve) {
		return ve.flags.containsKey(name);
	}

}
