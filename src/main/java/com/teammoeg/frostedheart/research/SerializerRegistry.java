package com.teammoeg.frostedheart.research;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

public abstract class SerializerRegistry<T, R> {
	protected Map<String, Function<R, T>> from = new HashMap<>();
    protected Map<String, Function<T, R>> to = new HashMap<>();
	public SerializerRegistry() {
		super();
	}

	protected void putSerializer(String type, Function<R, T> s) {
		from.put(type, s);
	};
	protected void putDeserializer(String type, Function<T, R> s) {
		to.put(type, s);
	};
	protected abstract void writeType(Pair<Integer, String> type,R obj);
	protected abstract String readType(R obj);
	public T read(R fromObj) {
		String type=readType(fromObj);
		if(type==null)return null;
		Function<R, T> ffrom=from.get(type);
		if(ffrom==null)return null;
		return ffrom.apply(fromObj);
	}
	public abstract String typeOf(Class<?> cls);
	public R write(T fromObj) {
		if(fromObj==null)return null;
		String type=typeOf(fromObj.getClass());
		if(type==null)return null;
		Function<T, R> ffrom=to.get(type);
		if(ffrom==null)return null;
		return ffrom.apply(fromObj);
	}
	protected void register(Class<? extends T> cls, String type, Function<R, T> json, Function<T, R> obj) {
	    putSerializer(type, json);
	    putDeserializer(type,obj);
	}
}