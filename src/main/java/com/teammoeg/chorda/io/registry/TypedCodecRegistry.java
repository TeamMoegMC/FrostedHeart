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

public class TypedCodecRegistry<T> extends TypeRegistry<T> {
	Map<String,MapCodec<T>> codecs=new HashMap<>();
	List<MapCodec<T>> codecList=new ArrayList<>();
	List<Codec<T>> codecCodecList=new ArrayList<>();
	Codec<T> byName=new KeyMapCodec<T,String>(Codec.STRING,o->this.typeOf(o.getClass()),this::getCodec);
	Codec<T> byInt=Codec.INT.dispatch(this::idOf,codecCodecList::get);
	public synchronized <A extends T>  void register(Class<A> cls, String type,MapCodec<A> codec) {
		codecs.put(type, (MapCodec<T>) codec);
		codecList.add((MapCodec<T>) codec);
		codecCodecList.add((Codec<T>)codec.codec());
		super.register(cls, type);

	}
	public Codec<T> byNameCodec(){
		return byName;
	}
	public Codec<T> codec(){
		return new CompressDifferCodec<>(byName,byInt);
	}
	public Codec<T> byIntCodec(){
		return byInt;
	}
	public void write(T obj,FriendlyByteBuf buffer) {
		CodecUtil.writeCodec(buffer, byInt, obj);
	}
	public MapCodec<T> getCodec(String name){
		MapCodec<T> selected= codecs.get(name);
//		System.out.println(selected);
		return selected;
	}
	public T read(FriendlyByteBuf buffer) {
		return CodecUtil.readCodec(buffer, byInt);
	}
	public <A> A write(DynamicOps<A> op,T obj) {
		return CodecUtil.encodeOrThrow(byName.encodeStart(op, obj));
	}
	public <A> T read(DynamicOps<A> op,A obj) {
		return CodecUtil.decodeOrThrow(byName.decode(op, obj));
	}
}
