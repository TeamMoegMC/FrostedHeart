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

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.io.Writeable;

import net.minecraft.network.FriendlyByteBuf;

/**
 * JSON序列化器注册表。专用于{@link JsonObject}格式的序列化和反序列化，
 * 同时支持数据包缓冲区的网络传输。
 * <p>
 * JSON serializer registry. Specialized for serialization and deserialization in {@link JsonObject} format,
 * while also supporting network transport via packet buffers.
 *
 * @param <U> 可写入的对象类型 / the writable object type
 */
public class JsonSerializerRegistry<U extends Writeable> extends PacketBufferSerializerRegistry<U, Object, JsonObject> {

    /**
     * 构造一个JSON序列化器注册表。
     * <p>
     * Constructs a JSON serializer registry.
     */
    public JsonSerializerRegistry() {
        super();
    }

	/** {@inheritDoc} */
	@Override
	protected void writeType(Pair<Integer, String> type, JsonObject obj) {
		obj.addProperty("type", type.getSecond());
	}

	/** {@inheritDoc} */
	@Override
	protected String readType(JsonObject obj) {
		return obj.get("type").getAsString();
	}

	/**
	 * 注册一个类型及其JSON读写函数和数据包读取函数。
	 * <p>
	 * Registers a type with its JSON read/write functions and packet reading function.
	 *
	 * @param cls 要注册的类 / the class to register
	 * @param type 类型标识符字符串 / the type identifier string
	 * @param json 从JsonObject反序列化的函数 / function to deserialize from JsonObject
	 * @param obj 序列化为JsonObject的函数 / function to serialize to JsonObject
	 * @param packet 从数据包缓冲区读取的函数 / function to read from packet buffer
	 */
	public void register(Class<? extends U> cls, String type, Function<JsonObject, U> json, Function<U, JsonObject> obj, Function<FriendlyByteBuf, U> packet) {
		super.register(cls, type, json, (t,c)->obj.apply(t), packet);
	}

	/**
	 * 从JSON对象读取并反序列化对象。
	 * <p>
	 * Reads and deserializes an object from a JSON object.
	 *
	 * @param fromObj 要反序列化的JSON对象 / the JSON object to deserialize from
	 * @return 反序列化后的对象 / the deserialized object
	 */
	@Override
	public U read(JsonObject fromObj) {
		return super.read(fromObj);
	}

	/**
	 * 将对象序列化为JSON对象。
	 * <p>
	 * Serializes an object to a JSON object.
	 *
	 * @param fromObj 要序列化的对象 / the object to serialize
	 * @return 序列化后的JSON对象 / the serialized JSON object
	 */
	public JsonObject write(U fromObj) {
		return super.write(fromObj, null);
	}


}
