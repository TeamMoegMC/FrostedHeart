package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.Font;

public interface UIElement {

	LayerHolder getLayerHolder();
	CUIScreenManager getManager();
	int getX();

	int getY();

	boolean isMouseOver();

	double getMouseX();

	double getMouseY();
	public default Font getFont() {
		return getLayerHolder().getFont();
	}

	void refresh();

	boolean onKeyPressed(int keyCode, int scanCode, int modifiers);
	boolean onMouseScrolled(double scroll);
	boolean onIMEInput(char c, int modifiers);

	int getScreenX();
	int getScreenY();
}
