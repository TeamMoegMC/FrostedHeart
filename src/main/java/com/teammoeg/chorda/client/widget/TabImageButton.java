/*
 * Copyright (c) 2024 TeamMoeg
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

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class TabImageButton extends Button {
	int xTexStart;
	int yTexStart;
	private final int textureWidth;
	private final int textureHeight;
	final int tab;
	Supplier<Integer> currentTab;
	ResourceLocation TEXTURE;
	public TabImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,int tab,
			Button.OnPress onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, tab, null, onPressIn);
	}

	public TabImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,int tab,
		Tooltip tt, Button.OnPress onPressIn) {
	
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, 256, 256, tab, onPressIn, tt,
			Components.empty());
	}

	public TabImageButton(ResourceLocation texture,int x, int y, int width, int height, int xTexStart, int yTexStart, int textureWidth,
			int textureHeight,int tab, Button.OnPress onPress, Component title) {
		this(texture,x, y, width, height, xTexStart, yTexStart, textureWidth, textureHeight, tab, onPress, null, title);
	}

	public TabImageButton(ResourceLocation texture,int p_i244513_1_, int p_i244513_2_, int p_i244513_3_, int p_i244513_4_, int p_i244513_5_,
			int p_i244513_6_, int p_i244513_9_, int p_i244513_10_,int tab, Button.OnPress p_i244513_11_,
			Tooltip p_i244513_12_, Component p_i244513_13_) {
		super(Button.builder(p_i244513_13_, p_i244513_11_).bounds(p_i244513_2_, p_i244513_3_, p_i244513_4_, p_i244513_5_).tooltip(p_i244513_12_));
		this.TEXTURE=texture;
		this.textureWidth = p_i244513_9_;
		this.textureHeight = p_i244513_10_;
		this.xTexStart = p_i244513_5_;
		this.yTexStart = p_i244513_6_;
		this.tab=tab;
	}
	public TabImageButton bind(Supplier<Integer> supp) {
		this.currentTab=supp;
		return this;
	}
	public void setPosition(int xIn, int yIn) {
		this.setX(xIn);
		this.setY(yIn);
	}
	@Override
	public void renderWidget(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		int current=0;
		if(currentTab!=null)
			current=currentTab.get();
		int i =  (tab==current)?0: this.width;


		RenderSystem.enableDepthTest();
		matrixStack.blit(TEXTURE, this.getX(), this.getY(), this.xTexStart + i, this.yTexStart, this.width, this.height,
				this.textureWidth, this.textureHeight);

	}
}