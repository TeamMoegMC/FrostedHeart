package com.teammoeg.frostedheart.trade.policy.conditions;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;

import net.minecraft.network.PacketBuffer;

public class GreaterFlagCondition extends FlagValueCondition {

	public GreaterFlagCondition(String name, int val) {
		super(name, val);
	}

	public GreaterFlagCondition(JsonObject jo) {
		super(jo);
	}

	public GreaterFlagCondition(PacketBuffer buffer) {
		super(buffer);
	}
	@Override
	public boolean test(FHVillagerData ve) {
		return ve.flags.getOrDefault(name,0)>=value;
	}
}
