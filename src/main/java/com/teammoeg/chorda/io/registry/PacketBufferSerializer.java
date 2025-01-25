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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;

public class PacketBufferSerializer<T> {

	private List<Function<FriendlyByteBuf, T>> fromPacket = new ArrayList<>();
	private Map<Class<? extends T>,Integer> types=new HashMap<>();
	public PacketBufferSerializer() {
		super();
	}

	public T read(FriendlyByteBuf pb) {
	    int id = pb.readByte();
	    if (id < 0 || id >= fromPacket.size())
	        throw new IllegalArgumentException("Packet Error");
	    return fromPacket.get(id).apply(pb);
	}
	public void register(Class<? extends T> cls,Function<FriendlyByteBuf, T> from) {
		int id=fromPacket.size();
		fromPacket.add(from);
		types.put(cls, id);
	}
	public T readOrDefault(FriendlyByteBuf pb, T def) {
	    int id = pb.readByte();
	    if (id < 0 || id >= fromPacket.size())
	        return def;
	    return fromPacket.get(id).apply(pb);
	}

	protected void writeId(FriendlyByteBuf pb, T obj) {
		Integer dat=types.get(obj.getClass());
		if(dat==null)dat=0;
	    pb.writeByte(dat);
	}

}