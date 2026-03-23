package com.teammoeg.chorda.client;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.theme.VanillaTheme;

public class RenderingHint {
	public int renderingDepth;
	public boolean isDebug;
	public Theme themeOverride;
	public Theme themeDefault=VanillaTheme.INSTANCE;
	public RenderingHint() {
		super();
	}
	public void pushHint() {
		renderingDepth++;
	}
	public void popHint() {
		renderingDepth--;
	}
	public Theme theme(UIElement elm) {
		if(themeOverride!=null)
			return themeOverride;
		Theme currentTheme=elm.theme();
		if(currentTheme==null)
			return themeDefault;
		return currentTheme;
	}
}
