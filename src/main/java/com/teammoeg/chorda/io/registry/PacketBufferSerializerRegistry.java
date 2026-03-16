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

import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.io.PacketWritable;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 数据包缓冲区序列化器注册表。在{@link SerializerRegistry}基础上增加了网络数据包的读写支持，
 * 同时维护类型注册信息，用于多态对象的网络传输。
 * <p>
 * Packet buffer serializer registry. Extends {@link SerializerRegistry} with network packet
 * read/write support, while maintaining type registration for polymorphic object network transmission.
 *
 * @param <T> 可写入数据包的对象类型 / the type of objects writable to packets
 * @param <C> 序列化上下文类型 / the serialization context type
 * @param <R> 序列化的中间表示类型 / the intermediate representation type
 */
public abstract class PacketBufferSerializerRegistry<T extends PacketWritable, C, R>  extends SerializerRegistry<T, C, R> {
	/** 数据包缓冲区序列化器 / Packet buffer serializer */
	PacketBufferSerializer<T> pbs= new PacketBufferSerializer<>();
	/** 类型注册表 / Type registry */
	TypeRegistry<T> types= new TypeRegistry<>();
	/**
	 * 从数据包缓冲区中读取对象。
	 * <p>
	 * Reads an object from a packet buffer.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @return 反序列化的对象 / the deserialized object
	 */
	public T read(FriendlyByteBuf pb) {
		return pbs.read(pb);
	}

	/**
	 * 从数据包缓冲区中读取对象，如果失败则返回默认值。
	 * <p>
	 * Reads an object from a packet buffer, returning a default value on failure.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @param def 默认值 / the default value
	 * @return 反序列化的对象或默认值 / the deserialized object or the default value
	 */
	public T readOrDefault(FriendlyByteBuf pb, T def) {
		return pbs.readOrDefault(pb, def);
	}
	/**
	 * 从中间表示中反序列化对象，如果结果为null则返回默认值。
	 * <p>
	 * Deserializes an object from the intermediate representation, returning a default value if the result is null.
	 *
	 * @param jo 序列化的输入 / the serialized input
	 * @param def 默认值 / the default value
	 * @return 反序列化的对象或默认值 / the deserialized object or the default value
	 */
	public T deserializeOrDefault(R jo, T def) {
		T res= super.read(jo);
		if(res==null)return def;
		return res;
	}
	
	/**
	 * 将对象的类型ID写入数据包缓冲区。
	 * <p>
	 * Writes the type ID of an object to the packet buffer.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @param obj 要写入ID的对象 / the object whose ID to write
	 */
	protected void writeId(FriendlyByteBuf pb, T obj) {
		pbs.writeId(pb, obj);
	}
	/**
	 * 将对象完整写入数据包缓冲区（先写类型ID，再写对象数据）。
	 * <p>
	 * Writes the complete object to the packet buffer (type ID first, then object data).
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @param obj 要写入的对象 / the object to write
	 */
	public void write(FriendlyByteBuf pb, T obj) {
		pbs.writeId(pb, obj);
		obj.write(pb);
	}
	/**
	 * 获取对象的类型ID。
	 * <p>
	 * Gets the type ID of an object.
	 *
	 * @param obj 要查询的对象 / the object to look up
	 * @return 类型ID / the type ID
	 */
	public int idOf(T obj) {
		return types.idOf(obj);
	}

	/**
	 * 获取类的完整类型信息。
	 * <p>
	 * Gets the full type information for a class.
	 *
	 * @param cls 要查询的类 / the class to look up
	 * @return 包含类型ID和名称的Pair / a Pair containing the type ID and name
	 */
	public Pair<Integer, String> typeOf(Class<?> cls) {
		return types.fullTypeOf(cls);
	}

	/**
	 * 注册一个类型的完整序列化支持（通用序列化 + 网络数据包）。
	 * <p>
	 * Registers full serialization support for a type (generic serialization + network packets).
	 *
	 * @param cls 要注册的类 / the class to register
	 * @param type 类型标识符 / the type identifier
	 * @param read 反序列化函数 / the deserialization function
	 * @param write 序列化函数 / the serialization function
	 * @param packet 数据包反序列化函数 / the packet deserialization function
	 */
	public synchronized void register(Class<? extends T> cls, String type, Function<R, T> read, BiFunction<T, C, R> write, Function<FriendlyByteBuf, T> packet) {
		pbs.register(cls, packet);
		types.register(cls, type);
    	super.register( type,read, write);
	}

	/**
	 * 构造一个空的数据包缓冲区序列化器注册表。
	 * <p>
	 * Constructs an empty packet buffer serializer registry.
	 */
	public PacketBufferSerializerRegistry() {
        super();
    }

}