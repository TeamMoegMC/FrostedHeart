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

import com.teammoeg.chorda.client.RenderingHint;
import net.minecraft.client.gui.GuiGraphics;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;

/**
 * 简单图层控件。无任何默认子元素或布局逻辑的空白UI图层，
 * 用于手动添加和管理子元素。
 * <p>
 * Simple layer widget. A blank UI layer with no default child elements or layout logic,
 * intended for manually adding and managing child elements.
 */
public class SimpleLayer extends UILayer {

	private int backgroundColor = 0x00000000;

	/** @return 背景颜色，格式为 ARGB，默认完全透明。 */
	public int getBackgroundColor() {
		return backgroundColor;
	}

	/** @param backgroundColor 背景颜色，格式为 ARGB；例如 0xFF202020 为不透明深灰色。 */
	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	/** {@inheritDoc} */
	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		if ((backgroundColor >>> 24) != 0) {
			graphics.fill(x, y, x + w, y + h, backgroundColor);
		}
	}


	/**
	 * 创建简单图层。
	 * <p>
	 * Creates a simple layer.
	 *
	 * @param panel 父级UI元素 / Parent UI element
	 */
	public SimpleLayer(UIElement panel) {
		super(panel);
	}

	/** {@inheritDoc} */
	@Override
	public void addUIElements() {

	}

	/** {@inheritDoc} */
	@Override
	public void alignWidgets() {
	}

}
