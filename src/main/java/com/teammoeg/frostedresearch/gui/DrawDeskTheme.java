package com.teammoeg.frostedresearch.gui;

import net.minecraft.client.gui.GuiGraphics;

public class DrawDeskTheme {

	public DrawDeskTheme() {
		
	}
	public static void drawButton(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		TechIcons.drawTexturedRect(graphics, x, y, w, h, isHighlight);
	}
	public static void drawSliderBackground(GuiGraphics graphics,int x,int y,int w,int h) {
		TechIcons.SLIDER_FRAME.draw(graphics, x, y, w, h);
	}
	public static void drawSliderBar(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		drawButton(graphics, x, y, w, h,!isHighlight);
	}
	public static void drawPanel(GuiGraphics graphics,int x,int y,int w,int h) {
        TechIcons.HLINE_L.draw(graphics, x, y + 8, 80, 3);
        TechIcons.VLINE.draw(graphics, x + 2, y + 9, 1, h- 16);
	}
}
