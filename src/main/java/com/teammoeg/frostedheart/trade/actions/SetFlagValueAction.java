package com.teammoeg.frostedheart.trade.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.Actions;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.PolicyAction;

import net.minecraft.network.PacketBuffer;

public class SetFlagValueAction extends SetFlagAction {
	int value;
	public SetFlagValueAction(JsonObject jo) {
		super(jo);
		value=jo.get("value").getAsInt();
	}
	public SetFlagValueAction(String name, int value) {
		super(name);
		this.value = value;
	}
	public SetFlagValueAction(PacketBuffer buffer) {
		super(buffer);
		value=buffer.readVarInt();
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=super.serialize().getAsJsonObject();
		jo.addProperty("value", value);
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		buffer.writeVarInt(value);
	}
	@Override
	public void deal(FHVillagerData data, int num) {
		if(value!=0)
			data.flags.put(name, value);
		else
			data.flags.remove(name);
	}


}
