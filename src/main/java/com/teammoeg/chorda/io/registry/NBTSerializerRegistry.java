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

import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.io.Writeable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * NBT序列化器注册表。专用于{@link CompoundTag}格式的序列化和反序列化，
 * 同时支持数据包缓冲区的网络传输。
 * <p>
 * NBT serializer registry. Specialized for serialization and deserialization in {@link CompoundTag} format,
 * while also supporting network transport via packet buffers.
 *
 * @param <U> 可写入的对象类型 / the writable object type
 */
public class NBTSerializerRegistry<U extends Writeable> extends PacketBufferSerializerRegistry<U, Object, CompoundTag> {

    /**
     * 构造一个NBT序列化器注册表。
     * <p>
     * Constructs an NBT serializer registry.
     */
    public NBTSerializerRegistry() {
        super();
    }
    /**
     * 从CompoundTag反序列化对象，根据"type"字段查找对应的反序列化函数。
     * <p>
     * Deserializes an object from a CompoundTag, looking up the deserialization function by the "type" field.
     *
     * @param je 要反序列化的CompoundTag / the CompoundTag to deserialize from
     * @return 反序列化后的对象，如果类型未注册则返回null / the deserialized object, or null if the type is not registered
     */
    public U deserialize(CompoundTag je) {
        Function<CompoundTag, U> func = from.get(je.getString("type"));
        if (func == null)
            return null;
        return func.apply(je);
    }

    /**
     * 从CompoundTag反序列化对象，如果类型未注册则返回默认值。
     * <p>
     * Deserializes an object from a CompoundTag, returning the default if the type is not registered.
     *
     * @param je 要反序列化的CompoundTag / the CompoundTag to deserialize from
     * @param def 默认值 / the default value
     * @return 反序列化后的对象或默认值 / the deserialized object or the default value
     */
    public U deserializeOrDefault(CompoundTag je, U def) {
        Function<CompoundTag, U> func = from.get(je.getString("type"));
        if (func == null)
            return def;
        return func.apply(je);
    }
    
	/** {@inheritDoc} */
	@Override
	protected void writeType(Pair<Integer, String> type, CompoundTag obj) {
		obj.putString("type", type.getSecond());
	}
	/** {@inheritDoc} */
	@Override
	protected String readType(CompoundTag obj) {
		return obj.getString("type");
	}
	/**
	 * 注册一个类型及其NBT读写函数和数据包读取函数。
	 * <p>
	 * Registers a type with its NBT read/write functions and packet reading function.
	 *
	 * @param cls 要注册的类 / the class to register
	 * @param type 类型标识符字符串 / the type identifier string
	 * @param json 从CompoundTag反序列化的函数 / function to deserialize from CompoundTag
	 * @param obj 序列化为CompoundTag的函数 / function to serialize to CompoundTag
	 * @param packet 从数据包缓冲区读取的函数 / function to read from packet buffer
	 */
	public void register(Class<? extends U> cls, String type, Function<CompoundTag, U> json, Function<U, CompoundTag> obj, Function<FriendlyByteBuf, U> packet) {
		super.register(cls, type, json, (t,c)->obj.apply(t), packet);
	}
	/**
	 * 将对象序列化为CompoundTag。
	 * <p>
	 * Serializes an object to a CompoundTag.
	 *
	 * @param fromObj 要序列化的对象 / the object to serialize
	 * @return 序列化后的CompoundTag / the serialized CompoundTag
	 */
	public CompoundTag write(U fromObj) {
		return super.write(fromObj, null);
	}
}
