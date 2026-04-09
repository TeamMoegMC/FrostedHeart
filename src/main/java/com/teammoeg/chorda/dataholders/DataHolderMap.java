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

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.io.SerializeUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * 基于 Map 的数据持有者实现，使用 {@link IdentityHashMap} 存储数据组件。
 * 提供线程安全的数据访问、NBT 序列化/反序列化功能。
 * 这是团队数据、客户端数据和世界数据持有者的基类。
 * <p>
 * Map-based data holder implementation using {@link IdentityHashMap} to store data components.
 * Provides thread-safe data access and NBT serialization/deserialization.
 * This is the base class for team data, client data, and world data holders.
 *
 * @param <T> 数据持有者的具体子类型 / the concrete subtype of the data holder
 */
public class DataHolderMap<T extends DataHolderMap<T>> implements SpecialDataHolder<T>, NBTSerializable {

	public final Marker marker;
	IdentityHashMap<SpecialDataType,SpecialData> data=new IdentityHashMap<>(SpecialDataType.TYPE_REGISTRY.size());
	ReentrantLock lock=new ReentrantLock();
	/**
	 * 构造一个新的数据持有者映射。
	 * <p>
	 * Constructs a new data holder map.
	 *
	 * @param markerName 用于日志记录的标记名称 / the marker name used for logging
	 */
	public DataHolderMap(String markerName) {
		marker = MarkerManager.getMarker(markerName);
	}

	
	/**
	 * {@inheritDoc}
	 * 将所有数据组件序列化为 NBT 格式。使用锁确保线程安全。
	 * <p>
	 * Serializes all data components to NBT format. Uses locking for thread safety.
	 */
	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		try {
			lock.lock();
			nbt.put("data", SerializeUtil.toNBTMap(data.entrySet(), (t,p)-> {
				try {
					p.put(t.getKey().getId(),(Tag)t.getKey().saveData(NbtOps.INSTANCE, t.getValue()));
				} catch (Exception e) {
					Chorda.LOGGER.error(marker, "Failed to save " + t.getKey(), e);
				}
			}));
		}finally {
			lock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 * 从 NBT 数据中加载所有已注册类型的数据组件。
	 * <p>
	 * Loads all registered types of data components from NBT data.
	 */
	@Override
	public void load(CompoundTag data, boolean isPacket) {
		data = data.getCompound("data");
		for(SpecialDataType<?> tc: SpecialDataType.TYPE_REGISTRY) {
        	if(data.contains(tc.getId())) {
				try {
					SpecialData raw=tc.loadData(NbtOps.INSTANCE, data.get(tc.getId()));
					this.data.put(tc, raw);
				} catch (Exception e) {
					Chorda.LOGGER.error(marker, "Failed to load " + tc, e);
					throw new DataLoadException("Failed to load " + tc,e);
				}
        	}
        }
	}
	
	
	/**
	 * {@inheritDoc}
	 * 获取指定类型的数据组件。如果不存在则创建新的，使用双重检查锁定保证线程安全。
	 * <p>
	 * Gets the data component of the specified type. Creates a new one if not present, using double-checked locking for thread safety.
	 */
	@SuppressWarnings("unchecked")
	public <U extends SpecialData> U getData(SpecialDataType<U> cap){
		U ret= (U) data.get(cap);
		if(ret==null) {
			try {
				lock.lock();
				ret=  (U) data.get(cap);
				if(ret==null) {
					ret=cap.create((T) this);
					data.put(cap, ret);
				}
			}finally {
				lock.unlock();
			}
		}
		return ret;
	}
	/**
	 * 设置指定类型的数据组件。
	 * <p>
	 * Sets the data component for the specified type.
	 *
	 * @param <U> 数据组件的类型 / the data component type
	 * @param cap 数据组件的类型定义 / the data component type definition
	 * @param data 要设置的数据组件 / the data component to set
	 * @return 被设置的数据组件 / the data component that was set
	 */
	public <U extends SpecialData> U setData(SpecialDataType<U> cap, U data){
		this.data.put(cap, data);
		return data;
	}
	/**
	 * 以原始类型获取数据组件，不进行泛型类型检查。如果不存在则创建新的。
	 * <p>
	 * Gets the data component with raw type without generic type checking. Creates a new one if not present.
	 *
	 * @param cap 数据组件的类型定义 / the data component type definition
	 * @return 数据组件实例 / the data component instance
	 */
	public SpecialData getDataRaw(SpecialDataType<?> cap){
		SpecialData ret=  data.get(cap);
		if(ret==null) {
			try {
				lock.lock();
				ret=  data.get(cap);
				if(ret==null) {
					ret=cap.createRaw(this);
					data.put(cap, ret);
				}
			}finally {
				lock.unlock();
			}
		}
		return ret;
	}
	/**
	 * 获取当前持有者中已存储的所有数据类型。
	 * <p>
	 * Gets all data types currently stored in this holder.
	 *
	 * @return 已存储的数据类型集合 / the collection of stored data types
	 */
	public Collection<SpecialDataType> getTypes(){
		return data.keySet();
	}
	/**
	 * {@inheritDoc}
	 * 获取已存在的数据组件，不会自动创建。
	 * <p>
	 * Gets the existing data component without auto-creating it.
	 */
	@Override
	public <U extends SpecialData> Optional<U> getOptional(SpecialDataType<U> cap) {
		
		return Optional.ofNullable((U)data.get(cap));
	}
}
