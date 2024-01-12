package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class ElsEndifNode implements Node {
	String command;
	
	public ElsEndifNode(String command, Map<String, String> params) {
		super();
		this.command = command.toLowerCase();
	}
	@Override
	public void run(ScenarioRunner runner) {
	}
	@Override
	public String getText() {
		return "@"+command;
	}
	@Override
	public boolean isLiteral() {
		return false;
	}
	@Override
	public String getDisplay(ScenarioRunner runner) {
		return "";
	}

}
