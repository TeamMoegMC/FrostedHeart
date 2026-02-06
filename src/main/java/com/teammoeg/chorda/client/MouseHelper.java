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

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;

public class MouseHelper {
    private static final Minecraft MC = Minecraft.getInstance();
    /**
     * 鼠标是否悬停在指定的方形区域内
     * @param mouseX 鼠标坐标 X
     * @param mouseY 鼠标坐标 Y
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param w 宽度
     * @param h 高度
     */
    public static boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h;
    }
    public static boolean isLeftPressed() {
        //MouseHandler.isLeftPressed() 在GUI打开时无效
        return GLFW.glfwGetMouseButton(MC.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    public static boolean isRightPressed() {
        return GLFW.glfwGetMouseButton(MC.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static int leftClicked = 0;
    public static boolean isLeftClicked() {
        if (isLeftPressed()) {
            leftClicked++;
            return leftClicked == 1;
        }
		leftClicked = 0;
		return false;
    }

    private static int rightClicked = 0;
    public static boolean isRightClicked() {
        if (isRightPressed()) {
            rightClicked++;
            return rightClicked == 1;
        }
		rightClicked = 0;
		return false;
    }
    public static double getScaledX() {
        return MC.mouseHandler.xpos() * (double)MC.getWindow().getGuiScaledWidth() / (double)MC.getWindow().getScreenWidth();
    }

    public static double getScaledY() {
        return MC.mouseHandler.ypos() * (double)MC.getWindow().getGuiScaledHeight() / (double)MC.getWindow().getScreenHeight();
    }
}
