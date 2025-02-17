package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.Font;

public interface LayerHolder {
	void focusOn(Focusable elm);
	Font getFont();
	int getErrorColor();
	int getHighlightColor();
	int getFontColor();
	int getBackgroundColor();
	int getFrameColor();
	int getButtonFaceColor();
	int getButtonShadowColor();
}
