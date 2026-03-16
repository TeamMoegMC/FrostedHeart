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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.SerializeName;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * 字段元信息。封装反射 Field 对象，处理序列化名称解析、Codec 创建以及字段级别的 NBT 读写操作。
 * <p>
 * Field metadata information. Wraps a reflection Field object, handles serialization name resolution,
 * Codec creation, and field-level NBT read/write operations.
 */
public class FieldInfo {
	public Field field;
	public String name;
	public Codec<?> codecCache;
	/**
	 * 根据反射 Field 构造字段信息，自动解析 {@link SerializeName} 注解。
	 * <p>
	 * Constructs field info from a reflection Field, automatically resolving the {@link SerializeName} annotation.
	 *
	 * @param f 反射字段对象 / The reflection Field object
	 */
	public FieldInfo(Field f) {
		field=f;
		SerializeName anno=f.getAnnotation(SerializeName.class);
		if(anno!=null)
			name=anno.value();
		else
			name=f.getName();
	}
	/**
	 * 获取此字段的 Codec，使用懒加载缓存。
	 * <p>
	 * Gets the Codec for this field, using lazy-initialized caching.
	 *
	 * @return 字段对应的 Codec / The Codec for this field
	 */
	public Codec<?> getCodec(){
		if(codecCache==null)
			codecCache=createCodec();
		return codecCache;
	}
	private Codec<?> createCodec(){
		Type type= field.getGenericType();
		if(List.class.isAssignableFrom(field.getType())) {
			Type[] types=((ParameterizedType)type).getActualTypeArguments();
			return Codec.list(MarshallUtil.getOrCreateCodec((Class<?>)types[0]));
		}else if(Map.class.isAssignableFrom(field.getType())) {
			Type[] types=((ParameterizedType)type).getActualTypeArguments();
			return Codec.unboundedMap(MarshallUtil.getOrCreateCodec((Class<?>)types[0]), MarshallUtil.getOrCreateCodec((Class<?>)types[1]));
		}
		return MarshallUtil.getOrCreateCodec(field.getType());
	}
	/**
	 * 从 CompoundTag 中加载此字段的值到目标对象。
	 * <p>
	 * Loads this field's value from a CompoundTag into the target object.
	 *
	 * @param data 数据源 CompoundTag / The source CompoundTag
	 * @param o 目标对象 / The target object
	 */
	public void load(CompoundTag data,Object o) {
		if(data.contains(name)) {
			Tag nbt=data.get(name);
			try {
				field.set(o,MarshallUtil.deserialize(field.getType(), nbt));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * 将目标对象中此字段的值保存到 CompoundTag。
	 * <p>
	 * Saves this field's value from the target object into a CompoundTag.
	 *
	 * @param data 目标 CompoundTag / The target CompoundTag
	 * @param o 源对象 / The source object
	 */
	public void save(CompoundTag data,Object o) {
		try {
			Object f=field.get(o);
			Tag nbt=MarshallUtil.serialize(f);
			if(nbt!=null)
			data.put(name,nbt);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
