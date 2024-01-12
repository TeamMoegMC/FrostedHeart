package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class TextualCommands {
	public void nw(ScenarioRunner runner) {
		runner.sendNoreline();
		runner.isNowait=true;
	}
	public void enw(ScenarioRunner runner) {
		runner.sendNoreline();
		runner.isNowait=false;
	}
	public void r(ScenarioRunner runner) {
		runner.sendNormal();
	}
}
