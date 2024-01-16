package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class TextualCommands {
	public void nowait(ScenarioConductor runner) {
		runner.sendCachedSence();
		runner.getScene().isNowait=true;
	}
	public void endnowait(ScenarioConductor runner) {
		runner.sendCachedSence();
		runner.getScene().isNowait=false;
	}
	public void r(ScenarioConductor runner) {
		runner.newLine();
	}
}
