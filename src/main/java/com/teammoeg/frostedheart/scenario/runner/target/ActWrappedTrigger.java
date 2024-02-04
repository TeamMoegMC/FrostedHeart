package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.scenario.runner.IScenarioTrigger;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class ActWrappedTrigger implements IScenarioTrigger {
	IScenarioTrigger original;
	ActTarget at;
	public ActWrappedTrigger(ActNamespace target, IScenarioTrigger original) {
		super();
		this.original = original;
		this.at = new ActTarget(target,original);
	}

	@Override
	public void apply(IScenarioThread conductor) {
		at.apply(conductor);
	}

	@Override
	public boolean test(ScenarioVM t) {
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
