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

public class MarshallUtil {

    private static final Map<Class<?>,Marshaller> marshallers=new HashMap<>();
    private static final Map<Class<?>,Codec<?>> codecs=new HashMap<>();
    private static boolean isBasicInitialized=false;
    public static <R extends Tag,T> void basicMarshaller(Class<T> val,Class<R> cls,Function<R, T> from, Function<T, R> to, T def){
    	marshallers.put(val,new BasicMarshaller<>(cls,from,to,def));
    }
    public static <R extends Tag,T> void basicMarshaller(Class<T> val,Class<R> cls,Function<R, T> from, Function<T, R> to){
    	basicMarshaller(val, cls, from, to, null);
    }
    public static <T> void nbtMarshaller(Class<T> val,Function<CompoundTag, T> from, Function<T, CompoundTag> to){
    	marshallers.put(val, new NBTRWMarshaller<>(from,to));
    }
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
