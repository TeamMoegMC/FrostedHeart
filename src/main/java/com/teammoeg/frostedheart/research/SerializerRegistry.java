package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import net.minecraft.network.PacketBuffer;

public abstract class SerializerRegistry<T,R> {

	protected Map<Class<? extends T>, Pair<Integer,String>> typeInfo = new HashMap<>();
	protected List<Function<PacketBuffer, T>> fromPacket = new ArrayList<>();

	public SerializerRegistry() {
		super();
	}

	public void register(Class<? extends T> cls, String type, Function<R,T> json, Function<PacketBuffer,T> packet) {
		putSerializer(type, json);
		int id=fromPacket.size();
		fromPacket.add(packet);
		typeInfo.put(cls, Pair.of(id, type));
	}
	protected abstract void putSerializer(String type,Function<R, T> s);
	public T read(PacketBuffer pb) {
		int id = pb.readVarInt();
		if (id < 0 || id >= fromPacket.size())
			throw new IllegalArgumentException("Packet Error");
		return fromPacket.get(id).apply(pb);
	}

	public T readOrDefault(PacketBuffer pb, T def) {
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

	public void writeId(PacketBuffer pb, T obj) {
		pb.writeVarInt(idOf(obj));
	}

}