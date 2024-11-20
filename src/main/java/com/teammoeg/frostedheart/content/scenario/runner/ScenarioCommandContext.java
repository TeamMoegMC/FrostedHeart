package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;

public record ScenarioCommandContext(ScenarioContext context,ScenarioThread thread) {

	public void callCommand(String command, Map<String, String> params) {
		thread.callCommand(this,command,params);
		
	}
	public double eval(String exp) {
		return thread.eval(context, exp);
	}
	public void jump(ScenarioTarget target) {
		thread.jump(context, target);
	}
}
