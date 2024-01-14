package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

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
	@Override
	public void accept(ScenarioConductor runner) {

		if(label!=null) {
			Integer ps=getScenario().labels.get(label);
			if(ps!=null) {
				runner.gotoNode(ps);
			}
		}
	}
	
}