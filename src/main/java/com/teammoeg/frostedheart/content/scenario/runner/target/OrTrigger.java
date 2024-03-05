package com.teammoeg.frostedheart.content.scenario.runner.target;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioTrigger;

public class OrTrigger implements IScenarioTrigger {
	IScenarioTrigger[] triggers;
	boolean async=true;
	public OrTrigger(IScenarioTrigger...iScenarioTriggers) {
		triggers=iScenarioTriggers;
	}

	@Override
	public boolean test(IScenarioThread t) {
		
		return Arrays.stream(triggers).map(a->a.test(t)).anyMatch(b->b);
	}
	public boolean isAsync() {
		return async;
	}
	@Override
	public boolean canUse() {
		return Arrays.stream(triggers).map(IScenarioTrigger::canUse).anyMatch(b->b);
	}

	public OrTrigger setSync() {
		this.async = false;
		return this;
	}
	@Override
	public boolean use() {
		return Arrays.stream(triggers).map(IScenarioTrigger::use).anyMatch(b->b);
	}


}
