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
public class ClassInfo implements Marshaller{
	static final Map<Class<?>,ClassInfo> clss=new HashMap<>();
	public ClassInfo superClass;
	public List<FieldInfo> infos=new ArrayList<>();
	public Class<?> cls;
	public Supplier<Object> factory;
	public static ClassInfo valueOf(Class<?> cls) {
		if(cls==Object.class)return null;
		ClassInfo ret= clss.computeIfAbsent(cls, ClassInfo::new);
		ret.init();
		return ret;
	}
	public static final UnsafeAllocator unsafe=UnsafeAllocator.INSTANCE;
	public static <T> T createInstance(Class<T> clazz){
		try {
			return unsafe.newInstance(clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
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
	@Override
	public Tag toNBT(Object o) {
		CompoundTag cnbt=new CompoundTag();
		saveNBT(o,cnbt);
		return cnbt;
	}
	public void saveNBT(Object o,CompoundTag cnbt) {
		if(superClass!=null) {
			superClass.saveNBT(o, cnbt);
		}
		for(FieldInfo fi:infos) {
			fi.save(cnbt, o);
		}
	}
	
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
	public void loadNBT(Object o,CompoundTag nbt) {
		if(superClass!=null)
			superClass.loadNBT(o, nbt);
		for(FieldInfo fi:infos) {
			fi.load(nbt, o);
		}
	}
}
