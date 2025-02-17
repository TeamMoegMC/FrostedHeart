package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.Font;

public interface UIElementBase {

	LayerHolder getLayerHolder();

	int getX();

	int getY();

	boolean isMouseOver();

	int getMouseX();

	int getMouseY();
	public default Font getFont() {
		return getLayerHolder().getFont();
	}
}
