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

package com.teammoeg.frostedheart.research;

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.util.PacketWritable;

import net.minecraft.network.PacketBuffer;

public abstract class PacketBufferSerializerRegistry<T extends PacketWritable, R>  extends SerializerRegistry<T, R> {
	PacketBufferSerializer<T> pbs=new PacketBufferSerializer<T>();
	TypeRegistry<T> types=new TypeRegistry<T>();
	public T read(PacketBuffer pb) {
		return pbs.read(pb);
	}

	public T readOrDefault(PacketBuffer pb, T def) {
		return pbs.readOrDefault(pb, def);
	}
	public T deserializeOrDefault(R jo, T def) {
		T res= super.read(jo);
		if(res==null)return def;
		return res;
	}
	
	protected void writeId(PacketBuffer pb, T obj) {
		pbs.writeId(pb, obj);
	}
	public void write(PacketBuffer pb, T obj) {
		pbs.writeId(pb, obj);
		obj.write(pb);
	}
	public int idOf(T obj) {
		return types.idOf(obj);
	}

	public Pair<Integer, String> typeOf(Class<?> cls) {
		return types.fullTypeOf(cls);
	}

	public void register(Class<? extends T> cls, String type, Function<R, T> json, Function<T, R> obj, Function<PacketBuffer, T> packet) {
		pbs.register(cls, packet);
		types.register(cls, type);
    	super.register( type,json, obj);
	}

	public PacketBufferSerializerRegistry() {
        super();
    }

}