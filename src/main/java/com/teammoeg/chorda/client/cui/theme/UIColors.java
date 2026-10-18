/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
