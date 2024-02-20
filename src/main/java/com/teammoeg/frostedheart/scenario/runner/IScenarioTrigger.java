package com.teammoeg.frostedheart.scenario.runner;

import java.util.function.Predicate;

public interface IScenarioTrigger extends Predicate<ScenarioVM> {
	boolean use();

	default boolean canUse() {return true;};
	boolean isAsync();
}
