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

import com.teammoeg.chorda.math.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;

/**
 * 原版Minecraft风格主题。使用原版纹理和配色方案，呈现与原版UI一致的外观。
 * <p>
 * Vanilla Minecraft style theme. Uses vanilla textures and color schemes to present an appearance
 * consistent with the vanilla UI.
 */
public class VanillaTheme implements Theme {
	private static final ResourceLocation BEACON_LOCATION = new ResourceLocation("textures/gui/container/beacon.png");
	private static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
	/** 单例实例 / Singleton instance */
	public static final VanillaTheme INSTANCE=new VanillaTheme();

	/**
	 * 受保护的构造函数，使用{@link #INSTANCE}获取实例。
	 * <p>
	 * Protected constructor, use {@link #INSTANCE} to get the instance.
	 */
	protected VanillaTheme() {
	}
	@Override
	public void drawButton(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight,boolean enabled) {
		if(enabled) {
			if(isHighlight) {
				graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, 86);
			}else {
				graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, 66);
			}
		}else{
			graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, 46); 
		}
	}
	@Override
	public void drawSliderBackground(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		int i = isHighlight? 0xFFFFFFFF : 0xFFA0A0A0;
		graphics.fill(x, y, x + w, y + h, i);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
	}
	@Override
	public void drawTextboxBackground(GuiGraphics graphics,int x,int y,int w,int h,boolean focused) {
		drawSliderBackground(graphics,x-1,y-1,w+2,h+2,focused);
	}
	@Override
	public void drawSliderBar(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		graphics.fill(x, y, x + w, y + h, 0xFF808080);
		graphics.fill(x, y, x + w - 1, y + h - 1,0xFFC0C0C0);
		
	}
	@Override
	public void drawPanel(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.fill(x, y, x+w, y+h, 0xff8b8b8b);
	}
	@Override
	public void drawSlot(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.blit(BEACON_LOCATION, x-1, y-1, 35, 136, 18, 18);
	}
	

	@Override
	public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, w, h, 4, 32, 32, 82, 208);
	}

	/**
	 * 绘制带搜索栏的UI背景。
	 * <p>
	 * Draws a UI background with a search bar area.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 */
	public void drawUIBackgroundWithSearch(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, w, h, 24, 28, 0, 0, 148, 167);
	}
	/**
	 * 绘制原版风格的UI槽位。
	 * <p>
	 * Draws a vanilla-style UI slot.
	 *
	 * @param graphics 图形上下文 / the graphics context
	 * @param x X坐标 / the x position
	 * @param y Y坐标 / the y position
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 */
	public void drawUISlot(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, w, h, 2, 24, 24, 29, 206);
	}
	@Override
	public boolean isUITextShadow() {
		return false;
	}
	
	@Override
	public boolean isButtonTextShadow() {
		return true;
	}
	
	@Override
	public int UITextColor() {
		return 0xff000000;
	}
	@Override
	public int UIAltTextColor() {
		return 0xff000000;
	}

	@Override
	public int UIBGColor() {
		return 0xFFc6c6c6;
	}

	@Override
	public int UIBGBorderColor() {
		return Colors.BLACK;
	}

	@Override
	public int buttonTextColor() {
		return 0xffffffff;
	}
	@Override
	public int errorColor() {
		return 0xffa92b0d;
	}

	@Override
	public int successColor() {
		return 0;
	}

	@Override
	public int buttonTextOverColor() {
		return 0xffffffff;
	}
	@Override
	public int buttonTextDisabledColor() {
		return 0xccffffff;
	}
}
