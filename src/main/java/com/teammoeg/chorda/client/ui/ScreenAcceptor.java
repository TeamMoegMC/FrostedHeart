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

package com.teammoeg.chorda.client.ui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * 屏幕接收器接口。用于接收和设置容器屏幕引用的组件。
 * <p>
 * Screen acceptor interface. Used by components that need to receive and hold a reference to a container screen.
 */
public interface ScreenAcceptor {

	/**
	 * 设置关联的容器屏幕。
	 * <p>
	 * Sets the associated container screen.
	 *
	 * @param screen 容器屏幕实例 / The container screen instance
	 */
	void setScreen(AbstractContainerScreen screen);

}