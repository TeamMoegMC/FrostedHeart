package com.teammoeg.frostedheart.content.scenario.parser.providers;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.parser.Scenario;

public interface ScenarioProvider{
	String getName();
	Scenario apply(String path,Map<String,String> params,String name);
}
