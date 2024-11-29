package com.teammoeg.frostedheart.content.scenario.runner.trigger;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.scenario.runner.ScenarioThread;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

public class OrTrigger implements IScenarioTrigger {
	IScenarioTrigger[] triggers;
	boolean async=true;
	public OrTrigger(IScenarioTrigger...iScenarioTriggers) {
		triggers=iScenarioTriggers;
	}

	@Override
	public boolean test(ScenarioContext t) {
		
		return Arrays.stream(triggers).anyMatch(a -> a.test(t));
	}
	public boolean isAsync() {
		return async;
	}
	@Override
	public boolean canUse() {
		return Arrays.stream(triggers).allMatch(IScenarioTrigger::canUse);
	}

	public OrTrigger setSync() {
		this.async = false;
		return this;
	}
	@Override
	public boolean use() {
		return Arrays.stream(triggers).anyMatch(IScenarioTrigger::use);
	}


}
