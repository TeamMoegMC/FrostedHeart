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

package com.teammoeg.chorda.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class TextPosition extends Point {

	public TextPosition(int x, int y) {
		super(x, y);
	}
	public void drawText(GuiGraphics graphics,String text,int x,int y,int color) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}
	public void drawText(GuiGraphics graphics,String text,int x,int y,int color,boolean hasShadow) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color,hasShadow);
	}
	public void drawText(GuiGraphics graphics,Component text,int x,int y,int color) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}
	public void drawText(GuiGraphics graphics,Component text,int x,int y,int color,boolean hasShadow) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color,hasShadow);
	}
	public void drawText(GuiGraphics graphics,String text,int color) {
		drawText(graphics, text,0,0, color);
	}
	public void drawText(GuiGraphics graphics,String text,int color,boolean hasShadow) {
		drawText(graphics, text,0,0, color,hasShadow);
	}
	public void drawText(GuiGraphics graphics,Component text,int color) {
		drawText(graphics, text,0,0, color);
	}
	public void drawText(GuiGraphics graphics,Component text,int color,boolean hasShadow) {
		drawText(graphics, text,0,0, color,hasShadow);
	}
	public void drawCenterText(GuiGraphics graphics,String text,int x,int y,int color) {
		graphics.drawCenteredString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}
	public void drawCenterText(GuiGraphics graphics,String text,int x,int y,int color,boolean hasShadow) {
		Font font=Minecraft.getInstance().font;
		graphics.drawString(font, text, x+this.x- font.width(text) / 2, y+this.y,color,hasShadow);
	}
	public void drawCenterText(GuiGraphics graphics,Component text,int x,int y,int color) {
		graphics.drawCenteredString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}
	public void drawCenterText(GuiGraphics graphics,Component text,int x,int y,int color,boolean hasShadow) {
		Font font=Minecraft.getInstance().font;
		FormattedCharSequence formattedcharsequence = text.getVisualOrderText();
		graphics.drawString(font, formattedcharsequence, x+this.x- font.width(formattedcharsequence) / 2, y+this.y,color,hasShadow);
	}
	public void drawCenterText(GuiGraphics graphics,String text,int color) {
		drawCenterText(graphics, text,0,0, color);
	}
	public void drawCenterText(GuiGraphics graphics,String text,int color,boolean hasShadow) {
		drawCenterText(graphics, text,0,0, color,hasShadow);
	}
	public void drawCenterText(GuiGraphics graphics,Component text,int color) {
		drawCenterText(graphics, text,0,0, color);
	}
	public void drawCenterText(GuiGraphics graphics,Component text,int color,boolean hasShadow) {
		drawCenterText(graphics, text,0,0, color,hasShadow);
	}
}
