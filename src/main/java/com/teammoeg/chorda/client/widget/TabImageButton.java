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

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.text.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/**
 * 标签页图片按钮。基于纹理图集的标签页切换按钮，当该标签为当前选中标签时显示选中纹理，
 * 否则显示未选中纹理（水平偏移一个按钮宽度）。
 * 通过{@link #bind(Supplier)}绑定当前标签页状态的提供者。
 * <p>
 * Tab image button. A texture atlas-based tab switching button that displays the selected
 * texture when this tab is the currently active tab, and the unselected texture otherwise
 * (offset by one button width horizontally).
 * Use {@link #bind(Supplier)} to bind the current tab state provider.
 */
public class TabImageButton extends Button {
	/** 纹理起始X偏移 / Texture start X offset */
	int xTexStart;
	/** 纹理起始Y偏移 / Texture start Y offset */
	int yTexStart;
	/** 纹理文件总宽度 / Total texture file width */
	private final int textureWidth;
	/** 纹理文件总高度 / Total texture file height */
	private final int textureHeight;
	/** 此按钮对应的标签页索引 / Tab index this button corresponds to */
	final int tab;
	/** 当前活动标签页索引的提供者 / Supplier for the currently active tab index */
	Supplier<Integer> currentTab;
	/** 按钮使用的纹理资源 / Texture resource used by the button */
	ResourceLocation TEXTURE;

	/**
	 * 创建一个标签页图片按钮，使用默认纹理尺寸256x256，无提示文本。
	 * <p>
	 * Creates a tab image button with default texture size 256x256 and no tooltip.
	 *
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param xIn x坐标 / X coordinate
	 * @param yIn y坐标 / Y coordinate
	 * @param widthIn 按钮宽度 / Button width
	 * @param heightIn 按钮高度 / Button height
	 * @param xTexStartIn 纹理起始X偏移 / Texture start X offset
	 * @param yTexStartIn 纹理起始Y偏移 / Texture start Y offset
	 * @param tab 此按钮对应的标签页索引 / Tab index this button corresponds to
	 * @param onPressIn 按下时的回调 / Callback when pressed
	 */
	public TabImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,int tab,
			Button.OnPress onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn,tab, null, onPressIn);
	}

	/**
	 * 创建一个标签页图片按钮，使用默认纹理尺寸256x256，可选提示文本。
	 * <p>
	 * Creates a tab image button with default texture size 256x256 and optional tooltip.
	 *
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param xIn x坐标 / X coordinate
	 * @param yIn y坐标 / Y coordinate
	 * @param widthIn 按钮宽度 / Button width
	 * @param heightIn 按钮高度 / Button height
	 * @param xTexStartIn 纹理起始X偏移 / Texture start X offset
	 * @param yTexStartIn 纹理起始Y偏移 / Texture start Y offset
	 * @param tab 此按钮对应的标签页索引 / Tab index this button corresponds to
	 * @param tooltipIn 提示文本，可为null / Tooltip, may be null
	 * @param onPressIn 按下时的回调 / Callback when pressed
	 */
	public TabImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,int tab,
		Tooltip tooltipIn, Button.OnPress onPressIn) {

		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, 256, 256,tab, onPressIn, tooltipIn,
			Components.empty());
	}

	/**
	 * 创建一个标签页图片按钮，可指定纹理尺寸和标题，无提示文本。
	 * <p>
	 * Creates a tab image button with custom texture dimensions and title, without tooltip.
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
	 * @param tab 此按钮对应的标签页索引 / Tab index this button corresponds to
	 * @param onPress 按下时的回调 / Callback when pressed
	 * @param title 按钮标题 / Button title
	 */
	public TabImageButton(ResourceLocation texture,int x, int y, int width, int height, int xTexStart, int yTexStart, int textureWidth,int textureHeight,int tab,
			Button.OnPress onPress, Component title) {
		this(texture,x, y, width, height, xTexStart, yTexStart, textureWidth, textureHeight,tab, onPress, null, title);
	}

	/**
	 * 创建一个标签页图片按钮的完整构造方法。
	 * <p>
	 * Full constructor for creating a tab image button.
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
	 * @param tab 此按钮对应的标签页索引 / Tab index this button corresponds to
	 * @param onPressIn 按下时的回调 / Callback when pressed
	 * @param tooltipIn 提示文本，可为null / Tooltip, may be null
	 * @param title 按钮标题 / Button title
	 */
	public TabImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStart,
			int yTexStart, int textureWidth, int textureHeight,int tab,
			Button.OnPress onPressIn,Tooltip tooltipIn, Component title) {
		super(Button.builder(title, onPressIn).bounds(xIn, yIn, widthIn, heightIn).tooltip(tooltipIn));
		this.TEXTURE=texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.xTexStart = xTexStart;
		this.yTexStart = yTexStart;
		this.tab=tab;
	}

	/**
	 * 绑定当前标签页索引的提供者。渲染时通过此提供者获取当前活动标签页，
	 * 以决定显示选中或未选中纹理。
	 * <p>
	 * Binds a supplier for the current tab index. During rendering, the supplier is queried
	 * to determine the currently active tab, deciding whether to display the selected or
	 * unselected texture.
	 *
	 * @param supp 当前标签页索引的提供者 / Supplier for the current tab index
	 * @return 当前实例，用于链式调用 / This instance for method chaining
	 */
	public TabImageButton bind(Supplier<Integer> supp) {
		this.currentTab=supp;
		return this;
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
	 * 渲染标签页按钮。当此按钮的标签索引与当前活动标签匹配时显示选中纹理（左侧），
	 * 否则显示未选中纹理（右侧偏移一个按钮宽度）。
	 * <p>
	 * Renders the tab button. When this button's tab index matches the currently active tab,
	 * displays the selected texture (left side); otherwise displays the unselected texture
	 * (right side, offset by one button width).
	 *
	 * @param matrixStack 图形上下文 / Graphics context
	 * @param mouseX 鼠标X坐标 / Mouse X coordinate
	 * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
	 * @param partialTicks 渲染插值时间 / Partial tick time for rendering interpolation
	 */
	@Override
	public void renderWidget(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		int current=0;
		if(currentTab!=null)
			current=currentTab.get();
		int i =  (tab==current)?0:this.width;


		RenderSystem.enableDepthTest();
		matrixStack.blit(TEXTURE, this.getX(), this.getY(), this.xTexStart + i, this.yTexStart, this.width, this.height,
				this.textureWidth, this.textureHeight);

	}
}