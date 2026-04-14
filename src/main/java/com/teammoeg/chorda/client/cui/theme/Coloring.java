package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.math.Colors;

public interface Coloring {
	public static final Coloring WHITE=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return Colors.WHITE;
		}
	};
	public static final Coloring BLACK=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return Colors.BLACK;
		}
	};
	public static final Coloring TRANSPARENT=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return 0;
		}
	};
	public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint);
}
