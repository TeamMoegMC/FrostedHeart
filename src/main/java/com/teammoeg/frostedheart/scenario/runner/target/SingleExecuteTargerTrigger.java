package com.teammoeg.frostedheart.scenario.runner.target;

import java.util.function.Predicate;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.IScenarioTrigger;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class SingleExecuteTargerTrigger extends ExecuteTarget implements IScenarioTrigger {
	boolean canStillTrigger;
	Predicate<ScenarioConductor> test;
	public SingleExecuteTargerTrigger(String name, String label,Predicate<ScenarioConductor> test) {
		super(name, label);
		this.test=test;
	}
	public SingleExecuteTargerTrigger(Scenario sc, String label,Predicate<ScenarioConductor> test) {
		super(sc, label);
		this.test=test;
	}
	@Override
	public boolean test(ScenarioConductor t) {

		return test.test(t);
	}
	@Override
	public boolean use() {
		if(canStillTrigger) {
			canStillTrigger=false;
			return true;
		}
		return false;
	}
	@Override
	public boolean canUse() {
		return canStillTrigger;
	}

}
