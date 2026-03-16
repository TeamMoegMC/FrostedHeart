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

/**
 * 可悬停图片按钮。基于纹理图集的按钮，悬停时显示不同的纹理区域。
 * 纹理布局要求：普通状态在左侧，悬停状态在右侧（水平偏移一个按钮宽度）；
 * 不同state状态的纹理在垂直方向排列（垂直偏移一个按钮高度）。
 * <p>
 * Hoverable image button. A texture atlas-based button that displays a different texture
 * region when hovered. Texture layout requires: normal state on the left, hovered state
 * on the right (offset by one button width horizontally); different state textures are
 * arranged vertically (offset by one button height).
 */
public class HoverableImageButton extends Button {
	/** 纹理起始X偏移 / Texture start X offset */
	int xTexStart;
	/** 纹理起始Y偏移 / Texture start Y offset */
	int yTexStart;
	/** 纹理文件总宽度 / Total texture file width */
	private final int textureWidth;
	/** 纹理文件总高度 / Total texture file height */
	private final int textureHeight;
	/** 当前按钮状态，用于选择垂直方向上的纹理偏移 / Current button state, used to select vertical texture offset */
	int state;
	/** 按钮使用的纹理资源 / Texture resource used by the button */
	ResourceLocation TEXTURE;

	/**
	 * 创建一个可悬停图片按钮，使用默认纹理尺寸256x256，无提示文本。
	 * <p>
	 * Creates a hoverable image button with default texture size 256x256 and no tooltip.
	 *
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param xIn x坐标 / X coordinate
	 * @param yIn y坐标 / Y coordinate
	 * @param widthIn 按钮宽度 / Button width
	 * @param heightIn 按钮高度 / Button height
	 * @param xTexStartIn 纹理起始X偏移 / Texture start X offset
	 * @param yTexStartIn 纹理起始Y偏移 / Texture start Y offset
	 * @param onPressIn 按下时的回调 / Callback when pressed
	 */
	public HoverableImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
			Button.OnPress onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, null, onPressIn);
	}

	/**
	 * 创建一个可悬停图片按钮，使用默认纹理尺寸256x256，可选提示文本。
	 * <p>
	 * Creates a hoverable image button with default texture size 256x256 and optional tooltip.
	 *
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param xIn x坐标 / X coordinate
	 * @param yIn y坐标 / Y coordinate
	 * @param widthIn 按钮宽度 / Button width
	 * @param heightIn 按钮高度 / Button height
	 * @param xTexStartIn 纹理起始X偏移 / Texture start X offset
	 * @param yTexStartIn 纹理起始Y偏移 / Texture start Y offset
	 * @param tooltipIn 提示文本，可为null / Tooltip, may be null
	 * @param onPressIn 按下时的回调 / Callback when pressed
	 */
	public HoverableImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
		Tooltip tooltipIn, Button.OnPress onPressIn) {

		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, 256, 256, onPressIn, tooltipIn,
			Components.empty());
	}

	/**
	 * 创建一个可悬停图片按钮，可指定纹理尺寸和标题，无提示文本。
	 * <p>
	 * Creates a hoverable image button with custom texture dimensions and title, without tooltip.
	 *
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param x x坐标 / X coordinate
	 * @param y y坐标 / Y coordinate
	 * @param width 按钮宽度 / Button width
	 * @param height 按钮高度 / Button height
	 * @param xTexStart 纹理起始X偏移 / Texture start X offset
	 * @param yTexStart 纹理起始Y偏移 / Texture start Y offset
	 * @param textureWidth 纹理文件总宽度 / Total texture file width
	 * @param textureHeight 纹理文件总高度 / Total texture file height
	 * @param onPress 按下时的回调 / Callback when pressed
	 * @param title 按钮标题 / Button title
	 */
	public HoverableImageButton(ResourceLocation texture,int x, int y, int width, int height, int xTexStart, int yTexStart, int textureWidth,
			int textureHeight, Button.OnPress onPress, Component title) {
		this(texture,x, y, width, height, xTexStart, yTexStart, textureWidth, textureHeight, onPress, null, title);
	}

	/**
	 * 创建一个可悬停图片按钮的完整构造方法。
	 * <p>
	 * Full constructor for creating a hoverable image button.
	 *
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param xIn x坐标 / X coordinate
	 * @param yIn y坐标 / Y coordinate
	 * @param widthIn 按钮宽度 / Button width
	 * @param heightIn 按钮高度 / Button height
	 * @param xTexStart 纹理起始X偏移 / Texture start X offset
	 * @param yTexStart 纹理起始Y偏移 / Texture start Y offset
	 * @param textureWidth 纹理文件总宽度 / Total texture file width
	 * @param textureHeight 纹理文件总高度 / Total texture file height
	 * @param onPressIn 按下时的回调 / Callback when pressed
	 * @param tooltipIn 提示文本，可为null / Tooltip, may be null
	 * @param title 按钮标题 / Button title
	 */
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

	/**
	 * 设置按钮的位置。
	 * <p>
	 * Sets the button position.
	 *
	 * @param xIn x坐标 / X coordinate
	 * @param yIn y坐标 / Y coordinate
	 */
	public void setPosition(int xIn, int yIn) {
		this.setX(xIn);
		this.setY(yIn);
	}

	/**
	 * 渲染按钮控件。悬停时水平偏移一个按钮宽度以显示悬停纹理，
	 * 根据state值垂直偏移以显示不同状态的纹理。
	 * <p>
	 * Renders the button widget. When hovered, offsets horizontally by one button width
	 * to show the hover texture; offsets vertically by the state value to show different
	 * state textures.
	 *
	 * @param matrixStack 图形上下文 / Graphics context
	 * @param mouseX 鼠标X坐标 / Mouse X coordinate
	 * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
	 * @param partialTicks 渲染插值时间 / Partial tick time for rendering interpolation
	 */
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