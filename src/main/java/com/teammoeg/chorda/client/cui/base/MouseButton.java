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

package com.teammoeg.chorda.client.cui.base;
/**
 * GLFW鼠标按钮的枚举封装。枚举顺序与GLFW按钮编号一致，
 * 其ordinal值用于从整数按钮编号转换为枚举值。
 * <p>
 * Enum wrapper for GLFW mouse buttons. The enum order matches GLFW button numbers;
 * the ordinal value is used to convert integer button numbers to enum values.
 */
public enum MouseButton {
	LEFT,
	RIGHT,
	MIDDLE,
	PREV,
	NEXT,
	BTN_5,
	BTN_6,
	BTN_7,
	BTN_8,
	BTN_9,
	BTN_10,
	BTN_11,
	BTN_12,
	BTN_13,
	BTN_14,
	BTN_15,
	ERROR;
	public static MouseButton of(int num) {
		return MouseButton.values().length>num?MouseButton.values()[num]:ERROR;
	}

	public boolean is(MouseButton button) {
		return this == button;
	}
}
