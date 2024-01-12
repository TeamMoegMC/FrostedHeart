package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class AssignNode implements Node {
	String exp;
	String str;
	String pat;
	String pat2;
	public AssignNode(String command, Map<String, String> params) {
		super();
		exp=params.getOrDefault("exp","0");
		pat=params.get("var");
		str=params.get("str");
		pat2=params.get("var2");
	}
	@Override
	public void run(ScenarioRunner runner) {
		if(pat2!=null) {
			runner.setPath(pat, runner.evalPath(pat2));
		}else if(str!=null) {
			runner.setPathString(pat, str);
		}else {
			runner.setPathNumber(pat, runner.eval(exp));
		}
	}
	@Override
	public String getText() {
		return "@eval exp=\""+exp.replaceAll("\"", "\\\"")+"\" str=\""+str.replaceAll("\"", "\\\"")+"\"";
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
