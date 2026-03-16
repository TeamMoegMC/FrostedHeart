/*
 * Copyright (c) 2026 TeamMoeg
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

/**
 * 数据包缓冲区序列化器。管理对象类型与数据包缓冲区之间的序列化和反序列化，
 * 使用数字ID标识不同类型。
 * <p>
 * Packet buffer serializer. Manages serialization and deserialization between object types
 * and packet buffers, using numeric IDs to identify different types.
 *
 * @param <T> 要序列化的基础类型 / the base type to serialize
 */
public class PacketBufferSerializer<T> {

	/** 按ID索引的数据包反序列化函数列表 / List of packet deserialization functions indexed by ID */
	private List<Function<FriendlyByteBuf, T>> fromPacket = new ArrayList<>();
	/** 类到数字ID的映射 / Map from class to numeric ID */
	private Map<Class<? extends T>,Integer> types=new HashMap<>();

	/**
	 * 构造一个数据包缓冲区序列化器。
	 * <p>
	 * Constructs a packet buffer serializer.
	 */
	public PacketBufferSerializer() {
		super();
	}

	/**
	 * 从数据包缓冲区读取对象。先读取一个字节作为类型ID，再调用对应的反序列化函数。
	 * <p>
	 * Reads an object from a packet buffer. Reads a byte as the type ID first,
	 * then calls the corresponding deserialization function.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @return 反序列化后的对象 / the deserialized object
	 * @throws IllegalArgumentException 如果类型ID无效 / if the type ID is invalid
	 */
	public T read(FriendlyByteBuf pb) {
	    int id = pb.readByte();
	    if (id < 0 || id >= fromPacket.size())
	        throw new IllegalArgumentException("Packet Error");
	    return fromPacket.get(id).apply(pb);
	}
	/**
	 * 注册一个类型及其数据包反序列化函数。
	 * <p>
	 * Registers a type with its packet deserialization function.
	 *
	 * @param cls 要注册的类 / the class to register
	 * @param from 从数据包缓冲区读取对象的函数 / function to read the object from a packet buffer
	 */
	public void register(Class<? extends T> cls,Function<FriendlyByteBuf, T> from) {
		int id=fromPacket.size();
		fromPacket.add(from);
		types.put(cls, id);
	}
	/**
	 * 从数据包缓冲区读取对象，如果类型ID无效则返回默认值。
	 * <p>
	 * Reads an object from a packet buffer, returning the default if the type ID is invalid.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @param def 默认值 / the default value
	 * @return 反序列化后的对象或默认值 / the deserialized object or the default value
	 */
	public T readOrDefault(FriendlyByteBuf pb, T def) {
	    int id = pb.readByte();
	    if (id < 0 || id >= fromPacket.size())
	        return def;
	    return fromPacket.get(id).apply(pb);
	}

	/**
	 * 将对象的类型ID作为字节写入数据包缓冲区。
	 * <p>
	 * Writes the object's type ID as a byte to the packet buffer.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @param obj 要写入ID的对象 / the object whose ID to write
	 */
	protected void writeId(FriendlyByteBuf pb, T obj) {
		Integer dat=types.get(obj.getClass());
		if(dat==null)dat=0;
	    pb.writeByte(dat);
	}

}