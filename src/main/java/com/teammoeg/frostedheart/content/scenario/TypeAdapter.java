package com.teammoeg.frostedheart.content.scenario;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

@FunctionalInterface
public interface TypeAdapter<T,U> {
	T convert(U runner, String[] parnames, Map<String, String> params);
}
