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

import com.teammoeg.chorda.client.cui.base.UILayer;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 无背景文本输入框。继承自{@link TextBox}，但不绘制任何背景，
 * 文本起始位置为0（无内边距）。
 * <p>
 * Text input box without background. Extends {@link TextBox} but does not draw
 * any background, with text starting position at 0 (no padding).
 */
public class TextBoxNoBackground extends TextBox {

	/**
	 * 创建无背景文本输入框。
	 * <p>
	 * Creates a text input box without background.
	 *
	 * @param panel 父级UI图层 / Parent UI layer
	 */
	public TextBoxNoBackground(UILayer panel) {
		super(panel);
		textStartPos=0;
	}

	/**
	 * 空实现，不绘制任何文本框背景。
	 * <p>
	 * No-op implementation; does not draw any text box background.
	 */
	@Override
	public void drawTextBox(GuiGraphics graphics, int x, int y, int w, int h) {

	}

}
