package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.function.Predicate;

public interface IScenarioTrigger extends Predicate<ScenarioContext> {
	boolean use();

	default boolean canUse() {return true;}

    boolean isAsync();
}
