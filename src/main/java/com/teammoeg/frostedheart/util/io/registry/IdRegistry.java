package com.teammoeg.frostedheart.util.io.registry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;

public class IdRegistry<T> {

	private List<T> fromPacket = new ArrayList<>();
	private Map<T,Integer> types=new IdentityHashMap<>();
	public IdRegistry() {
		super();
	}

	public T read(FriendlyByteBuf pb) {
	    int id = pb.readByte();
	    if (id < 0 || id >= fromPacket.size())
	        throw new IllegalArgumentException("Packet Error");
	    return fromPacket.get(id);
	}
	public T register(T from) {
		int id=fromPacket.size();
		fromPacket.add(from);
		types.put(from, id);
		return from;
	}
	public void write(FriendlyByteBuf pb, T obj) {
		Integer dat=types.get(obj.getClass());
		if(dat==null)dat=0;
	    pb.writeByte(dat);
	}

}