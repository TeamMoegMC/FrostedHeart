package com.teammoeg.frostedheart.util.io.codec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;

public class DispatchCodecUtils {
	private DispatchCodecUtils() {}
	public static class DispatchNameCodecBuilder<A>{
		Map<Class<? extends A>,String> classes=new LinkedHashMap<>();
		Map<String,Codec<? extends A>> codecs=new LinkedHashMap<>();
		public <T extends A> DispatchNameCodecBuilder<A> type(String type,Class<T> clazz,Codec<T> codec){
			classes.put(clazz, type);
			codecs.put(type, codec);
			return this;
		}
		public <T extends A> DispatchNameCodecBuilder<A> type(Class<T> clazz,Codec<T> codec){
			String type="n"+classes.size();
			classes.put(clazz, type);
			codecs.put(type, codec);
			return this;
		}
		public Codec<A> buildByName(){
			return Codec.STRING.dispatch(o->ImmutableMap.copyOf(classes).get(o.getClass()), ImmutableMap.copyOf(codecs)::get);
		}
		public Codec<A> buildByInt(){
			List<Class<? extends A>> classes=new ArrayList<>();
			List<Codec<? extends A>> codecs=new ArrayList<>();
			for(Entry<Class<? extends A>, String> name:this.classes.entrySet()) {
				classes.add(name.getKey());
				codecs.add(this.codecs.get(name.getValue()));
			}
			return Codec.INT.dispatch(o->ImmutableList.copyOf(classes).indexOf(o.getClass()), ImmutableList.copyOf(codecs)::get);
		}
		public Codec<A> build(){
			return new CompressDifferCodec<>(buildByName(),buildByInt());
		}
	}
	public static <A> DispatchNameCodecBuilder<A> create(){
		return new DispatchNameCodecBuilder<A>();
	}
	public static <A> DispatchNameCodecBuilder<A> create(Class<A> clazz){
		return new DispatchNameCodecBuilder<A>();
	}
}
