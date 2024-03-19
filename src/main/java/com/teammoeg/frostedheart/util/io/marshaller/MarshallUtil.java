package com.teammoeg.frostedheart.util.io.marshaller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.content.heatdevice.chunkheatdata.CubicHeatArea;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

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
    private static boolean isBasicInitialized=false;
    public static void initializeMarshallers() {
    	if(isBasicInitialized)return;
    		isBasicInitialized=true;
	    	marshallers.put(byte.class, new BasicMarshaller<>(ByteNBT.class,ByteNBT::getByte,ByteNBT::valueOf,(byte)0));
	    	marshallers.put(Byte.class, new BasicMarshaller<>(ByteNBT.class,ByteNBT::getByte,ByteNBT::valueOf));
	    	
	    	marshallers.put(double.class, new BasicMarshaller<>(DoubleNBT.class,DoubleNBT::getDouble,DoubleNBT::valueOf,0d));
	    	marshallers.put(Double.class, new BasicMarshaller<>(DoubleNBT.class,DoubleNBT::getDouble,DoubleNBT::valueOf));
	    	
	    	marshallers.put(float.class, new BasicMarshaller<>(FloatNBT.class,FloatNBT::getFloat,FloatNBT::valueOf,0f));
	    	marshallers.put(Float.class, new BasicMarshaller<>(FloatNBT.class,FloatNBT::getFloat,FloatNBT::valueOf));
	    	
	    	marshallers.put(int.class, new BasicMarshaller<>(IntNBT.class,IntNBT::getInt,IntNBT::valueOf,0));
	    	marshallers.put(Integer.class, new BasicMarshaller<>(IntNBT.class,IntNBT::getInt,IntNBT::valueOf));
	    	
	    	marshallers.put(long.class, new BasicMarshaller<>(LongNBT.class,LongNBT::getLong,LongNBT::valueOf, 0L));
	    	marshallers.put(Long.class, new BasicMarshaller<>(LongNBT.class,LongNBT::getLong,LongNBT::valueOf));
	    	
	       	marshallers.put(short.class, new BasicMarshaller<>(ShortNBT.class,ShortNBT::getShort,ShortNBT::valueOf,(short)0));
	    	marshallers.put(Short.class, new BasicMarshaller<>(ShortNBT.class,ShortNBT::getShort,ShortNBT::valueOf));
	    	
	       	marshallers.put(String.class, new BasicMarshaller<>(StringNBT.class,StringNBT::getString,StringNBT::valueOf));
	    	
	    	marshallers.put(byte[].class, new BasicMarshaller<>(ByteArrayNBT.class,ByteArrayNBT::getByteArray,ByteArrayNBT::new));
	    	marshallers.put(int[].class, new BasicMarshaller<>(IntArrayNBT.class,IntArrayNBT::getIntArray,IntArrayNBT::new));
	    	marshallers.put(long[].class, new BasicMarshaller<>(LongArrayNBT.class,LongArrayNBT::getAsLongArray,LongArrayNBT::new));
	    	marshallers.put(BlockPos.class, new BasicMarshaller<LongNBT,BlockPos>(LongNBT.class,o->BlockPos.fromLong(o.getLong()),o->LongNBT.valueOf(o.toLong())));
	    	
	    	marshallers.put(ItemStack.class, new NBTRWMarshaller<>(ItemStack.class,ItemStack::read,o->o.write(new CompoundNBT())));
	    	
	    	
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
