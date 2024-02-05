package com.teammoeg.frostedheart.scenario.runner.target;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public interface IScenarioTarget extends Consumer<ScenarioVM> {
	void apply(IScenarioThread conductor);

	@Override
	default void accept(ScenarioVM t) {
		apply(t);
	}

}
