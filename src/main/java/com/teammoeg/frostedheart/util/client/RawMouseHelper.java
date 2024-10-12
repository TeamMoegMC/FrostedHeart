package com.teammoeg.frostedheart.util.client;

import net.minecraft.client.Minecraft;

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

    private static int leftClicked = 0;
    public static boolean isLeftClicked() {
        if (MC.mouseHandler.isLeftPressed()) {
            leftClicked++;
            return leftClicked == 1;
        } else {
            leftClicked = 0;
            return false;
        }
    }

    private static int rightClicked = 0;
    public static boolean isRightClicked() {
        if (MC.mouseHandler.isRightPressed()) {
            rightClicked++;
            return rightClicked == 1;
        } else {
            rightClicked = 0;
            return false;
        }
    }

    public static int getScaledX() {
        return (int)(MC.mouseHandler.xpos() * (double)MC.getWindow().getScreenWidth() / (double)MC.getWindow().getWidth());
    }

    public static int getScaledY() {
        return (int)(MC.mouseHandler.ypos() * (double)MC.getWindow().getScreenHeight() / (double)MC.getWindow().getHeight());
    }
}
