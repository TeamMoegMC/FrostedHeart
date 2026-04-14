package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;

import net.minecraft.util.FastColor;

public class UIColors {
	public static final Coloring UI_TEXT=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).UITextColor();
		}
	};
	public static final Coloring UI_ALT_TEXT=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).UIAltTextColor();
		}
	};
	public static final Coloring UI_BACKGROUND=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).UIBGColor();
		}
	};
	public static final Coloring UI_BORDER=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).UIBGBorderColor();
		}
	};
	public static final Coloring BUTTON_TEXT=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).buttonTextColor();
		}
	};
	public static final Coloring ERROR_TEXT=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).errorColor();
		}
	};
	public static final Coloring SUCCESS_TEXT=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).successColor();
		}
	};
	public static final Coloring BUTTON_TEXT_OVER=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).buttonTextOverColor();
		}
	};
	public static final Coloring BUTTON_TEXT_DISABLED=new Coloring() {
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return hint.theme(parent).buttonTextDisabledColor();
		}
	};
	public static record ConstantColoring(int value) implements Coloring{
		@Override
		public int getColorARGB(UIElement parent, int xHint, int yHint, RenderingHint hint) {
			return value;
		}
		
	}
	public abstract static class WrappedColoring {
		final Coloring wrapped;

		public WrappedColoring(Coloring wrapped) {
			super();
			this.wrapped = wrapped;
		}
	}
	public static Coloring argb(int color) {
		return new ConstantColoring(color);
	}
	public static Coloring of(int color) {
		return argb(color);
	}
	public static Coloring argb(int alpha,int red,int green,int blue) {
		return new ConstantColoring(FastColor.ARGB32.color(alpha, red, green, blue));
	}
	public static Coloring argb(float alpha,float red,float green,float blue) {
		return argb(((int)(alpha*255))&0xFF, ((int)(red*255))&0xFF, ((int)(green*255))&0xFF, ((int)(blue*255))&0xFF);
	}
	public UIColors() {
	}

}
