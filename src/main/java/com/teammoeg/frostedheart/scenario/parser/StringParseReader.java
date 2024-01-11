package com.teammoeg.frostedheart.scenario.parser;

public class StringParseReader{
	String str;
	int idx=-1;
	int srecord;
	public StringParseReader(String str) {
		super();
		this.str = str;
	}
	boolean hasNext() {
		return idx<str.length()-1;
	}
	char next() {
		return str.charAt(idx++);
	}
	char peek() {
		return str.charAt(idx+1);
	}
	void saveIndex() {
		srecord=idx;
	}
	void loadIndex() {
		idx=srecord;
	}
	String fromStart() {
		return str.substring(srecord, idx);
	}
	char peekLast() {
		if(idx<0)
			return str.charAt(0);
		return str.charAt(idx-1);
	}
	char last() {
		return str.charAt(idx-1);
	}
	void skipWhitespace() {
		while(Character.isWhitespace(str.charAt(idx))&&hasNext()) {
			idx++;
		}
	}
}
