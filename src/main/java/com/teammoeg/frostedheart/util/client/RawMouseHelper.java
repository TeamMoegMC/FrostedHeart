package com.teammoeg.frostedheart.util.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class RawMouseHelper {
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
    public static boolean isMouseIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h;
    }
    public static boolean isLeftDown() {
        return GLFW.glfwGetMouseButton(MC.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    public static boolean isRightDown() {
        return GLFW.glfwGetMouseButton(MC.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static int leftClicked = 0;
    public static boolean isLeftClicked() {
        if (isLeftDown()) {
            leftClicked++;
            return leftClicked == 1;
        } else {
            leftClicked = 0;
            return false;
        }
    }

    private static int rightClicked = 0;
    public static boolean isRightClicked() {
        if (isRightDown()) {
            rightClicked++;
            return rightClicked == 1;
        } else {
            rightClicked = 0;
            return false;
        }
    }

    public static int getScaledMouseX() {
        return (int)(MC.mouseHelper.getMouseX() * (double)MC.getMainWindow().getScaledWidth() / (double)MC.getMainWindow().getWidth());
    }

    public static int getScaledMouseY() {
        return (int)(MC.mouseHelper.getMouseY() * (double)MC.getMainWindow().getScaledHeight() / (double)MC.getMainWindow().getHeight());
    }
}
