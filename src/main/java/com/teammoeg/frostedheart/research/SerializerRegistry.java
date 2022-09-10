package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.network.PacketBuffer;

public class SerializerRegistry<T> {
	Map<String, Function<JsonObject, T>> fromJson = new HashMap<>();
	Map<Class<? extends T>, Integer> toPacket = new HashMap<>();
	Map<Class<? extends T>, String> TypeOf = new HashMap<>();
	List<Function<PacketBuffer, T>> fromPacket = new ArrayList<>();

	public SerializerRegistry() {
	}
	public void register(Class<? extends T> cls,String type,Function<JsonObject, T> json,Function<PacketBuffer, T> packet) {
		fromJson.put(type, json);
		int id=fromPacket.size();
		fromPacket.add(packet);
		toPacket.put(cls, id);
	}
	public T read(PacketBuffer pb) {
		int id = pb.readVarInt();
		if (id < 0 || id >= fromPacket.size())
			throw new IllegalArgumentException("Packet Error");
		return fromPacket.get(id).apply(pb);
	}
	public int idOf(T obj) {
		return toPacket.getOrDefault(obj.getClass(),-1);
	}
	public void writeId(PacketBuffer pb,T obj) {
		pb.writeVarInt(idOf(obj));
	}
	public T read(JsonElement je) {
		JsonObject jo = je.getAsJsonObject();
		return fromJson.get(jo.get("type").getAsString()).apply(jo);

	}
}
