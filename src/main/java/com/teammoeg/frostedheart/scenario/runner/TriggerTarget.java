package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

public class TriggerTarget extends ExecuteTarget{
	IScenarioTrigger trigger;
	int maxtrigger;
	public TriggerTarget(String name, String label, IScenarioTrigger trigger,int maxtrigger) {
		super(name, label);
		this.trigger = trigger;
		this.maxtrigger=maxtrigger;
	}

	public TriggerTarget(Scenario sc, String label, IScenarioTrigger trigger,int maxtrigger) {
		super(sc, label);
		this.trigger = trigger;
		this.maxtrigger=maxtrigger;
	}

	public boolean doTrigger(ScenarioConductor t) {
		return trigger.test(t);
	}
	
	

}
