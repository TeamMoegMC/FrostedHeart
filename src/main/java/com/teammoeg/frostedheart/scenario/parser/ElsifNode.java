package com.teammoeg.frostedheart.scenario.parser;

import java.util.Map;

public class ElsifNode extends ElseNode {
	String exp;
	public ElsifNode(String command, Map<String, String> params) {
		super(command, params);
		exp=params.getOrDefault("exp","1");
	}

}
