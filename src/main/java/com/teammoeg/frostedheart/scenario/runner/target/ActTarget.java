package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class ActTarget implements IScenarioTarget {
	ActNamespace ns;
	IScenarioTarget parent;
	public ActTarget(ActNamespace ns, IScenarioTarget parent) {
		super();
		this.ns = ns;
		this.parent = parent;
	}
	@Override
	public void accept(ScenarioConductor t) {
		t.continueQuest(ns);
		parent.accept(t);
		
	}

}
