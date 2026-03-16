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

package com.teammoeg.chorda.client.cui.screenadapter;

import com.teammoeg.chorda.client.cui.base.PrimaryLayer;

import net.minecraft.client.gui.screens.Screen;

/**
 * CUI屏幕接口。定义获取主层和底层Screen实例的统一访问方式。
 * <p>
 * CUI screen interface. Defines a unified way to access the primary layer and the underlying Screen instance.
 */
public interface CUIScreen {
	/**
	 * 获取主层。
	 * <p>
	 * Gets the primary layer.
	 *
	 * @return 主层实例 / the primary layer instance
	 */
	PrimaryLayer getPrimaryLayer();

	/**
	 * 获取底层的Minecraft Screen实例。
	 * <p>
	 * Gets the underlying Minecraft Screen instance.
	 *
	 * @return Screen实例，如果不是Screen则返回null / the Screen instance, or null if not a Screen
	 */
	Screen getScreen();
}
