package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class ParagraphNode implements Node{
	int nodeNum;
	public ParagraphNode(String command, Map<String, String> params) {
		super();
	}
	@Override
	public String getText() {
		return "@p";
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
