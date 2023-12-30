package com.teammoeg.frostedheart.trade.policy.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.Conditions;
import com.teammoeg.frostedheart.trade.policy.PolicyCondition;

import net.minecraft.network.PacketBuffer;

public class LevelCondition implements PolicyCondition{
	int level;
	
	public LevelCondition(int level) {
		super();
		this.level = level;
	}
	public LevelCondition(JsonObject jo) {
		this(jo.get("level").getAsInt());
	}
	public LevelCondition(PacketBuffer buffer) {
		this(buffer.readVarInt());
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Conditions.writeType(this, jo);
		jo.addProperty("level", level);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Conditions.writeId(this, buffer);
		buffer.writeVarInt(level);
	}

	@Override
	public boolean test(FHVillagerData ve) {
		return ve.getTradeLevel()>=level;
	}

}
