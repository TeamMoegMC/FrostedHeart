package com.teammoeg.frostedheart.content.scenario.parser.reader;

public interface CodeLineSource {
	public boolean hasNext();
	public String read();
	public String getName();
}
