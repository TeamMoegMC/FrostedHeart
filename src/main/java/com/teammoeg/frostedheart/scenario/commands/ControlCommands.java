package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class ControlCommands {
	public void wt(ScenarioRunner runner,@Param("t")int t) {
		runner.addWait(t);
		if(runner.shouldWaitClient())
			runner.waitClient();
		runner.sendNoreline();

	}
}
