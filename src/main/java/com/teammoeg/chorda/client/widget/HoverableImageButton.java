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

package com.teammoeg.chorda.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.text.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class HoverableImageButton extends Button {
	int xTexStart;
	int yTexStart;
	private final int textureWidth;
	private final int textureHeight;
	int state;
	ResourceLocation TEXTURE;
	public HoverableImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
			Button.OnPress onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, null, onPressIn);
	}

	public HoverableImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
		Tooltip tooltipIn, Button.OnPress onPressIn) {
	
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, 256, 256, onPressIn, tooltipIn,
			Components.empty());
	}

	public HoverableImageButton(ResourceLocation texture,int x, int y, int width, int height, int xTexStart, int yTexStart, int textureWidth,
			int textureHeight, Button.OnPress onPress, Component title) {
		this(texture,x, y, width, height, xTexStart, yTexStart, textureWidth, textureHeight, onPress, null, title);
	}

	public HoverableImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStart,
			int yTexStart, int textureWidth, int textureHeight, Button.OnPress onPressIn,
			Tooltip tooltipIn, Component title) {
		super(Button.builder(title, onPressIn).bounds(xIn, yIn, widthIn, heightIn).tooltip(tooltipIn));
		this.TEXTURE=texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.xTexStart = xTexStart;
		this.yTexStart = yTexStart;
	}

	public void setPosition(int xIn, int yIn) {
		this.setX(xIn);
		this.setY(yIn);
	}
	@Override
	public void renderWidget(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		int i = 0, j = state * this.height;

		if (this.isHovered()) {
			i += this.width;
		}
		RenderSystem.enableDepthTest();
		matrixStack.blit(TEXTURE, this.getX(), this.getY(), this.xTexStart + i, this.yTexStart + j, this.width, this.height,
				this.textureWidth, this.textureHeight);

	}
}