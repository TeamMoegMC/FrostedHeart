package com.teammoeg.chorda.client.cui.theme;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;

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
	public UIColors() {
	}

}
