package com.teammoeg.frostedheart.scenario.runner.target;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.runner.IScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public interface IScenarioTarget extends Consumer<ScenarioConductor> {
	void apply(IScenarioConductor conductor);

	@Override
	default void accept(ScenarioConductor t) {
		apply(t);
	}

}
