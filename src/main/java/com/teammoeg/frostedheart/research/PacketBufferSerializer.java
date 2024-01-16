package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.network.PacketBuffer;

public class PacketBufferSerializer<T> {

	private List<Function<PacketBuffer, T>> fromPacket = new ArrayList<>();
	private Map<Class<? extends T>,Integer> types=new HashMap<>();
	public PacketBufferSerializer() {
		super();
	}

	public T read(PacketBuffer pb) {
	    int id = pb.readVarInt();
	    if (id < 0 || id >= fromPacket.size())
	        throw new IllegalArgumentException("Packet Error");
	    return fromPacket.get(id).apply(pb);
	}
	public void register(Class<? extends T> cls,Function<PacketBuffer, T> from) {
		int id=fromPacket.size();
		fromPacket.add(from);
		types.put(cls, id);
	}
	public T readOrDefault(PacketBuffer pb, T def) {
	    int id = pb.readVarInt();
	    if (id < 0 || id >= fromPacket.size())
	        return def;
	    return fromPacket.get(id).apply(pb);
	}

	public void writeId(PacketBuffer pb, T obj) {
		Integer dat=types.get(obj.getClass());
		if(dat==null)dat=0;
	    pb.writeVarInt(dat);
	}

}