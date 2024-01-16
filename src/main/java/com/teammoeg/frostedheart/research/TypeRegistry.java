package com.teammoeg.frostedheart.research;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

public class TypeRegistry<T> {

	protected Map<Class<? extends T>, Pair<Integer, String>> typeInfo = new HashMap<>();

	public TypeRegistry() {
		super();
	}

	public int idOf(T obj) {
	    Pair<Integer, String> info = typeInfo.get(obj.getClass());
	    if (info == null)
	        return -1;
	    return info.getFirst();
	}
	public String typeOf(Class<?> cls) {
		return typeInfo.get(cls).getSecond();
	}
	public void register(Class<? extends T> cls, String type) {
	    int id = typeInfo.size();
	    
	    typeInfo.put(cls, Pair.of(id, type));
	}

}