package com.teammoeg.frostedheart.trade;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;

public class DemandData extends BaseData {
	Ingredient item;

	public DemandData(String id, int maxstore, float recover, int price, Ingredient item) {
		super(id, maxstore, recover, price);
		this.item = item;
	}

	public DemandData(JsonObject jo) {
		super(jo);
		item = Ingredient.deserialize(jo.get("demand"));
	}

	public DemandData(PacketBuffer pb) {
		super(pb);
		item = Ingredient.read(pb);
	}

	@Override
	public void fetch(List<BuyData> buys, List<SellData> sell, Map<String, Float> data) {
		int num = (int) (float) data.getOrDefault(getId(), 0f);
		if (num > 0)
			buys.add(new BuyData(item, num, getId()));
	}

	@Override
	public JsonElement serialize() {
		JsonObject jo = super.serialize().getAsJsonObject();
		jo.add("demand", item.serialize());
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeBoolean(false);
		super.write(buffer);
		item.write(buffer);
	}

	@Override
	public String getType() {
		return "b";
	}

}
