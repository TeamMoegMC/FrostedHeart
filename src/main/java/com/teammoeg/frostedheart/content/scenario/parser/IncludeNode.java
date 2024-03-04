package com.teammoeg.frostedheart.content.scenario.parser;

import java.util.Map;

public class IncludeNode extends NopNode {
	String s;
	public IncludeNode(String command, Map<String, String> params) {
		super(command, params);
		s=params.get("s");
	}

}
