package com.teammoeg.chorda.util.io.registry;

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
	   return get( pb.readByte());
	}
	public T register(T from) {
		if(types.containsKey(from))return from;
		int id=fromPacket.size();
		fromPacket.add(from);
		types.put(from, id);
		return from;
	}
	public int getId(T obj) {
		Integer dat=types.get(obj.getClass());
		if(dat==null)dat=0;
		return dat;
	}
	public T get(int id) {
	    if (id < 0 || id >= fromPacket.size())
	        throw new IllegalArgumentException("Packet Error");
	    return fromPacket.get(id);
	}
	public void write(FriendlyByteBuf pb, T obj) {

	    pb.writeByte(getId(obj));
	}

}