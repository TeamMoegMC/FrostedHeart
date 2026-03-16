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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 可旋转的带纹理UV。预设旋转中心点，支持围绕中心点旋转绘制纹理。
 * <p>
 * Rotatable textured UV. Predefines a rotation center point and supports drawing the texture rotated around it.
 */
public class RotatableUV extends TexturedUV {
	/** 旋转中心X坐标 / Rotation center X coordinate */
	final int cX;
	/** 旋转中心Y坐标 / Rotation center Y coordinate */
	final int cY;

	/**
	 * 构造一个可旋转UV，使用默认纹理尺寸（256x256）。
	 * <p>
	 * Constructs a rotatable UV with default texture dimensions (256x256).
	 *
	 * @param rl 纹理资源位置 / Texture resource location
	 * @param x 纹理源X坐标 / Source X coordinate in texture
	 * @param y 纹理源Y坐标 / Source Y coordinate in texture
	 * @param w 宽度 / Width
	 * @param h 高度 / Height
	 * @param cX 旋转中心X偏移 / Rotation center X offset
	 * @param cY 旋转中心Y偏移 / Rotation center Y offset
	 */
	public RotatableUV(ResourceLocation rl,int x, int y, int w, int h,int cX, int cY) {
		super(rl, x, y, w, h);
		this.cX=cX;
		this.cY=cY;
	}

	/**
	 * 构造一个可旋转UV，使用自定义纹理尺寸。
	 * <p>
	 * Constructs a rotatable UV with custom texture dimensions.
	 *
	 * @param rl 纹理资源位置 / Texture resource location
	 * @param x 纹理源X坐标 / Source X coordinate in texture
	 * @param y 纹理源Y坐标 / Source Y coordinate in texture
	 * @param w 宽度 / Width
	 * @param h 高度 / Height
	 * @param cX 旋转中心X偏移 / Rotation center X offset
	 * @param cY 旋转中心Y偏移 / Rotation center Y offset
	 * @param textureW 纹理总宽度 / Total texture width
	 * @param textureH 纹理总高度 / Total texture height
	 */
	public RotatableUV(ResourceLocation rl,int x, int y, int w, int h,int cX, int cY, int textureW, int textureH) {
		super(rl, x, y, w, h, textureW, textureH);
		this.cX=cX;
		this.cY=cY;
	}

	/**
	 * 使用预设的旋转中心绘制旋转后的纹理。
	 * <p>
	 * Draws the texture rotated around the preset rotation center.
	 *
	 * @param matrixStack 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param degrees 旋转角度（度） / Rotation angle in degrees
	 */
	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, cX, cY, degrees);
	}

	/**
	 * 使用预设的旋转中心和额外的点偏移绘制旋转后的纹理。
	 * <p>
	 * Draws the texture rotated around the preset rotation center with an additional point offset.
	 *
	 * @param matrixStack 图形上下文 / Graphics context
	 * @param targetX 目标X坐标 / Target X coordinate
	 * @param targetY 目标Y坐标 / Target Y coordinate
	 * @param loc 额外的位置偏移 / Additional position offset
	 * @param degrees 旋转角度（度） / Rotation angle in degrees
	 */
	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, Point loc, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, loc, cX, cY, degrees);
	}


}
