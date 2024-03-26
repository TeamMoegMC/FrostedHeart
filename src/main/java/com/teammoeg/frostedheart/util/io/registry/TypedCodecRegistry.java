package com.teammoeg.frostedheart.util.io.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.codec.CompressDifferCodec;

import net.minecraft.network.PacketBuffer;

public class TypedCodecRegistry<T> extends TypeRegistry<T> {
	Map<String,Codec<? extends T>> codecs=new HashMap<>();
	List<Codec<? extends T>> codecList=new ArrayList<>();
	Codec<T> byName=Codec.STRING.dispatch(o->this.typeOf(o.getClass()),this.codecs::get);
	Codec<T> byInt=Codec.INT.dispatch(this::idOf,codecList::get);
	public <A extends T> void register(Class<A> cls, String type,Codec<A> codec) {
		codecs.put(type, codec);
		codecList.add(codec);
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
	public void write(T obj,PacketBuffer buffer) {
		CodecUtil.writeCodec(buffer, byInt, obj);
	}
	public T read(PacketBuffer buffer) {
		return CodecUtil.readCodec(buffer, byInt);
	}
	public <A> A write(DynamicOps<A> op,T obj) {
		return CodecUtil.encodeOrThrow(byName.encodeStart(op, obj));
	}
	public <A> T read(DynamicOps<A> op,A obj) {
		return CodecUtil.decodeOrThrow(byName.decode(op, obj));
	}
}
