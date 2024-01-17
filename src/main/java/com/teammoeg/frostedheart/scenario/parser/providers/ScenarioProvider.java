package com.teammoeg.frostedheart.scenario.parser.providers;

import java.util.Map;
import java.util.function.BiFunction;

import com.teammoeg.frostedheart.scenario.parser.Scenario;

public interface ScenarioProvider{
	String getName();
	Scenario apply(String path,Map<String,String> params,String name);
}
