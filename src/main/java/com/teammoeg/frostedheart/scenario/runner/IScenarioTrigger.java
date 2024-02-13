package com.teammoeg.frostedheart.scenario.runner;

import java.util.function.Predicate;

import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;

public interface IScenarioTrigger extends IScenarioTarget,Predicate<ScenarioVM> {
	boolean use();

	default boolean canUse() {return true;};
	boolean isAsync();
}
