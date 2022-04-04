package com.teammoeg.frostedheart.research.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class Effects {
	private static Map<ResourceLocation,Function<JsonObject,Effect>> fromJson=new HashMap<>();
	private static Map<ResourceLocation,Function<PacketBuffer,Effect>> fromPacket=new HashMap<>();
	public static void register(ResourceLocation id,Function<JsonObject,Effect> j,Function<PacketBuffer,Effect> p) {
		fromJson.put(id,j);
		fromPacket.put(id,p);
	}
	static {
		register(new ResourceLocation(FHMain.MODID,"form_structure"),EffectBuilding::new,EffectBuilding::new);
	}
	private Effects() {
	}
	public static Effect deserialize(JsonObject jo) {
		return fromJson.get(new ResourceLocation(jo.get("type").getAsString())).apply(jo);
	}
	public static Effect deserialize(PacketBuffer data) {
		return fromPacket.get(data.readResourceLocation()).apply(data);
	}
}
