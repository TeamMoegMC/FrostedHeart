package com.teammoeg.frostedheart.trade.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.Conditions;
import com.teammoeg.frostedheart.trade.PolicyCondition;
import com.teammoeg.frostedheart.trade.FHVillagerData;

import net.minecraft.network.PacketBuffer;

public class NotCondition implements PolicyCondition{
	PolicyCondition nested;
	
	public NotCondition(PolicyCondition n) {
		super();
		this.nested=n;
	}
	public NotCondition(JsonObject jo) {
		this(Conditions.deserialize(jo.get("condition").getAsJsonObject()));
	}
	public NotCondition(PacketBuffer buffer) {
		this(Conditions.deserialize(buffer));
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Conditions.writeType(this, jo);
		jo.add("condition",nested.serialize());
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Conditions.writeId(this, buffer);
		nested.write(buffer);
	}

	@Override
	public boolean test(FHVillagerData ve) {
		return !nested.test(ve);
	}

}
