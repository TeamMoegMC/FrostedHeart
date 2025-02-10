package com.teammoeg.frostedheart.content.scenario.runner.target;

import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

public enum NullTarget implements ScenarioTarget {
	INSTANCE;
	public static final ExecuteStackElement NULL_STACK=new ExecuteStackElement("empty",0);
	@Override
	public PreparedScenarioTarget prepare(ScenarioContext t, Scenario current) {
		return new PreparedScenarioTarget(Scenario.EMPTY, 0);
	}

}
