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

package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.text.Components;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 按钮控件基类。可点击触发动作的抽象UI组件，支持图标和标题显示。
 * <p>
 * Abstract button widget base class. A clickable UI component that triggers actions,
 * supporting both icon and title display.
 */
public abstract class Button extends UIElement {
	/** 按钮标题文本 / Button title text */
	protected Component title;
	/** 按钮图标 / Button icon */
	protected CIcon icon;

	/**
	 * 创建带标题和图标的按钮。
	 * <p>
	 * Creates a button with a title and an icon.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 * @param t 按钮标题 / Button title
	 * @param i 按钮图标 / Button icon
	 */
	public Button(UIElement panel, Component t, CIcon i) {
		super(panel);
		setSize(16, 16);
		icon = i;
		title = t;
	}

	/**
	 * 创建无标题无图标的按钮。
	 * <p>
	 * Creates a button with no title and no icon.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 */
	public Button(UIElement panel) {
		this(panel, Components.immutableEmpty(), CIcons.nop());
	}

	/**
	 * 创建仅带图标的按钮。
	 * <p>
	 * Creates a button with an icon only.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 * @param i 按钮图标 / Button icon
	 */
	public Button(UIElement panel, CIcon i) {
		this(panel,Components.immutableEmpty(),i);
	}

	/** {@inheritDoc} */
	@Override
	public Component getTitle() {
		return title;
	}

	/**
	 * 设置按钮标题并自动调整尺寸。
	 * <p>
	 * Sets the button title and auto-fits the size.
	 *
	 * @param s 新标题 / New title
	 * @return 当前按钮实例（链式调用） / This button instance (for chaining)
	 */
	public Button setTitle(Component s) {
		title = s;
		fitSize();
		return this;
	}

	/**
	 * 判断按钮是否设置了图标。
	 * <p>
	 * Checks whether the button has an icon set.
	 *
	 * @return 是否有图标 / Whether an icon is present
	 */
	public boolean hasIcon() {
		return icon!=CIcons.nop();
	}

	/**
	 * 设置按钮图标并自动调整尺寸。
	 * <p>
	 * Sets the button icon and auto-fits the size.
	 *
	 * @param i 新图标 / New icon
	 * @return 当前按钮实例（链式调用） / This button instance (for chaining)
	 */
	public Button setIcon(CIcon i) {
		icon = i;
		fitSize();
		return this;
	}

	/**
	 * 根据标题和图标自动调整按钮尺寸。
	 * <p>
	 * Auto-fits the button size based on title and icon.
	 */
	protected void fitSize() {
		setWidth(parent.getFont().width(title)+((Components.isEmpty(title)&&hasIcon())?0:8) + (hasIcon() ? 20 : 0));
		setHeight(hasIcon() ?20:16);
	}

	/**
	 * 绘制按钮背景。
	 * <p>
	 * Draws the button background.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param x 绘制起始X坐标 / Drawing X coordinate
	 * @param y 绘制起始Y坐标 / Drawing Y coordinate
	 * @param w 绘制宽度 / Drawing width
	 * @param h 绘制高度 / Drawing height
	 */
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		theme().drawButton(graphics, x, y, w, h, isMouseOver(), isEnabled());
	}

	/**
	 * 绘制按钮图标。
	 * <p>
	 * Draws the button icon.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param x 绘制起始X坐标 / Drawing X coordinate
	 * @param y 绘制起始Y坐标 / Drawing Y coordinate
	 * @param w 绘制宽度 / Drawing width
	 * @param h 绘制高度 / Drawing height
	 */
	public void drawIcon(GuiGraphics graphics, int x, int y, int w, int h) {
		icon.draw(graphics, x, y, w, h);
	}

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		CGuiHelper.resetGuiDrawing();
		int s = h >= 16 ? 16 : 8;
		drawBackground(graphics, x, y, w, h);
		if(hasIcon())
			drawIcon(graphics, x + (w - s) / 2, y + (h - s) / 2, s, s);
	}

	/** {@inheritDoc} */
	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				playClickSound();
				onClicked(button);
			}

			return true;
		}

		return false;
	}

	/**
	 * 按钮被点击时的回调方法，由子类实现具体逻辑。
	 * <p>
	 * Callback method when the button is clicked. Subclasses implement the specific logic.
	 *
	 * @param button 被点击的鼠标按键 / The mouse button that was clicked
	 */
	public abstract void onClicked(MouseButton button);

	/**
	 * 播放按钮点击音效。
	 * <p>
	 * Plays the button click sound effect.
	 */
	public void playClickSound() {
		CInputHelper.playClickSound();
	}

	/** {@inheritDoc} */
	@Override
	public Cursor getCursor() {
		return Cursor.HAND;
	}
}