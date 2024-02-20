package com.teammoeg.frostedheart.scenario;

import java.util.Map;

@FunctionalInterface
public interface TypeAdapter<T,R> {
	public T convert(R runner,String parname,Map<String, String> params);
}
