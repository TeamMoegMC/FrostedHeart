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

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 鼠标状态辅助工具类，提供鼠标位置查询、区域悬停检测和点击状态跟踪功能。
 * 支持获取原始坐标、GUI缩放坐标和归一化坐标，以及左右键的按下和单击检测。
 * <p>
 * Mouse state helper utility providing mouse position queries, area hover detection,
 * and click state tracking. Supports raw coordinates, GUI-scaled coordinates, and
 * normalized coordinates, as well as left/right button press and single-click detection.
 */
public class MouseHelper {
    private static final Minecraft MC = Minecraft.getInstance();
    /**
     * 鼠标是否悬停在指定的矩形区域内。
     * <p>
     * Checks whether the mouse is hovering within the specified rectangular area.
     *
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param x 矩形左上角X坐标 / Top-left X coordinate of the rectangle
     * @param y 矩形左上角Y坐标 / Top-left Y coordinate of the rectangle
     * @param w 矩形宽度 / Width of the rectangle
     * @param h 矩形高度 / Height of the rectangle
     * @return 鼠标是否在区域内 / Whether the mouse is within the area
     */
    public static boolean isMouseIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h;
    }
    public static boolean isMouseIn(int x, int y, int w, int h) {
        return isMouseIn(getScaledX(), getScaledY(), x, y, w, h);
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
    public static double getX() {
        return MC.mouseHandler.xpos();
    }

    public static double getY() {
        return MC.mouseHandler.ypos();
    }
    public static double getNormalX() {
        return MC.mouseHandler.xpos()/(double)MC.getWindow().getScreenWidth();
    }

    public static double getNormalY() {
        return MC.mouseHandler.ypos()/(double)MC.getWindow().getScreenHeight();
    }
}
