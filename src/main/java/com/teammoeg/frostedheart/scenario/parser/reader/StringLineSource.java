package com.teammoeg.frostedheart.scenario.parser.reader;

public class StringLineSource extends StreamLineSource {
	String code;
	int idx;
	public StringLineSource(String name, String code) {
		super(name);
		this.code = code;
	}

	@Override
	public int readCh() {
		if(idx<code.length())
			return code.codePointAt(idx++);
		return -1;
	}

}
