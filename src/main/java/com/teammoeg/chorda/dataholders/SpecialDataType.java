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

package com.teammoeg.chorda.dataholders;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.io.CodecUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * 特殊数据的类型注册表，包含编解码器和工厂方法支持。
 * 每个 SpecialDataType 实例代表一种特定类型的数据组件，并在全局类型注册表中自动注册。
 * 提供数据的创建、序列化/反序列化以及按类型获取等功能。
 * <p>
 * Type registry for special data with codec and factory support.
 * Each SpecialDataType instance represents a specific type of data component and is automatically registered in the global type registry.
 * Provides data creation, serialization/deserialization, and type-based retrieval capabilities.
 *
 * @param <T> 数据组件的数据类型 / the data component data type
 */
@ToString
public class SpecialDataType<T extends SpecialData>{
	static final Set<SpecialDataType<?>> TYPE_REGISTRY=new HashSet<>();
	@Getter
	private String id;
	@ToString.Exclude
	private Function<SpecialDataHolder,T> factory;
	@Getter
	private Codec<T> codec;
	
	final int numId;
	private static int nid=0;
	/**
	 * 根据 ID 从全局注册表中获取特殊数据类型。
	 * <p>
	 * Get a special data type from the global registry by its ID.
	 *
	 * @param id 数据类型的唯一标识符 / the unique identifier of the data type
	 * @return 对应的数据类型，如果未找到则返回 null / the corresponding data type, or null if not found
	 */
	public static SpecialDataType<?> getType(String id){
		for(SpecialDataType<?> data:TYPE_REGISTRY) {
			if(data.getId().equals(id))
				return data;
		}
		return null;
	}
	/**
	 * 实例化并注册一个新的特殊数据类型。
	 * <p>
	 * Instantiates and registers a new special data type.
	 *
	 * @param id 数据类型的唯一标识符 / the unique data type identifier
	 * @param factory 数据对象工厂，接收当前数据持有者来创建数据对象 / the data object factory, receives the current data holder to create the data object
	 * @param codec 用于序列化和网络传输的编解码器 / codec for serialization and networking
	 */
	public  SpecialDataType(String id, Function<SpecialDataHolder, T> factory, Codec<T> codec) {
		super();
		this.id = id;
		this.factory = factory;
		this.codec=codec;
		synchronized(TYPE_REGISTRY) {
			TYPE_REGISTRY.add(this);
			numId=nid++;
		}
	}
	
	/**
	 * 创建一个数据组件。
	 * <p>
	 * Creates a data component.
	 *
	 * @param data 数据持有者 / the data holder
	 * @return 创建的数据组件 / the created data component
	 */
	public <U extends SpecialDataHolder> T create(U data) {
		return factory.apply(data);
	}
	
	/**
	 * 以原始类型创建数据组件，不进行类型检查。
	 * <p>
	 * Creates a data component with raw type and no type check.
	 *
	 * @param data 数据持有者 / the data holder
	 * @return 创建的数据组件 / the created data component
	 */
	public <T extends SpecialDataHolder> SpecialData createRaw(T data) {
		return factory.apply(data);
	}
	/**
	 * 使用指定的动态操作从序列化数据中加载数据组件。
	 * <p>
	 * Loads a data component from serialized data using the specified dynamic ops.
	 *
	 * @param <U> 序列化格式类型 / the serialization format type
	 * @param ops 动态操作实例 / the dynamic ops instance
	 * @param data 要反序列化的原始数据 / the raw data to deserialize
	 * @return 反序列化后的数据组件 / the deserialized data component
	 * @throws Exception 如果反序列化失败 / if deserialization fails
	 */
	public <U> T loadData(DynamicOps<U> ops,U data) throws Exception {
		try {
			return CodecUtil.decodeOrThrow(codec.decode(ops, data));
		} catch (Exception e) {
			Chorda.LOGGER.error("Error loading data for SpecialDataType " + this);
			Chorda.LOGGER.error("Data: " + data);
			e.printStackTrace();
			// throw
			throw new Exception("Error loading data for SpecialDataType " + this, e);
		}
    }
	/**
	 * 使用指定的动态操作将数据组件序列化为指定格式。
	 * <p>
	 * Serializes a data component to the specified format using the given dynamic ops.
	 *
	 * @param <U> 序列化格式类型 / the serialization format type
	 * @param ops 动态操作实例 / the dynamic ops instance
	 * @param data 要序列化的数据组件 / the data component to serialize
	 * @return 序列化后的数据 / the serialized data
	 * @throws Exception 如果序列化失败 / if serialization fails
	 */
	public <U> U saveData(DynamicOps<U> ops,T data) throws Exception {
		try {
			return CodecUtil.encodeOrThrow(codec.encodeStart(ops, data));
		} catch (Exception e) {
			Chorda.LOGGER.error("Error saving data for SpecialDataType " + this);
			Chorda.LOGGER.error("Data: " + data);
			e.printStackTrace();
			// throw
			throw new Exception("Error saving data for SpecialDataType " + this, e);
		}

	}
	
	/**
	 * 获取或创建数据组件。如果数据持有者中不存在该类型的数据，则创建新的。
	 * <p>
	 * Gets or creates a data component. If the data holder does not contain this type of data, a new one is created.
	 *
	 * @param data 数据持有者 / the data holder
	 * @return 数据组件 / the data component
	 */
	public <U extends SpecialDataHolder<U>> T getOrCreate(U data) {
		if(data==null)return null;
		return data.getData(this);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SpecialDataType<?> other = (SpecialDataType<?>) obj;
		return Objects.equals(id, other.id);
	}
}
