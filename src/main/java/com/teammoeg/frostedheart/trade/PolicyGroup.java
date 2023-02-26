package com.teammoeg.frostedheart.trade;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.network.PacketBuffer;

public abstract class PolicyGroup implements Writeable{
	List<PolicyCondition> conditions;
	public PolicyGroup(List<PolicyCondition> conditions) {
		super();
		this.conditions = conditions;
	}
	public PolicyGroup(JsonObject jo) {
		super();
		if(jo.has("conditions"))
			conditions=SerializeUtil.parseJsonList(jo.get("conditions"),Conditions::deserialize);
		else
			conditions=ImmutableList.of();
	}
	public PolicyGroup(PacketBuffer pb) {
		super();
		conditions=SerializeUtil.readList(pb, Conditions::deserialize);
	}
	public abstract void CollectPoliciesNoCheck(PolicySnapshot policy,FHVillagerData ve);
	public void CollectPolicies(PolicySnapshot policy,FHVillagerData ve) {
		if(conditions.stream().allMatch(t->t.test(ve))) 
			CollectPoliciesNoCheck(policy,ve);
		
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		jo.add("conditions",SerializeUtil.toJsonList(conditions,PolicyCondition::serialize));
		return jo;
	}
	
	@Override
	public void write(PacketBuffer buffer) {
		SerializeUtil.writeList(buffer,conditions,PolicyCondition::write);
	};
	public static PolicyGroup read(JsonObject jo) {
		if(jo.has("parent"))
			return new ExtendPolicyGroup(jo);
		return new BasicPolicyGroup(jo);
	}
	public static PolicyGroup read(PacketBuffer pb) {
		if(pb.readBoolean())
			return new ExtendPolicyGroup(pb);
		return new BasicPolicyGroup(pb);
	}
}
