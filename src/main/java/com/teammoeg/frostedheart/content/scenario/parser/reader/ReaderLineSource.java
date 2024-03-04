package com.teammoeg.frostedheart.content.scenario.parser.reader;

import java.io.IOException;
import java.io.Reader;

public class ReaderLineSource extends StreamLineSource {
	Reader reader;

	public ReaderLineSource(String name, Reader reader) {
		super(name);
		this.reader = reader;
	}

	@Override
	public int readCh() {
		try {
			return reader.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

}
