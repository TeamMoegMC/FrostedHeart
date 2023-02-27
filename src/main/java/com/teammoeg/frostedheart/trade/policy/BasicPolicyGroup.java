package com.teammoeg.frostedheart.trade.policy;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.snapshot.PolicySnapshot;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.network.PacketBuffer;

public class BasicPolicyGroup extends PolicyGroup{
	List<BaseData> bdata;
	public BasicPolicyGroup(List<PolicyCondition> conditions, List<BaseData> bdata) {
		super(conditions);
		this.bdata = bdata;
	}
	public BasicPolicyGroup(JsonObject jo) {
		super(jo);
		bdata=SerializeUtil.parseJsonList(jo.get("data"),BaseData::read);
	}

	public BasicPolicyGroup(PacketBuffer pb) {
		super(pb);
		bdata=SerializeUtil.readList(pb,BaseData::read);
	}
	
	@Override
	public void CollectPoliciesNoCheck(PolicySnapshot policy,FHVillagerData ve) {
		bdata.forEach(policy::register);
	}

	@Override
	public JsonElement serialize() {
		JsonObject jo=super.serialize().getAsJsonObject();
		jo.add("data",SerializeUtil.toJsonList(bdata,BaseData::serialize));
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeBoolean(false);
		super.write(buffer);
		SerializeUtil.writeList(buffer,bdata,BaseData::write);
	}
}
