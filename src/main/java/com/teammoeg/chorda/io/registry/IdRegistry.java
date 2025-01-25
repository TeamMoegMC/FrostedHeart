/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.io.registry;

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
	public synchronized <R extends T> R register(R from) {
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