package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.math.Colors;

import net.minecraft.util.FastColor;
import net.minecraft.util.FastColor.ARGB32;

@FunctionalInterface
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
	public static Coloring argb(float alpha,float red,float green,float blue) {
		return argb(((int)(alpha*255))&0xFF, ((int)(red*255))&0xFF, ((int)(green*255))&0xFF, ((int)(blue*255))&0xFF);
	}
	public static Coloring argb(int alpha,int red,int green,int blue) {
		return new ConstantColoring(FastColor.ARGB32.color(alpha, red, green, blue));
	}
	public static Coloring of(int color) {
		return argb(color);
	}
	public static Coloring argb(int color) {
		return new ConstantColoring(color);
	}
	public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint);
}
