package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class TextualCommands {
	public void nw(ScenarioConductor runner) {
		runner.prepareTextualModification();
		runner.isNowait=true;
	}
	public void enw(ScenarioConductor runner) {
		runner.prepareTextualModification();
		runner.isNowait=false;
	}
	public void r(ScenarioConductor runner) {
		runner.newLine();
	}
}
