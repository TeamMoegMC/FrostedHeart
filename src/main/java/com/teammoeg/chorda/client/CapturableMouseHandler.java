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

package com.teammoeg.chorda.client;

/**
 * 可捕获的鼠标处理器接口，允许在不打开GUI屏幕的情况下捕获鼠标移动。
 * 当鼠标被捕获时，鼠标移动不再控制玩家视角，而是转化为增量值供自定义逻辑使用。
 * 通过Mixin注入到Minecraft的MouseHandler中实现。
 * <p>
 * Capturable mouse handler interface that allows capturing mouse movement without
 * opening a GUI screen. When the mouse is captured, mouse movement no longer controls
 * the player's camera view, but is instead converted to delta values for custom logic.
 * Implemented via Mixin injection into Minecraft's MouseHandler.
 */
public interface CapturableMouseHandler {

	boolean isMouseCaptured();

	void setCaptureMouse(boolean captureMouse);

	double getAndResetCapturedX();

	double getAndResetCapturedY();

}