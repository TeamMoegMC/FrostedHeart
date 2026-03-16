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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 基于身份的ID注册表。使用{@link IdentityHashMap}为对象实例分配数字ID，
 * 支持通过数据包缓冲区进行ID的读写。适用于需要通过引用标识对象的场景。
 * <p>
 * Identity-based ID registry. Uses {@link IdentityHashMap} to assign numeric IDs to object instances,
 * supporting ID reading/writing through packet buffers. Suitable for scenarios requiring
 * object identification by reference.
 *
 * @param <T> 要注册的对象类型 / the type of object to register
 */
public class IdRegistry<T> {

	/** 按ID索引的对象列表 / List of objects indexed by ID */
	private List<T> fromPacket = new ArrayList<>();
	/** 对象实例到ID的映射（使用引用相等） / Map from object instance to ID (using reference equality) */
	private Map<T,Integer> types=new IdentityHashMap<>();

	/**
	 * 构造一个ID注册表。
	 * <p>
	 * Constructs an ID registry.
	 */
	public IdRegistry() {
		super();
	}

	/**
	 * 从数据包缓冲区读取一个字节作为ID，并返回对应的注册对象。
	 * <p>
	 * Reads a byte from the packet buffer as an ID and returns the corresponding registered object.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @return 对应ID的注册对象 / the registered object corresponding to the ID
	 * @throws IllegalArgumentException 如果ID无效 / if the ID is invalid
	 */
	public T read(FriendlyByteBuf pb) {
		byte id=pb.readByte();
		//System.out.println("read "+id);
	   return get( id);
	}
	/**
	 * 注册一个对象并分配数字ID。如果对象已注册则直接返回。此方法是线程安全的。
	 * <p>
	 * Registers an object and assigns a numeric ID. Returns directly if the object is already registered.
	 * This method is thread-safe.
	 *
	 * @param from 要注册的对象 / the object to register
	 * @param <R> 对象的具体类型 / the concrete type of the object
	 * @return 注册的对象（原样返回） / the registered object (returned as-is)
	 */
	public synchronized <R extends T> R register(R from) {
		if(types.containsKey(from))return from;
		int id=fromPacket.size();
		fromPacket.add(from);
		types.put(from, id);
		return from;
	}
	/**
	 * 获取对象的注册数字ID。
	 * <p>
	 * Gets the registered numeric ID of an object.
	 *
	 * @param obj 要查询ID的对象 / the object to query the ID for
	 * @return 对象的数字ID，如果未注册则返回0 / the numeric ID of the object, or 0 if not registered
	 */
	public int getId(T obj) {
		Integer dat=types.get(obj);
		if(dat==null)dat=0;
		return dat;
	}
	/**
	 * 根据数字ID获取注册的对象。
	 * <p>
	 * Gets the registered object by its numeric ID.
	 *
	 * @param id 对象的数字ID / the numeric ID of the object
	 * @return 对应ID的注册对象 / the registered object corresponding to the ID
	 * @throws IllegalArgumentException 如果ID超出范围 / if the ID is out of range
	 */
	public T get(int id) {
	    if (id < 0 || id >= fromPacket.size())
	        throw new IllegalArgumentException("Packet Error");
	    return fromPacket.get(id);
	}
	/**
	 * 将对象的ID作为字节写入数据包缓冲区。
	 * <p>
	 * Writes the object's ID as a byte to the packet buffer.
	 *
	 * @param pb 数据包缓冲区 / the packet buffer
	 * @param obj 要写入ID的对象 / the object whose ID to write
	 */
	public void write(FriendlyByteBuf pb, T obj) {
		int id=getId(obj);
		//System.out.println("write "+id);
	    pb.writeByte(id);
	}

}