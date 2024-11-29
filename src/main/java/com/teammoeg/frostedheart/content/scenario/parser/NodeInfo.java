package com.teammoeg.frostedheart.content.scenario.parser;

import com.teammoeg.frostedheart.content.scenario.parser.reader.StringParseReader.ParserState;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public record NodeInfo(Node node,ParserState state) implements Node{

	@Override
	public String getLiteral(ScenarioCommandContext scenarioVM) {
		try {
			return node.getLiteral(scenarioVM);
		}catch(Throwable t) {
			throw state.generateException(t);
		}
	}

	@Override
	public String getText() {
		return node.getText();
	}

	@Override
	public boolean isLiteral() {
		return node.isLiteral();
	}

	@Override
	public void run(ScenarioCommandContext scenarioVM) {
		try {
			node.run(scenarioVM);
		}catch(Throwable t) {
			throw state.generateException(t);
		}
	}
	
	
}