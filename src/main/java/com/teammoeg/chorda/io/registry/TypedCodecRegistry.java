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

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.CompressDifferCodec;
import com.teammoeg.chorda.io.codec.KeyMapCodec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;

/**
 * 基于Codec的类型化注册表。扩展{@link TypeRegistry}，为每个注册类型关联一个{@link MapCodec}，
 * 支持通过名称或整数ID进行多态对象的Codec编解码和网络传输。
 * <p>
 * Codec-based typed registry. Extends {@link TypeRegistry} by associating a {@link MapCodec}
 * with each registered type, supporting polymorphic object Codec encoding/decoding and network
 * transmission via name or integer ID.
 *
 * @param <T> 注册的基类型 / the base type being registered
 */
public class TypedCodecRegistry<T> extends TypeRegistry<T> {
	/** 类型名称 -> MapCodec的映射 / Mapping from type name to MapCodec */
	Map<String,MapCodec<T>> codecs=new HashMap<>();
	/** 按注册顺序排列的MapCodec列表 / List of MapCodecs in registration order */
	List<MapCodec<T>> codecList=new ArrayList<>();
	/** 按注册顺序排列的Codec列表 / List of Codecs in registration order */
	List<Codec<T>> codecCodecList=new ArrayList<>();
	/** 基于名称的Codec，使用类型名称作为鉴别器 / Name-based Codec using type name as discriminator */
	Codec<T> byName=new KeyMapCodec<T,String>(Codec.STRING,o->this.typeOf(o.getClass()),this::getCodec);
	/** 基于整数ID的Codec，使用注册顺序ID作为鉴别器 / Integer ID-based Codec using registration order ID as discriminator */
	Codec<T> byInt=Codec.INT.dispatch(this::idOf,codecCodecList::get);
	/**
	 * 注册一个类型及其对应的MapCodec。线程安全。
	 * <p>
	 * Registers a type with its corresponding MapCodec. Thread-safe.
	 *
	 * @param <A> 要注册的具体类型 / the specific type to register
	 * @param cls 要注册的类 / the class to register
	 * @param type 类型名称 / the type name
	 * @param codec 该类型的MapCodec / the MapCodec for the type
	 */
	public synchronized <A extends T>  void register(Class<A> cls, String type,MapCodec<A> codec) {
		codecs.put(type, (MapCodec<T>) codec);
		codecList.add((MapCodec<T>) codec);
		codecCodecList.add((Codec<T>)codec.codec());
		super.register(cls, type);

	}
	/**
	 * 获取基于名称的Codec。适用于可读性优先的序列化（如JSON）。
	 * <p>
	 * Gets the name-based Codec. Suitable for readability-first serialization (e.g. JSON).
	 *
	 * @return 基于名称的Codec / the name-based Codec
	 */
	public Codec<T> byNameCodec(){
		return byName;
	}
	/**
	 * 获取自适应压缩Codec。在可读格式中使用名称，在压缩格式中使用整数ID。
	 * <p>
	 * Gets the adaptive compression Codec. Uses names in readable formats and integer IDs in compressed formats.
	 *
	 * @return 自适应压缩Codec / the adaptive compression Codec
	 */
	public Codec<T> codec(){
		return CodecUtil.catchingCodec(new CompressDifferCodec<>(byName,byInt));
	}
	/**
	 * 获取基于整数ID的Codec。适用于紧凑序列化（如网络传输）。
	 * <p>
	 * Gets the integer ID-based Codec. Suitable for compact serialization (e.g. network transmission).
	 *
	 * @return 基于整数ID的Codec / the integer ID-based Codec
	 */
	public Codec<T> byIntCodec(){
		return byInt;
	}
	/**
	 * 将对象写入数据包缓冲区，使用整数ID编码。
	 * <p>
	 * Writes an object to a packet buffer using integer ID encoding.
	 *
	 * @param obj 要写入的对象 / the object to write
	 * @param buffer 数据包缓冲区 / the packet buffer
	 */
	public void write(T obj,FriendlyByteBuf buffer) {
		CodecUtil.writeCodec(buffer, byInt, obj);
	}
	/**
	 * 根据类型名称获取对应的MapCodec。
	 * <p>
	 * Gets the MapCodec for a given type name.
	 *
	 * @param name 类型名称 / the type name
	 * @return 对应的MapCodec / the corresponding MapCodec
	 */
	public MapCodec<T> getCodec(String name){
		MapCodec<T> selected= codecs.get(name);
//		System.out.println(selected);
		return selected;
	}
	/**
	 * 从数据包缓冲区中读取对象，使用整数ID解码。
	 * <p>
	 * Reads an object from a packet buffer using integer ID decoding.
	 *
	 * @param buffer 数据包缓冲区 / the packet buffer
	 * @return 反序列化的对象 / the deserialized object
	 */
	public T read(FriendlyByteBuf buffer) {
		return CodecUtil.readCodec(buffer, byInt);
	}
	/**
	 * 使用指定的DynamicOps将对象编码为目标格式，使用名称编码。
	 * <p>
	 * Encodes an object to the target format using the given DynamicOps, with name-based encoding.
	 *
	 * @param <A> 目标格式类型 / the target format type
	 * @param op DynamicOps实例 / the DynamicOps instance
	 * @param obj 要编码的对象 / the object to encode
	 * @return 编码后的结果 / the encoded result
	 */
	public <A> A write(DynamicOps<A> op,T obj) {
		return CodecUtil.encodeOrThrow(byName.encodeStart(op, obj));
	}
	/**
	 * 使用指定的DynamicOps从源格式解码对象，使用名称解码。
	 * <p>
	 * Decodes an object from the source format using the given DynamicOps, with name-based decoding.
	 *
	 * @param <A> 源格式类型 / the source format type
	 * @param op DynamicOps实例 / the DynamicOps instance
	 * @param obj 要解码的数据 / the data to decode
	 * @return 解码后的对象 / the decoded object
	 */
	public <A> T read(DynamicOps<A> op,A obj) {
		return CodecUtil.decodeOrThrow(byName.decode(op, obj));
	}
}
