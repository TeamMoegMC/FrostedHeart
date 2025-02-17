package com.teammoeg.chorda.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class CInputHelper {

	public CInputHelper() {
	}
	public boolean shift(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
	}

	public boolean control(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
	}

	public boolean alt(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
	}

	public boolean start(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
	}

	public boolean numLock(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0;
	}

	public boolean capsLock(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0;
	}
	public boolean onlyControl(int modifiers) {
		return control(modifiers) && !shift(modifiers) && !alt(modifiers);
	}
	public boolean esc(int keyCode) {
		return keyCode==GLFW.GLFW_KEY_ESCAPE;
	}

	public boolean escOrInventory(int keyCode,int scanCode) {
		
		return esc(keyCode) || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode);
	}

	public boolean enter(int keyCode) {
		return keyCode==GLFW.GLFW_KEY_ENTER;
	}

	public boolean backspace(int keyCode) {
		return keyCode==GLFW.GLFW_KEY_BACKSPACE;
	}

	public boolean cut(int keyCode) {
		return Screen.isCut(keyCode);
	}

	public boolean paste(int keyCode) {
		return Screen.isPaste(keyCode);
	}

	public boolean copy(int keyCode) {
		return Screen.isCopy(keyCode);
	}

	public boolean selectAll(int keyCode) {
		return Screen.isSelectAll(keyCode);
	}

	public boolean deselectAll(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_D && Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown();
	}
}
