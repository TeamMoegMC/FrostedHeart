package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;

public abstract class ScenarioTarget{
	private final String name;
	private transient Scenario sp;
	public ScenarioTarget(String name) {
		super();
		this.name=name;
	}
	public ScenarioTarget(Scenario sc) {
		super();
		this.sp=sc;
		this.name=sc.name;
	}
	Scenario getScenario() {
		if(sp==null)
			sp=FHScenario.loadScenario(name);
		return sp;
	}
	String getName() {
		return name;
	}
}