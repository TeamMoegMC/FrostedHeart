package com.teammoeg.frostedheart.scenario.parser.providers;

import java.util.Map;
import java.util.function.BiFunction;

import com.teammoeg.frostedheart.scenario.parser.Scenario;

public interface ScenarioProvider extends BiFunction<String,Map<String,String>, Scenario>{
	String getName();
}
