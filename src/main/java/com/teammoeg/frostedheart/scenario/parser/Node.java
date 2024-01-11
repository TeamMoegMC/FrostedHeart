package com.teammoeg.frostedheart.scenario.parser;

import com.teammoeg.frostedheart.scenario.ScenarioRunner;

public interface Node {
	void run(ScenarioRunner runner);
	String getText();
	boolean isLiteral();
}
