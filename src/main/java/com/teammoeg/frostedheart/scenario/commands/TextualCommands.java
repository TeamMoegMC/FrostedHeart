package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class TextualCommands {
	public void nw(ScenarioConductor runner) {
		runner.sendNoreline();
		runner.isNowait=true;
	}
	public void enw(ScenarioConductor runner) {
		runner.sendNoreline();
		runner.isNowait=false;
	}
	public void r(ScenarioConductor runner) {
		runner.sendNormal();
	}
}
