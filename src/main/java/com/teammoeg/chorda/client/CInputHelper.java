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

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

public class CInputHelper {

	public CInputHelper() {
	}
	public static boolean isShift(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
	}

	public static boolean isControl(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
	}

	public static boolean isAlt(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
	}
	public static boolean isEsc(int keyCode) {
		return keyCode==GLFW.GLFW_KEY_ESCAPE;
	}

	public static boolean shouldCloseMenu(int keyCode,int scanCode) {
		return isEsc(keyCode) || ClientUtils.getMc().options.keyInventory.matches(keyCode, scanCode);
	}

	public static boolean isEnter(int keyCode) {
		return keyCode==GLFW.GLFW_KEY_ENTER;
	}

	public static boolean isBackspace(int keyCode) {
		return keyCode==GLFW.GLFW_KEY_BACKSPACE;
	}

	public static boolean isCut(int keyCode) {
		return Screen.isCut(keyCode);
	}

	public static boolean isPaste(int keyCode) {
		return Screen.isPaste(keyCode);
	}

	public static boolean isCopy(int keyCode) {
		return Screen.isCopy(keyCode);
	}

	public static boolean isSelectAll(int keyCode) {
		return Screen.isSelectAll(keyCode);
	}

	public static boolean isDeselectAll(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_D && Screen.hasControlDown();
	}
	public static String getClipboardText() {
		return ClientUtils.getMc().keyboardHandler.getClipboard();
	}

	public static void setClipboardText(String string) {
		ClientUtils.getMc().keyboardHandler.setClipboard(string);
	}
	public static boolean isShiftKeyDown() {
		return Screen.hasShiftDown();
	}

	public static boolean isCtrlKeyDown() {
		return Screen.hasControlDown();
	}
	public static boolean isKeyPressed(int keyCode) {
		return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
	}
	public static boolean isDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
	}
	public static void playClickSound() {
		ClientUtils.getMc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1));
	}
	public static boolean isMouseLeftDown() {
		return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}
	public static boolean isMouseRightDown() {
		return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
	}
	public enum Cursor{
		NORMAL(GLFW.GLFW_ARROW_CURSOR),
		IBEAM(GLFW.GLFW_IBEAM_CURSOR),
		CROSSHAIR(GLFW.GLFW_CROSSHAIR_CURSOR),
		HAND(GLFW.GLFW_POINTING_HAND_CURSOR),
		HRESIZE(GLFW.GLFW_RESIZE_EW_CURSOR),
		VRESIZE(GLFW.GLFW_RESIZE_NS_CURSOR),
		MOVE(GLFW.GLFW_RESIZE_ALL_CURSOR),
		TL_BR_RESIZE(GLFW.GLFW_RESIZE_NWSE_CURSOR),
		TR_BL_RESIZE(GLFW.GLFW_RESIZE_NESW_CURSOR),
		NOT_ALLOWED(GLFW.GLFW_NOT_ALLOWED_CURSOR)
		;
		private final int type;
		private long handle = 0L;

		Cursor(int type) {
			this.type = type;
		}
		public void use() {
			long window = Minecraft.getInstance().getWindow().getWindow();
			
			if (handle == 0) {
				handle = GLFW.glfwCreateStandardCursor(type);
			}

			GLFW.glfwSetCursor(window, handle);
		}
		public static void reset() {
			long window = Minecraft.getInstance().getWindow().getWindow();
			GLFW.glfwSetCursor(window, MemoryUtil.NULL);
		}
	}

}
