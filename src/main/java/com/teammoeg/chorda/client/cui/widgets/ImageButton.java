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

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 图像按钮控件。使用不同图标表示正常、悬停和按下三种状态的抽象按钮。
 * <p>
 * Image button widget. An abstract button that uses different icons to represent
 * normal, hover, and pressed states.
 */
public abstract class ImageButton extends UIElement {
	/** 正常状态图标、悬停状态图标、按下状态图标 / Normal state icon, hover state icon, pressed state icon */
	protected CIcon normal,over,pressed;
	/** 当前是否被按下 / Whether currently pressed */
	protected boolean isPressed;


	/**
	 * 创建具有三种状态图标的图像按钮。
	 * <p>
	 * Creates an image button with three state icons.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param normal 正常状态图标 / Normal state icon
	 * @param over 悬停状态图标 / Hover state icon
	 * @param pressed 按下状态图标 / Pressed state icon
	 */
	public ImageButton(UIElement parent, CIcon normal, CIcon over, CIcon pressed) {
		super(parent);
		this.normal = normal;
		this.over = over;
		this.pressed = pressed;
	}

	/**
	 * 创建具有正常和悬停两种图标的图像按钮，按下状态使用悬停图标。
	 * <p>
	 * Creates an image button with normal and hover icons. Pressed state reuses the hover icon.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param normal 正常状态图标 / Normal state icon
	 * @param over 悬停状态图标 / Hover state icon
	 */
	public ImageButton(UIElement parent, CIcon normal, CIcon over) {
		this(parent,normal,over,over);
	}

	/**
	 * 创建仅有一种图标的图像按钮，所有状态使用同一图标。
	 * <p>
	 * Creates an image button with a single icon for all states.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param normal 所有状态使用的图标 / Icon used for all states
	 */
	public ImageButton(UIElement parent, CIcon normal) {
		this(parent,normal,normal);
	}

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		CGuiHelper.resetGuiDrawing();
		if(isMouseOver()) {
			if(isPressed) {
				pressed.draw(graphics, x, y, w, h);
			}else {
				over.draw(graphics, x, y, w, h);
			}
		}else
			normal.draw(graphics, x, y, w, h);
	}

	/** {@inheritDoc} */
	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (isEnabled()) {
				onClicked(button);
				isPressed=true;
			}

			return true;
		}

		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void onMouseReleased(MouseButton button) {
		super.onMouseReleased(button);
		isPressed=false;
	}

	/**
	 * 按钮被点击时的回调方法，由子类实现具体逻辑。
	 * <p>
	 * Callback method when the button is clicked. Subclasses implement the specific logic.
	 *
	 * @param button 被点击的鼠标按键 / The mouse button that was clicked
	 */
	public abstract void onClicked(MouseButton button);

	/** {@inheritDoc} */
	@Override
	public Cursor getCursor() {
		return Cursor.HAND;
	}
}