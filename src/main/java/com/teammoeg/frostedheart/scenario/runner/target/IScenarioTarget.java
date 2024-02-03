package com.teammoeg.frostedheart.scenario.runner.target;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.runner.IScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public interface IScenarioTarget extends Consumer<ScenarioVM> {
	void apply(IScenarioConductor conductor);

	@Override
	default void accept(ScenarioVM t) {
		apply(t);
	}

}
