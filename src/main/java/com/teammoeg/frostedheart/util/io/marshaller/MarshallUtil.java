package com.teammoeg.frostedheart.util.io.marshaller;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.CubicHeatArea;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.codec.NBTCodec;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.math.BlockPos;

public class MarshallUtil {

    private static final Map<Class<?>,Marshaller> marshallers=new HashMap<>();
    private static final Map<Class<?>,Codec<?>> codecs=new HashMap<>();
    private static boolean isBasicInitialized=false;
    public static <R extends INBT,T> void basicMarshaller(Class<T> val,Class<R> cls,Function<R, T> from, Function<T, R> to, T def){
    	marshallers.put(val,new BasicMarshaller<>(cls,from,to,def));
    }
    public static <R extends INBT,T> void basicMarshaller(Class<T> val,Class<R> cls,Function<R, T> from, Function<T, R> to){
    	basicMarshaller(val, cls, from, to, null);
    }
    public static <T> void nbtMarshaller(Class<T> val,Function<CompoundNBT, T> from, Function<T, CompoundNBT> to){
    	marshallers.put(val, new NBTRWMarshaller<>(from,to));
    }
    public static <T> void nbtInstanceMarshaller(Class<T> val,BiConsumer<T, CompoundNBT> from, Function<T, CompoundNBT> to){
    	marshallers.put(val, new NBTInstanceMarshaller<>(val,from,to));
    }
    public static <T> void addCodec(Class<T> clazz,Codec<T> codec) {
    	codecs.put(clazz, codec);
    }
    public static void initializeMarshallers() {
    	if(isBasicInitialized)return;
    		isBasicInitialized=true;
    		basicMarshaller(byte.class, ByteNBT.class,ByteNBT::getByte,ByteNBT::valueOf,(byte)0);
    		basicMarshaller(Byte.class, ByteNBT.class,ByteNBT::getByte,ByteNBT::valueOf);
    		addCodec(byte.class, Codec.BYTE);
    		addCodec(Byte.class, Codec.BYTE);
	    	
	    	basicMarshaller(double.class,DoubleNBT.class,DoubleNBT::getDouble,DoubleNBT::valueOf,0d);
	    	basicMarshaller(Double.class,DoubleNBT.class,DoubleNBT::getDouble,DoubleNBT::valueOf);
    		addCodec(double.class, Codec.DOUBLE);
    		addCodec(Double.class, Codec.DOUBLE);
	    	
	    	basicMarshaller(float.class, FloatNBT.class,FloatNBT::getFloat,FloatNBT::valueOf,0f);
	    	basicMarshaller(Float.class, FloatNBT.class,FloatNBT::getFloat,FloatNBT::valueOf);
    		addCodec(float.class, Codec.FLOAT);
    		addCodec(Float.class, Codec.FLOAT);
	    	
	    	basicMarshaller(int.class, IntNBT.class,IntNBT::getInt,IntNBT::valueOf,0);
	    	basicMarshaller(Integer.class, IntNBT.class,IntNBT::getInt,IntNBT::valueOf);
    		addCodec(int.class, Codec.INT);
    		addCodec(Integer.class, Codec.INT);
	    	
	    	basicMarshaller(long.class, LongNBT.class,LongNBT::getLong,LongNBT::valueOf, 0L);
	    	basicMarshaller(Long.class, LongNBT.class,LongNBT::getLong,LongNBT::valueOf);
	    	addCodec(long.class, Codec.LONG);
    		addCodec(Long.class, Codec.LONG);
	    	
	       	basicMarshaller(short.class, ShortNBT.class,ShortNBT::getShort,ShortNBT::valueOf,(short)0);
	    	basicMarshaller(Short.class, ShortNBT.class,ShortNBT::getShort,ShortNBT::valueOf);
	    	addCodec(short.class, Codec.SHORT);
    		addCodec(Short.class, Codec.SHORT);
	    	
	       	basicMarshaller(String.class, StringNBT.class,StringNBT::getString,StringNBT::valueOf);
	       	addCodec(String.class, Codec.STRING);
	    	
	    	basicMarshaller(byte[].class, ByteArrayNBT.class,ByteArrayNBT::getByteArray,ByteArrayNBT::new);
	    	basicMarshaller(int[].class, IntArrayNBT.class,IntArrayNBT::getIntArray,IntArrayNBT::new);
	    	basicMarshaller(long[].class, LongArrayNBT.class,LongArrayNBT::getAsLongArray,LongArrayNBT::new);
	    	basicMarshaller(BlockPos.class,LongNBT.class,o->BlockPos.fromLong(o.getLong()),o->LongNBT.valueOf(o.toLong()));
	    	
	    	nbtMarshaller(ItemStack.class,ItemStack::read,o->o.write(new CompoundNBT()));
	    	
	    	
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
    public static Object deserialize(Class<?> type,INBT nbt) {
    	initializeMarshallers();
    	Marshaller msl=getOrCreate(type);

    	if(msl!=null)
    		return msl.fromNBT(nbt);

    	return null;
    }
    public static INBT serialize(Object data) {
    	initializeMarshallers();
    	if(data==null)return null;
    	Marshaller msl=getOrCreate(data.getClass());
    	if(msl!=null)
    		return msl.toNBT(data);
    	return null;
    	
    }
    public static void main(String[] args) {
    	INBT data=serialize(new CubicHeatArea(new BlockPos(10,20,30),40,50));
    	System.out.println(data);
    	System.out.println(deserialize(CubicHeatArea.class,data));
    }
}
