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
