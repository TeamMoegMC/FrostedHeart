package com.teammoeg.frostedheart.content.scenario.runner.target;

import com.teammoeg.frostedheart.content.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioTrigger;

public class TriggerTarget implements IScenarioTrigger,IScenarioTarget {
	IScenarioTrigger original;
	IScenarioTarget target;
	public TriggerTarget(IScenarioTrigger trigger, IScenarioTarget target) {
		super();
		this.original = trigger;
		this.target = target;
	}

	@Override
	public void apply(IScenarioThread conductor) {
		target.apply(conductor);
	}

	@Override
	public boolean test(IScenarioThread t) {
		return original.test(t);
	}

	@Override
	public boolean use() {
		return original.use();
	}

	@Override
	public boolean canUse() {
		return original.canUse();
	}

	@Override
	public boolean isAsync() {
		return original.isAsync();
	}

}
