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

package com.teammoeg.chorda.client.cui;
/**
 * Represent/Rename of glfw mouse
 * order is important, its ordinal would be used to convert button to enum
 * 
 * */
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
}
