/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.network.PacketBuffer;

public class SerializerRegistry<T> {
	
	Map<String, Function<JsonObject, T>> fromJson = new HashMap<>();
	Map<Class<? extends T>, Pair<Integer,String>> typeInfo = new HashMap<>();
	List<Function<PacketBuffer, T>> fromPacket = new ArrayList<>();
	public SerializerRegistry() {
	}
	public void register(Class<? extends T> cls,String type,Function<JsonObject, T> json,Function<PacketBuffer, T> packet) {
		fromJson.put(type, json);
		int id=fromPacket.size();
		fromPacket.add(packet);
		typeInfo.put(cls, Pair.of(id, type));
	}
	public T read(PacketBuffer pb) {
		int id = pb.readVarInt();
		if (id < 0 || id >= fromPacket.size())
			throw new IllegalArgumentException("Packet Error");
		return fromPacket.get(id).apply(pb);
	}
	public T readOrDefault(PacketBuffer pb,T def) {
		int id = pb.readVarInt();
		if (id < 0 || id >= fromPacket.size())
			return def;
		return fromPacket.get(id).apply(pb);
	}
	public int idOf(T obj) {
		Pair<Integer,String> info=typeInfo.get(obj.getClass());
		if(info==null)
			return -1;
		return info.getFirst();
	}
	public String typeOf(T obj) {
		Pair<Integer,String> info=typeInfo.get(obj.getClass());
		if(info==null)
			return "";
		return info.getSecond();
	}
	public void writeId(PacketBuffer pb,T obj) {
		pb.writeVarInt(idOf(obj));
	}
	public void writeType(JsonObject jo,T obj) {
		jo.addProperty("type", typeOf(obj));;
	}
	public T deserialize(JsonElement je) {
		JsonObject jo = je.getAsJsonObject();
		Function<JsonObject, T> func=fromJson.get(jo.get("type").getAsString());
		if(func==null)
			return null;
		return func.apply(jo);
	}
	public T deserializeOrDefault(JsonElement je,T def) {
		JsonObject jo = je.getAsJsonObject();
		Function<JsonObject, T> func=fromJson.get(jo.get("type").getAsString());
		if(func==null)
			return def;
		return func.apply(jo);
	}
}
