package com.teammoeg.frostedheart.scenario.parser.providers;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;

public abstract class StringScenarioProvider implements ScenarioProvider {

	public StringScenarioProvider() {
	}
	public abstract String get(String t, Map<String, String> u);
	@Override
	public Scenario apply(String t, Map<String, String> u) {
		String ss=get(t,u);
		if(ss==null)return null;
		return FHScenario.parser.parseString(t,ss);
	}

}
