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


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class RotatableUV extends TexturedUV {
	final int cX,cY;
	public RotatableUV(ResourceLocation rl,int x, int y, int w, int h,int cX, int cY) {
		super(rl, x, y, w, h);
		this.cX=cX;
		this.cY=cY;
	}

	public RotatableUV(ResourceLocation rl,int x, int y, int w, int h,int cX, int cY, int textureW, int textureH) {
		super(rl, x, y, w, h, textureW, textureH);
		this.cX=cX;
		this.cY=cY;
	}

	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, cX, cY, degrees);
	}

	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, Point loc, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, loc, cX, cY, degrees);
	}


}
