package com.teammoeg.frostedheart.trade.policy.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.policy.Actions;
import com.teammoeg.frostedheart.trade.policy.PolicyAction;

import net.minecraft.network.PacketBuffer;

public abstract class AbstractAction implements PolicyAction {

	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Actions.writeType(this, jo);
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Actions.writeId(this, buffer);
	}


}
