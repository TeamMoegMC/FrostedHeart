package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class Effects {
	private static Map<String,Function<JsonObject,Effect>> fromJson=new HashMap<>();
	private static List<Function<PacketBuffer,Effect>> fromPacket=new ArrayList<>();
	public static void register(String id,Function<JsonObject,Effect> j,Function<PacketBuffer,Effect> p) {
		fromJson.put(id,j);
		fromPacket.add(p);
	}
	static {
		register("multiblock",EffectBuilding::new,EffectBuilding::new);
		register("recipe",EffectCrafting::new,EffectCrafting::new);
		register("item",EffectItemReward::new,EffectItemReward::new);
		register("stats",EffectStats::new,EffectStats::new);
		register("use",EffectUse::new,EffectUse::new);
	}
	private Effects() {
	}
	public static Effect deserialize(JsonObject jo) {
		return fromJson.get(jo.get("type").getAsString()).apply(jo);
	}
	public static Effect deserialize(PacketBuffer data) {
		return fromPacket.get(data.readVarInt()).apply(data);
	}
}
