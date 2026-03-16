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

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.io.codec.NBTCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 编组工具类。管理类型到编组器和 Codec 的映射，提供对象与 NBT 之间的序列化/反序列化入口。
 * 支持基本类型、NBT 可序列化类型、数组和列表的自动编组。
 * <p>
 * Marshalling utility class. Manages mappings from types to marshallers and codecs,
 * providing entry points for serialization/deserialization between objects and NBT.
 * Supports automatic marshalling of primitive types, NBT-serializable types, arrays, and lists.
 */
public class MarshallUtil {

    private static final Map<Class<?>,Marshaller> marshallers=new HashMap<>();
    private static final Map<Class<?>,Codec<?>> codecs=new HashMap<>();
    private static boolean isBasicInitialized=false;
    /**
     * 注册一个带默认值的基础类型编组器。
     * <p>
     * Registers a basic type marshaller with a default value.
     *
     * @param val Java 类型类 / Java type class
     * @param cls NBT 标签类 / NBT tag class
     * @param from NBT 转对象的函数 / Function to convert from NBT to object
     * @param to 对象转 NBT 的函数 / Function to convert from object to NBT
     * @param def 默认值 / Default value
     * @param <R> NBT 标签类型 / NBT tag type
     * @param <T> Java 对象类型 / Java object type
     */
    public static <R extends Tag,T> void basicMarshaller(Class<T> val,Class<R> cls,Function<R, T> from, Function<T, R> to, T def){
    	marshallers.put(val,new BasicMarshaller<>(cls,from,to,def));
    }
    /**
     * 注册一个无默认值的基础类型编组器。
     * <p>
     * Registers a basic type marshaller without a default value.
     *
     * @param val Java 类型类 / Java type class
     * @param cls NBT 标签类 / NBT tag class
     * @param from NBT 转对象的函数 / Function to convert from NBT to object
     * @param to 对象转 NBT 的函数 / Function to convert from object to NBT
     * @param <R> NBT 标签类型 / NBT tag type
     * @param <T> Java 对象类型 / Java object type
     */
    public static <R extends Tag,T> void basicMarshaller(Class<T> val,Class<R> cls,Function<R, T> from, Function<T, R> to){
    	basicMarshaller(val, cls, from, to, null);
    }
    /**
     * 注册一个基于 CompoundTag 读写函数的 NBT 编组器。
     * <p>
     * Registers an NBT marshaller based on CompoundTag read/write functions.
     *
     * @param val Java 类型类 / Java type class
     * @param from CompoundTag 转对象的函数 / Function to convert from CompoundTag to object
     * @param to 对象转 CompoundTag 的函数 / Function to convert from object to CompoundTag
     * @param <T> Java 对象类型 / Java object type
     */
    public static <T> void nbtMarshaller(Class<T> val,Function<CompoundTag, T> from, Function<T, CompoundTag> to){
    	marshallers.put(val, new NBTRWMarshaller<>(from,to));
    }
    /**
     * 注册一个基于实例的 NBT 编组器，先创建实例再加载数据。
     * <p>
     * Registers an instance-based NBT marshaller that creates an instance first then loads data into it.
     *
     * @param val Java 类型类 / Java type class
     * @param from 将 CompoundTag 数据加载到已有实例的消费者 / Consumer to load CompoundTag data into an existing instance
     * @param to 对象转 CompoundTag 的函数 / Function to convert from object to CompoundTag
     * @param <T> Java 对象类型 / Java object type
     */
    public static <T> void nbtInstanceMarshaller(Class<T> val,BiConsumer<T, CompoundTag> from, Function<T, CompoundTag> to){
    	marshallers.put(val, new NBTInstanceMarshaller<>(val,from,to));
    }
    public static <T> void addCodec(Class<T> clazz,Codec<T> codec) {
    	codecs.put(clazz, codec);
    }
    public static void initializeMarshallers() {
    	if(isBasicInitialized)return;
    		isBasicInitialized=true;
    		basicMarshaller(byte.class, ByteTag.class,ByteTag::getAsByte,ByteTag::valueOf,(byte)0);
    		basicMarshaller(Byte.class, ByteTag.class,ByteTag::getAsByte,ByteTag::valueOf);
    		addCodec(byte.class, Codec.BYTE);
    		addCodec(Byte.class, Codec.BYTE);
	    	
	    	basicMarshaller(double.class,DoubleTag.class,DoubleTag::getAsDouble,DoubleTag::valueOf,0d);
	    	basicMarshaller(Double.class,DoubleTag.class,DoubleTag::getAsDouble,DoubleTag::valueOf);
    		addCodec(double.class, Codec.DOUBLE);
    		addCodec(Double.class, Codec.DOUBLE);
	    	
	    	basicMarshaller(float.class, FloatTag.class,FloatTag::getAsFloat,FloatTag::valueOf,0f);
	    	basicMarshaller(Float.class, FloatTag.class,FloatTag::getAsFloat,FloatTag::valueOf);
    		addCodec(float.class, Codec.FLOAT);
    		addCodec(Float.class, Codec.FLOAT);
	    	
	    	basicMarshaller(int.class, IntTag.class,IntTag::getAsInt,IntTag::valueOf,0);
	    	basicMarshaller(Integer.class, IntTag.class,IntTag::getAsInt,IntTag::valueOf);
    		addCodec(int.class, Codec.INT);
    		addCodec(Integer.class, Codec.INT);
	    	
	    	basicMarshaller(long.class, LongTag.class,LongTag::getAsLong,LongTag::valueOf, 0L);
	    	basicMarshaller(Long.class, LongTag.class,LongTag::getAsLong,LongTag::valueOf);
	    	addCodec(long.class, Codec.LONG);
    		addCodec(Long.class, Codec.LONG);
	    	
	       	basicMarshaller(short.class, ShortTag.class,ShortTag::getAsShort,ShortTag::valueOf,(short)0);
	    	basicMarshaller(Short.class, ShortTag.class,ShortTag::getAsShort,ShortTag::valueOf);
	    	addCodec(short.class, Codec.SHORT);
    		addCodec(Short.class, Codec.SHORT);
	    	
	       	basicMarshaller(String.class, StringTag.class,StringTag::getAsString,StringTag::valueOf);
	       	addCodec(String.class, Codec.STRING);
	    	
	    	basicMarshaller(byte[].class, ByteArrayTag.class,ByteArrayTag::getAsByteArray,ByteArrayTag::new);
	    	basicMarshaller(int[].class, IntArrayTag.class,IntArrayTag::getAsIntArray,IntArrayTag::new);
	    	basicMarshaller(long[].class, LongArrayTag.class,LongArrayTag::getAsLongArray,LongArrayTag::new);
	    	basicMarshaller(BlockPos.class,LongTag.class,o->BlockPos.of(o.getAsLong()),o->LongTag.valueOf(o.asLong()));
	    	
	    	nbtMarshaller(ItemStack.class,ItemStack::of,o->o.save(new CompoundTag()));
	    	
	    	
    }
    private static <T> Codec<T> createCodec(Class<T> type) {
    	//System.out.println(type.getSimpleName());
    	if(NBTSerializable.class.isAssignableFrom(type)) {
    		return new NBTCodec<>((Class)type);
    	}else if(type.isArray()) {
    		return (Codec<T>) CodecUtil.array((Codec<Object>)createCodec(type.getComponentType()), t->(T)Array.newInstance(type.getComponentType(), t));
    	}
    	return new ReflectionCodec<T>(type).codec();
    }
    
    public static <T> Codec<T> getOrCreateCodec(Class<T> type) {
    	return (Codec<T>) codecs.computeIfAbsent(type,MarshallUtil::createCodec);
    }
    private static <T> Marshaller create(Class<T> type) {
    	//System.out.println(type.getSimpleName());
    	if(NBTSerializable.class.isAssignableFrom(type)) {
    		return new NBTInstanceMarshaller<T>(type,(o,t)->((NBTSerializable)o).deserializeNBT(t),o->((NBTSerializable)o).serializeNBT());
    	}else if(List.class.isAssignableFrom(type)) {
    		return new ListMarshaller<>(Object.class,t-> new ListListWrapper<>((List<Object>) t),ListListWrapper::new);
    	}else if(type.isArray()) {
    		return new ListMarshaller<>(type, ArrayListWrapper::new,ArrayListWrapper::new);
    	}
    	return ClassInfo.valueOf(type);
    }
    public static Marshaller getOrCreate(Class<?> type) {
    	return marshallers.computeIfAbsent(type,MarshallUtil::create);
    }
    public static Object deserialize(Class<?> type,Tag nbt) {
    	initializeMarshallers();
    	Marshaller msl=getOrCreate(type);

    	if(msl!=null)
    		return msl.fromNBT(nbt);

    	return null;
    }
    public static Tag serialize(Object data) {
    	initializeMarshallers();
    	if(data==null)return null;
    	Marshaller msl=getOrCreate(data.getClass());
    	if(msl!=null)
    		return msl.toNBT(data);
    	return null;
    	
    }
    public static void main(String[] args) {
//    	Tag data=serialize(new CubicHeatArea(new BlockPos(10,20,30),40,50));
//    	System.out.println(data);
//    	System.out.println(deserialize(CubicHeatArea.class,data));
    }
}
