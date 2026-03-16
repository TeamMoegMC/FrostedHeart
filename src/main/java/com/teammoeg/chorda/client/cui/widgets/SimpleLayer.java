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
