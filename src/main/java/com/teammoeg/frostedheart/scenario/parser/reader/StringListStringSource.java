package com.teammoeg.frostedheart.scenario.parser.reader;

import java.util.List;

public class StringListStringSource implements CodeLineSource {
	int idx=0;
	String name;
	List<String> strs;
	public StringListStringSource(String name, List<String> strs) {
		super();
		this.name = name;
		this.strs = strs;
	}
	
	@Override
	public boolean hasNext() {
		return idx<strs.size();
	}

	@Override
	public String read() {
		return strs.get(idx++);
	}

	@Override
	public String getName() {
		return name;
	}

}
