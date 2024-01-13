package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.parser.Scenario;

public class TriggerTarget extends ExecuteTarget{
	IScenarioTrigger trigger;

	public TriggerTarget(String name, String label, IScenarioTrigger trigger) {
		super(name, label);
		this.trigger = trigger;
	}

	public TriggerTarget(Scenario sc, String label, IScenarioTrigger trigger) {
		super(sc, label);
		this.trigger = trigger;
	}

	public boolean doTrigger(ScenarioConductor t) {
		return trigger.test(t);
	}
	
	

}
