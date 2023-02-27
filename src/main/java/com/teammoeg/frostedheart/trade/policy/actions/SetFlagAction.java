package com.teammoeg.frostedheart.trade.policy.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.Actions;
import com.teammoeg.frostedheart.trade.policy.PolicyAction;

import net.minecraft.network.PacketBuffer;

public class SetFlagAction implements PolicyAction {
	String name;
	public SetFlagAction(JsonObject jo) {
		name=jo.get("name").getAsString();
	}

	public SetFlagAction(PacketBuffer buffer) {
		name=buffer.readString();
	}

	public SetFlagAction(String name) {
		this.name=name;
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Actions.writeType(this, jo);
		jo.addProperty("name",name);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Actions.writeId(this, buffer);
		buffer.writeString(name);
	}

	@Override
	public void deal(FHVillagerData data, int num) {
		data.flags.computeIfAbsent(name,k->1);
	}

}
