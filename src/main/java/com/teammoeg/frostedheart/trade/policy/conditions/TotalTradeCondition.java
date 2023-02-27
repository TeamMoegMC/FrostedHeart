package com.teammoeg.frostedheart.trade.policy.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.Conditions;
import com.teammoeg.frostedheart.trade.policy.PolicyCondition;

import net.minecraft.network.PacketBuffer;

public class TotalTradeCondition implements PolicyCondition{
	int level;
	
	public TotalTradeCondition(int level) {
		super();
		this.level = level;
	}
	public TotalTradeCondition(JsonObject jo) {
		this(jo.get("point").getAsInt());
	}
	public TotalTradeCondition(PacketBuffer buffer) {
		this(buffer.readVarInt());
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Conditions.writeType(this, jo);
		jo.addProperty("point", level);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Conditions.writeId(this, buffer);
		buffer.writeVarInt(level);
	}

	@Override
	public boolean test(FHVillagerData ve) {
		return ve.getTotaltraded()>=level;
	}

}
