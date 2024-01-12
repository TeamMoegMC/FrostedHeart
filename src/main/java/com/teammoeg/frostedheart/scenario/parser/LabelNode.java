package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class LabelNode implements Node{
	String name;
	public LabelNode(String command, Map<String, String> params) {
		super();
		name=params.get("name");
	}
	@Override
	public String getText() {
		return "@label name=\""+name+"\"";
	}

	@Override
	public String getDisplay(ScenarioRunner runner) {
		return "";
	}

	@Override
	public boolean isLiteral() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void run(ScenarioRunner runner) {
	}
}
