package com.teammoeg.frostedheart.content.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.ScenarioUtils;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;

public class CallNode implements Node {
	String storage;
	String label;
	String name;
	public CallNode(String command,Map<String,String> params) {
		storage=ScenarioUtils.getOfAlias(params,"s","storage");
		label=ScenarioUtils.getOfAlias(params,"l","label");
		name=ScenarioUtils.getOfAlias(params,"n","name");
				
	}
	@Override
	public String getLiteral(ScenarioCommandContext scenarioVM) {
		return "";
	}

	@Override
	public String getText() {
		return "@Call s="+storage+" l="+label+" n="+name;
	}

	@Override
	public boolean isLiteral() {
		return false;
	}

	@Override
	public void run(ScenarioCommandContext scenarioVM) {
		scenarioVM.thread().addCallStack(name==null?null:new ExecuteTarget(scenarioVM.thread().getScenario().name(),name));
		scenarioVM.thread().jump(scenarioVM.context(),storage, label);
		
	}

}
