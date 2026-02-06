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

package com.teammoeg.frostedresearch.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class DrawDeskTheme {

	public DrawDeskTheme() {
		
	}
	public static void drawButton(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight,boolean enabled) {
		TechIcons.drawTexturedRect(graphics, x, y, w, h, isHighlight||(!enabled));
	}
	public static void drawSliderBackground(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		TechIcons.SLIDER_FRAME.draw(graphics, x, y, w, h);
	}
	public static void drawSliderBar(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		drawButton(graphics, x - 1, y, w + 2, h,!isHighlight,true);
	}
	public static void drawPanel(GuiGraphics graphics,int x,int y,int w,int h) {
        TechIcons.HLINE_L.draw(graphics, x, y + 8, 80, 3);
        TechIcons.VLINE.draw(graphics, x + 2, y + 9, 1, h- 16);
	}
	public static void drawProgressBar(GuiGraphics graphics,int x,int y,int w,int h,double progress) {
		TechIcons.SLIDER_FRAME.draw(graphics, x, y, w, h);
		int progressWidth = Mth.ceil((w-2) * progress);
		if (progressWidth > 1)
			TechIcons.drawTexturedRect(graphics, x + 1, y + 1, progressWidth, 6, true);
		
	}
	public static void drawSlot(GuiGraphics graphics,int x,int y,int w,int h) {
		TechIcons.SLOT.draw(graphics, x - 4, y - 4, w+8, h+8);
	}
	public static void drawDialog(GuiGraphics graphics,int x,int y,int w,int h) {
		TechIcons.DIALOG.draw(graphics, x, y, w, h);
	}
	
	public static void horizontalSplit(GuiGraphics graphics,int x,int y,int w) {
		TechIcons.HLINE.draw(graphics, x, y, w, 1);
	}
	public static void verticalSplit(GuiGraphics graphics,int x,int y,int h) {
		TechIcons.VLINE.draw(graphics, x, y, 1, h);
	}
	public static int getTextColor() {
		return TechIcons.text;
	}
	public static int getWeakColor() {
		return 0xFFADA691;
	}
	public static int getErrorColor() {
		return TechIcons.text_red;
	}
	public static void drawCheckBox(GuiGraphics graphics,int x,int y,int w,int h,boolean check,boolean cross) {
		if (check)
			TechIcons.CHECKBOX_CHECKED.draw(graphics, x, y, w, h);
		else if (cross)
			TechIcons.CHECKBOX_CROSS.draw(graphics, x, y, w, h);
		else
			TechIcons.CHECKBOX.draw(graphics, x, y, w, h);
	}
	
}
