package com.teammoeg.frostedheart.util.io;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

public abstract class SerializerRegistry<T, C, R> {
	protected Map<String, Function<R, T>> from = new HashMap<>();
    protected Map<String, BiFunction<T, C, R>> to = new HashMap<>();
	public SerializerRegistry() {
		super();
	}

	protected void putSerializer(String type, Function<R, T> s) {
		from.put(type, s);
	}

    protected void putDeserializer(String type, BiFunction<T, C, R> s) {
		to.put(type, s);
	}

    protected abstract void writeType(Pair<Integer, String> type,R obj);
	protected abstract String readType(R obj);
	public T read(R fromObj) {
		String type=readType(fromObj);
		if(type==null)return null;
		Function<R, T> ffrom=from.get(type);
		if(ffrom==null)return null;
		return ffrom.apply(fromObj);
	}
	public abstract Pair<Integer,String> typeOf(Class<?> cls);
	public R write(T fromObj,C context) {
		if(fromObj==null)return null;
		Pair<Integer, String> type=typeOf(fromObj.getClass());
		if(type==null)return null;
		BiFunction<T, C, R> ffrom=to.get(type.getSecond());
		if(ffrom==null)return null;
		R obj= ffrom.apply(fromObj, context);
		writeType(type,obj);
		return obj;
	}
	protected void register(String type, Function<R, T> json, BiFunction<T, C, R> obj) {
	    putSerializer(type, json);
	    putDeserializer(type,obj);
	}
}