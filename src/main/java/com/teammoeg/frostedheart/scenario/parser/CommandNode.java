package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.ScenarioRunner;

public class CommandNode implements Node {
	String command;
	Map<String,String> params;
	
	public CommandNode(String command, Map<String, String> params) {
		super();
		this.command = command;
		this.params = params;
	}
	@Override
	public void run(ScenarioRunner runner) {
		FHScenario.callCommand(command, runner, params);
	}
	@Override
	public String getText() {
		return "@"+command+" "+params.entrySet().stream().map(e->e.getKey()+"=\""+e.getValue().replaceAll("\"","\\\"")+"\"").reduce("",(a,b)->a+b+" ");
	}
	@Override
	public boolean isLiteral() {
		return false;
	}

}
