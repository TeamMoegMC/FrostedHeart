package com.teammoeg.frostedheart.scenario.runner.target;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public abstract class ScenarioTarget implements IScenarioTarget{
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
	protected Scenario getScenario() {
		if(sp==null)
			sp=FHScenario.loadScenario(name);
		return sp;
	}
	protected String getName() {
		return name;
	}
	@Override
	public void accept(ScenarioConductor runner) {
		if(!getScenario().equals(runner.getScenario())) {
			runner.setScenario(getScenario());
			runner.gotoNode(0);
		}
	}
}