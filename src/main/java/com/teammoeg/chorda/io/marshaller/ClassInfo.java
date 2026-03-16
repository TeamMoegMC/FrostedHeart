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

package com.teammoeg.chorda.io.marshaller;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.gson.internal.UnsafeAllocator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
/**
 * 类元信息。通过反射收集类的非静态、非瞬态字段，并实现 {@link Marshaller} 接口以支持对象与 CompoundTag 之间的序列化和反序列化。
 * 使用缓存避免重复解析同一类。
 * <p>
 * Class metadata information. Collects non-static, non-transient fields of a class via reflection
 * and implements {@link Marshaller} to support serialization/deserialization between objects and CompoundTag.
 * Uses caching to avoid redundant parsing of the same class.
 */
public class ClassInfo implements Marshaller{
	static final Map<Class<?>,ClassInfo> clss=new HashMap<>();
	public ClassInfo superClass;
	public List<FieldInfo> infos=new ArrayList<>();
	public Class<?> cls;
	public Supplier<Object> factory;
	/**
	 * 获取或创建指定类的 ClassInfo 实例。对 Object.class 返回 null。
	 * <p>
	 * Gets or creates a ClassInfo instance for the specified class. Returns null for Object.class.
	 *
	 * @param cls 目标类 / The target class
	 * @return ClassInfo 实例，或 null / ClassInfo instance, or null
	 */
	public static ClassInfo valueOf(Class<?> cls) {
		if(cls==Object.class)return null;
		ClassInfo ret= clss.computeIfAbsent(cls, ClassInfo::new);
		ret.init();
		return ret;
	}
	public static final UnsafeAllocator unsafe=UnsafeAllocator.INSTANCE;
	/**
	 * 使用 UnsafeAllocator 创建指定类的实例，跳过构造函数。
	 * <p>
	 * Creates an instance of the given class using UnsafeAllocator, bypassing constructors.
	 *
	 * @param clazz 要实例化的类 / The class to instantiate
	 * @param <T> 类类型 / Class type
	 * @return 新实例，失败时返回 null / New instance, or null on failure
	 */
	public static <T> T createInstance(Class<T> clazz){
		try {
			return unsafe.newInstance(clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 获取包含父类在内的所有字段信息的流。
	 * <p>
	 * Returns a stream of all field information, including fields from superclasses.
	 *
	 * @return 字段信息流 / Stream of field information
	 */
	public Stream<FieldInfo> getFields(){
		if(superClass==null) {
			return infos.stream();
		}else {
			return Stream.concat(superClass.getFields(), infos.stream());
		}
	}
	boolean inited=false;
	private void init() {
		if(inited)return;
		inited=true;
		factory=()->createInstance(cls);
		superClass=valueOf(cls.getSuperclass());
		for(Field f:cls.getDeclaredFields()) {
			if(!Modifier.isTransient(f.getModifiers())&&!Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				infos.add(new FieldInfo(f));
			}
		}
	}
	private ClassInfo(Class<?> cls) {
		super();
		this.cls = cls;

	}
	/** {@inheritDoc} */
	@Override
	public Tag toNBT(Object o) {
		CompoundTag cnbt=new CompoundTag();
		saveNBT(o,cnbt);
		return cnbt;
	}
	/**
	 * 将对象的所有字段保存到 CompoundTag 中，包括父类字段。
	 * <p>
	 * Saves all fields of an object into a CompoundTag, including superclass fields.
	 *
	 * @param o 要序列化的对象 / The object to serialize
	 * @param cnbt 目标 CompoundTag / The target CompoundTag
	 */
	public void saveNBT(Object o,CompoundTag cnbt) {
		if(superClass!=null) {
			superClass.saveNBT(o, cnbt);
		}
		for(FieldInfo fi:infos) {
			fi.save(cnbt, o);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public Object fromNBT(Tag nbt) {
		if(nbt instanceof CompoundTag) {
			CompoundTag cnbt=(CompoundTag) nbt;
			Object o=factory.get();
			if(o!=null) {
				loadNBT(o,cnbt);
				return o;
			}
		}
		return null;
	}
	/**
	 * 从 CompoundTag 加载数据到已有对象中，包括父类字段。
	 * <p>
	 * Loads data from a CompoundTag into an existing object, including superclass fields.
	 *
	 * @param o 要加载数据的对象 / The object to load data into
	 * @param nbt 数据源 CompoundTag / The source CompoundTag
	 */
	public void loadNBT(Object o,CompoundTag nbt) {
		if(superClass!=null)
			superClass.loadNBT(o, nbt);
		for(FieldInfo fi:infos) {
			fi.load(nbt, o);
		}
	}
}
