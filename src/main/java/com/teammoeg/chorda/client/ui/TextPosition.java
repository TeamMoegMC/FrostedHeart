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

import com.teammoeg.chorda.math.Point;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 文本位置类。基于一个预定义的点坐标，提供在GUI中绘制普通文本和居中文本的便捷方法。
 * <p>
 * Text position class. Based on a predefined point coordinate, provides convenience methods for
 * drawing normal and centered text in the GUI.
 */
public class TextPosition extends Point {

	/**
	 * 构造一个文本位置。
	 * <p>
	 * Constructs a text position.
	 *
	 * @param x X坐标 / X coordinate
	 * @param y Y坐标 / Y coordinate
	 */
	public TextPosition(int x, int y) {
		super(x, y);
	}
	/**
	 * 在指定偏移处绘制字符串文本。
	 * <p>
	 * Draws string text at the specified offset.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 */
	public void drawText(GuiGraphics graphics,String text,int x,int y,int color) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}

	/**
	 * 在指定偏移处绘制字符串文本，可选阴影。
	 * <p>
	 * Draws string text at the specified offset with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawText(GuiGraphics graphics,String text,int x,int y,int color,boolean hasShadow) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color,hasShadow);
	}

	/**
	 * 在指定偏移处绘制组件文本。
	 * <p>
	 * Draws component text at the specified offset.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 */
	public void drawText(GuiGraphics graphics,Component text,int x,int y,int color) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}

	/**
	 * 在指定偏移处绘制组件文本，可选阴影。
	 * <p>
	 * Draws component text at the specified offset with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawText(GuiGraphics graphics,Component text,int x,int y,int color,boolean hasShadow) {
		graphics.drawString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color,hasShadow);
	}

	/**
	 * 在预设位置绘制字符串文本。
	 * <p>
	 * Draws string text at the preset position.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param color 文本颜色 / Text color
	 */
	public void drawText(GuiGraphics graphics,String text,int color) {
		drawText(graphics, text,0,0, color);
	}

	/**
	 * 在预设位置绘制字符串文本，可选阴影。
	 * <p>
	 * Draws string text at the preset position with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawText(GuiGraphics graphics,String text,int color,boolean hasShadow) {
		drawText(graphics, text,0,0, color,hasShadow);
	}

	/**
	 * 在预设位置绘制组件文本。
	 * <p>
	 * Draws component text at the preset position.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param color 文本颜色 / Text color
	 */
	public void drawText(GuiGraphics graphics,Component text,int color) {
		drawText(graphics, text,0,0, color);
	}

	/**
	 * 在预设位置绘制组件文本，可选阴影。
	 * <p>
	 * Draws component text at the preset position with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawText(GuiGraphics graphics,Component text,int color,boolean hasShadow) {
		drawText(graphics, text,0,0, color,hasShadow);
	}

	/**
	 * 在指定偏移处绘制居中字符串文本。
	 * <p>
	 * Draws centered string text at the specified offset.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 */
	public void drawCenterText(GuiGraphics graphics,String text,int x,int y,int color) {
		graphics.drawCenteredString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}
	/**
	 * 在指定偏移处绘制居中字符串文本，可选阴影。
	 * <p>
	 * Draws centered string text at the specified offset with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawCenterText(GuiGraphics graphics,String text,int x,int y,int color,boolean hasShadow) {
		Font font=Minecraft.getInstance().font;
		graphics.drawString(font, text, x+this.x- font.width(text) / 2, y+this.y,color,hasShadow);
	}
	/**
	 * 在指定偏移处绘制居中组件文本。
	 * <p>
	 * Draws centered component text at the specified offset.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 */
	public void drawCenterText(GuiGraphics graphics,Component text,int x,int y,int color) {
		graphics.drawCenteredString(Minecraft.getInstance().font, text, x+this.x, y+this.y, color);
	}

	/**
	 * 在指定偏移处绘制居中组件文本，可选阴影。
	 * <p>
	 * Draws centered component text at the specified offset with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param x 额外X偏移 / Additional X offset
	 * @param y 额外Y偏移 / Additional Y offset
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawCenterText(GuiGraphics graphics,Component text,int x,int y,int color,boolean hasShadow) {
		Font font=Minecraft.getInstance().font;
		FormattedCharSequence formattedcharsequence = text.getVisualOrderText();
		graphics.drawString(font, formattedcharsequence, x+this.x- font.width(formattedcharsequence) / 2, y+this.y,color,hasShadow);
	}
	/**
	 * 在预设位置绘制居中字符串文本。
	 * <p>
	 * Draws centered string text at the preset position.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param color 文本颜色 / Text color
	 */
	public void drawCenterText(GuiGraphics graphics,String text,int color) {
		drawCenterText(graphics, text,0,0, color);
	}

	/**
	 * 在预设位置绘制居中字符串文本，可选阴影。
	 * <p>
	 * Draws centered string text at the preset position with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的文本 / Text to draw
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawCenterText(GuiGraphics graphics,String text,int color,boolean hasShadow) {
		drawCenterText(graphics, text,0,0, color,hasShadow);
	}

	/**
	 * 在预设位置绘制居中组件文本。
	 * <p>
	 * Draws centered component text at the preset position.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param color 文本颜色 / Text color
	 */
	public void drawCenterText(GuiGraphics graphics,Component text,int color) {
		drawCenterText(graphics, text,0,0, color);
	}

	/**
	 * 在预设位置绘制居中组件文本，可选阴影。
	 * <p>
	 * Draws centered component text at the preset position with optional shadow.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param text 要绘制的组件文本 / Component text to draw
	 * @param color 文本颜色 / Text color
	 * @param hasShadow 是否绘制阴影 / Whether to draw shadow
	 */
	public void drawCenterText(GuiGraphics graphics,Component text,int color,boolean hasShadow) {
		drawCenterText(graphics, text,0,0, color,hasShadow);
	}
}
