package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;

public record ConstantColoring(int value) implements Coloring{
	@Override
	public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
		return value;
	}
	
}