package com.teammoeg.frostedheart.content.scenario.parser.reader;

public interface CodeLineSource {
	boolean hasNext();
	String read();
	String getName();
}
