package com.teammoeg.chorda.client.cui.theme;

public abstract class WrappedColoring {
	final Coloring wrapped;

	public WrappedColoring(Coloring wrapped) {
		super();
		this.wrapped = wrapped;
	}
}