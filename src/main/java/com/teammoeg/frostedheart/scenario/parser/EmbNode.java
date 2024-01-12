package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class EmbNode implements Node {
	String exp;
	String pat;
	String format;
	public EmbNode(String command, Map<String, String> params) {
		super();
		exp=params.get("exp");
		pat=params.get("var");
		format=params.getOrDefault("format", "%s");
	}
	@Override
	public void run(ScenarioRunner runner) {

	}
	@Override
	public String getText() {
		return "@emb exp=\""+exp.replaceAll("\"", "\\\"")+"\"";
	}
	@Override
	public boolean isLiteral() {
		return false;
	}
	@Override
	public String getDisplay(ScenarioRunner runner) {
		Object dat="";
		if(exp!=null)
			dat=runner.eval(exp);
		else if(pat!=null)
			dat=runner.evalPathString(pat);
		return String.format(format, dat);
	}

}
