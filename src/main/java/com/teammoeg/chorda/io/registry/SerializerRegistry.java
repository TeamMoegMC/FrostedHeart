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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

/**
 * 抽象序列化器注册表。管理类型字符串与序列化/反序列化函数之间的映射关系，
 * 支持通过类型标识进行多态对象的读写。
 * <p>
 * Abstract serializer registry. Manages mappings between type strings and
 * serialization/deserialization functions, supporting polymorphic object read/write
 * via type identifiers.
 *
 * @param <T> 被序列化的对象类型 / the type of objects being serialized
 * @param <C> 序列化上下文类型 / the serialization context type
 * @param <R> 序列化的中间表示类型（如JSON或NBT） / the intermediate representation type (e.g. JSON or NBT)
 */
public abstract class SerializerRegistry<T, C, R> {
	/** 类型名称 -> 反序列化函数的映射 / Mapping from type name to deserialization function */
	protected Map<String, Function<R, T>> from = new HashMap<>();
	/** 类型名称 -> 序列化函数的映射 / Mapping from type name to serialization function */
    protected Map<String, BiFunction<T, C, R>> to = new HashMap<>();
	/**
	 * 构造一个空的序列化器注册表。
	 * <p>
	 * Constructs an empty serializer registry.
	 */
	public SerializerRegistry() {
		super();
	}

	/**
	 * 注册一个反序列化函数。
	 * <p>
	 * Registers a deserialization function.
	 *
	 * @param type 类型标识符 / the type identifier
	 * @param s 反序列化函数 / the deserialization function
	 */
	protected void putSerializer(String type, Function<R, T> s) {
		from.put(type, s);
	}

    /**
	 * 注册一个序列化函数。
	 * <p>
	 * Registers a serialization function.
	 *
	 * @param type 类型标识符 / the type identifier
	 * @param s 序列化函数 / the serialization function
	 */
    protected void putDeserializer(String type, BiFunction<T, C, R> s) {
		to.put(type, s);
	}

    /**
	 * 将类型信息写入序列化结果中。
	 * <p>
	 * Writes type information into the serialized result.
	 *
	 * @param type 类型的ID和名称对 / the type ID and name pair
	 * @param obj 要写入类型信息的序列化对象 / the serialized object to write type info into
	 */
    protected abstract void writeType(Pair<Integer, String> type,R obj);
	/**
	 * 从序列化表示中读取类型标识符。
	 * <p>
	 * Reads the type identifier from the serialized representation.
	 *
	 * @param obj 序列化的对象 / the serialized object
	 * @return 类型标识符，如果不存在则返回null / the type identifier, or null if not present
	 */
	protected abstract String readType(R obj);
	/**
	 * 从序列化表示中反序列化对象。首先读取类型，然后查找对应的反序列化函数。
	 * <p>
	 * Deserializes an object from the serialized representation. Reads the type first,
	 * then looks up the corresponding deserialization function.
	 *
	 * @param fromObj 序列化的输入 / the serialized input
	 * @return 反序列化的对象，如果类型未知则返回null / the deserialized object, or null if the type is unknown
	 */
	public T read(R fromObj) {
		String type=readType(fromObj);
		if(type==null)return null;
		Function<R, T> ffrom=from.get(type);
		if(ffrom==null)return null;
		return ffrom.apply(fromObj);
	}
	/**
	 * 获取指定类的类型信息（ID和名称）。
	 * <p>
	 * Gets the type information (ID and name) for the given class.
	 *
	 * @param cls 要查询的类 / the class to look up
	 * @return 类型ID和名称对 / the type ID and name pair
	 */
	public abstract Pair<Integer,String> typeOf(Class<?> cls);
	/**
	 * 将对象序列化为中间表示。自动附加类型信息。
	 * <p>
	 * Serializes an object to the intermediate representation. Automatically appends type information.
	 *
	 * @param fromObj 要序列化的对象 / the object to serialize
	 * @param context 序列化上下文 / the serialization context
	 * @return 序列化的中间表示，如果对象为null或类型未知则返回null / the serialized representation, or null if the object is null or type is unknown
	 */
	public R write(T fromObj,C context) {
		if(fromObj==null)return null;
		Pair<Integer, String> type=typeOf(fromObj.getClass());
		if(type==null)return null;
		BiFunction<T, C, R> ffrom=to.get(type.getSecond());
		if(ffrom==null)return null;
		R obj= ffrom.apply(fromObj, context);
		writeType(type,obj);
		return obj;
	}
	/**
	 * 注册一个类型的序列化和反序列化函数对。
	 * <p>
	 * Registers a serialization and deserialization function pair for a type.
	 *
	 * @param type 类型标识符 / the type identifier
	 * @param json 反序列化函数 / the deserialization function
	 * @param obj 序列化函数 / the serialization function
	 */
	protected void register(String type, Function<R, T> json, BiFunction<T, C, R> obj) {
	    putSerializer(type, json);
	    putDeserializer(type,obj);
	}
}