package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;

public class ExecuteStackElement extends ScenarioTarget{
	private final int nodeNum;
	ExecuteStackElement(String name, int nodeNum) {
		super(name);
		this.nodeNum = nodeNum;
	}
	ExecuteStackElement(Scenario sc, int nodeNum) {
		super(sc);
		this.nodeNum = nodeNum;
	}

	public int getNodeNum() {
		return nodeNum;
	}

}