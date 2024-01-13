package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.parser.Scenario;

public class ExecuteTarget extends ScenarioTarget{

	private final String label;
	public ExecuteTarget(String name, String label) {
		super(name);

		this.label = label;
	}
	public ExecuteTarget(Scenario sc, String label) {
		super(sc);

		this.label = label;
	}
	String getLabel() {
		return label;
	}
}