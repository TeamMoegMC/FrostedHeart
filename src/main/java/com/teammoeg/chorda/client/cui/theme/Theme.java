package com.teammoeg.chorda.client.cui.theme;

import net.minecraft.client.gui.GuiGraphics;

public interface Theme {

	void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled);

	void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight);

	void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused);

	void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight);

	void drawPanel(GuiGraphics graphics, int x, int y, int w, int h);

	void drawSlot(GuiGraphics graphics, int x, int y, int w, int h);

	void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h);

	int getUITextColor();
	int getButtonTextColor();
	int getButtonTextOverColor();
	int getButtonTextDisabledColor();
	int getErrorColor();

	boolean isUITextShadow();

	boolean isButtonTextShadow();
}