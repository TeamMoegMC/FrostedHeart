package com.teammoeg.frostedheart.scenario.parser.reader;

public interface CodeLineSource {
	public boolean hasNext();
	public String read();
	public String getName();
}
