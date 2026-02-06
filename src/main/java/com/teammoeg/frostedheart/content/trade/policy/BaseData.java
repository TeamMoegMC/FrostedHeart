/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.trade.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.io.Writeable;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.TradeConstants;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;

public abstract class BaseData implements Writeable {
	private String id;
	public int maxstore;
	float recover;
	public int price;
	public List<PolicyAction> actions;
	public List<PolicyAction> soldactions = new ArrayList<>();
	public List<PolicyCondition> sellconditions = new ArrayList<>();
	public List<PolicyCondition> restockconditions = new ArrayList<>();
	boolean hideStockout;

	public static BaseData read(JsonObject jo) {
		if (jo.has("produce"))
			return new ProductionData(jo);
		else if (jo.has("demand"))
			return new DemandData(jo);
		return new NopData(jo);

	}

	public static BaseData read(FriendlyByteBuf pb) {
		switch (pb.readVarInt()) {
		case 1:
			return new ProductionData(pb);
		case 2:
			return new DemandData(pb);
		default:
			return new NopData(pb);
		}
	}

	public BaseData(JsonObject jo) {
		id = jo.get("id").getAsString();
		maxstore = jo.get("store").getAsInt();
		recover = jo.get("recover").getAsFloat();
		price = jo.get("price").getAsInt();
		this.actions = SerializeUtil.parseJsonList(jo.get("actions"), Actions::deserialize);
		this.soldactions = SerializeUtil.parseJsonList(jo.get("use_actions"), Actions::deserialize);
		this.restockconditions = SerializeUtil.parseJsonList(jo.get("restock_condition"), Conditions::deserialize);
		this.sellconditions=SerializeUtil.parseJsonList(jo.get("sell_conditions"), Conditions::deserialize);
		if (jo.has("hide_stockout"))
			hideStockout = jo.get("hide_stockout").getAsBoolean();
	}

	public BaseData(FriendlyByteBuf pb) {
		id = pb.readUtf();
		maxstore = pb.readVarInt();
		recover = pb.readFloat();
		price = pb.readVarInt();
		this.actions = SerializeUtil.readList(pb, Actions::deserialize);
		this.soldactions = SerializeUtil.readList(pb, Actions::deserialize);
		this.restockconditions = SerializeUtil.readList(pb, Conditions::deserialize);
		this.sellconditions = SerializeUtil.readList(pb, Conditions::deserialize);
		hideStockout = pb.readBoolean();
	}

	public BaseData(String id, int maxstore, float recover, int price, PolicyAction... restock) {
		super();
		this.id = id;
		this.maxstore = maxstore;
		this.recover = recover;
		this.price = price;
		this.actions = new ArrayList<>(Arrays.asList(restock));
	}

	public int canRestock(FHVillagerData fhvd) {
		return restockconditions.stream().mapToInt(c -> c.test(fhvd)).reduce(Integer.MAX_VALUE, Math::min);
	}

	public void execute(FHVillagerData data, int count) {
		soldactions.forEach(c -> c.deal(data, count));
	}

	public abstract void fetch(PolicySnapshot shot,FHVillagerData vd, Map<String, Float> data);

	public String getId() {
		return id + "_" + getType();
	}

	public abstract String getType();

	@Override
	public JsonElement serialize() {
		JsonObject jo = new JsonObject();
		jo.addProperty("id", id);
		jo.addProperty("store", maxstore);
		jo.addProperty("recover", recover);
		jo.addProperty("price", price);
		jo.add("actions", SerializeUtil.toJsonList(actions, Actions::serialize));
		jo.add("use_actions", SerializeUtil.toJsonList(soldactions, Actions::serialize));
		jo.add("restock_condition", SerializeUtil.toJsonList(restockconditions, Conditions::serialize));
		jo.add("sell_condition", SerializeUtil.toJsonList(sellconditions, Conditions::serialize));
		jo.addProperty("hide_stockout", hideStockout);

		return jo;
	}

	public void tick(int deltaDay, FHVillagerData data) {
		// System.out.println("try recover for "+id+" : "+deltaDay);
		if (deltaDay > 0) {
			deltaDay = Math.min(deltaDay, canRestock(data));
			if (deltaDay > 0) {
				float actualRecover = recover;
				AbstractVillager v = data.parent;
				if (v instanceof Villager villager) {
					// if a villager does not have a home (bed), then it produces much less
					Brain<Villager> brain = villager.getBrain();
					// Does it *have* a home (claimed bed)?
					boolean hasHome = brain.hasMemoryValue(MemoryModuleType.HOME);
					if (!hasHome) {
						actualRecover /= TradeConstants.NO_HOME_RECOVER_PUNISHEMENT_DIVISOR;
					} else {
						float t = WorldTemperature.block(v.level(), v.blockPosition());
						if (t < TradeConstants.TOO_COLD_RECOVER_TEMP) {
							actualRecover /= TradeConstants.TEMPERATURE_RECOVER_DIVISOR;
						}
					}
				}
				// other types are all wandering, very slow!
				else {
					actualRecover /= TradeConstants.NO_HOME_RECOVER_PUNISHEMENT_DIVISOR;
				}

				float curstore = data.storage.getOrDefault(getId(), 0f);
				int recDay = Math.min((int) Math.ceil((maxstore - curstore) / actualRecover), deltaDay);
				float val = Math.min(actualRecover * recDay + curstore, maxstore);
				data.storage.put(getId(), val);
				actions.forEach(c -> c.deal(data, recDay));
			}
		}
	}

	@Override
	public String toString() {
		return "BaseData [id=" + id + ", maxstore=" + maxstore + ", recover=" + recover + ", price=" + price + "]";
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(id);
		buffer.writeVarInt(maxstore);
		buffer.writeFloat(recover);
		buffer.writeVarInt(price);
		SerializeUtil.writeList(buffer, actions, Actions::write);
		SerializeUtil.writeList(buffer, soldactions, Actions::write);
		SerializeUtil.writeList(buffer, restockconditions, Conditions::write);
		SerializeUtil.writeList(buffer, sellconditions, Conditions::write);
		buffer.writeBoolean(hideStockout);
	}
}
