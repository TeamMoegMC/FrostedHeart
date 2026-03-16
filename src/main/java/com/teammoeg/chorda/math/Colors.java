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

package com.teammoeg.chorda.math;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

/**
 * 颜色操作和转换工具类，提供ARGB颜色的混合、提取、归一化等功能。
 * <p>
 * Color manipulation and conversion utility class, providing ARGB color blending, extraction,
 * normalization and other operations.
 */
@SuppressWarnings("unused")
public class Colors {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int CYAN = 0xFFC6FCFF;
    public static final int RED = 0xFFFF5340;
    /**
     * Minecraft聊天颜色常量集合，对应Minecraft的16种聊天文字颜色。
     * <p>
     * Collection of Minecraft chat color constants, corresponding to Minecraft's 16 chat text colors.
     */
    public static final class ChatColors{
    	public static final int BLACK			= 0xff000000;
    	public static final int DARK_BLUE		= 0xff0000aa;
    	public static final int DARK_GREEN		= 0xff00aa00;
    	public static final int DARK_AQUA		= 0xff00aaaa;
    	public static final int DARK_RED		= 0xffaa0000;
    	public static final int DARK_PURPLE		= 0xffaa00aa;
    	public static final int GOLD			= 0xffffaa00;
    	public static final int GRAY			= 0xffaaaaaa;
    	public static final int DARK_GRAY		= 0xff555555;
    	public static final int BLUE			= 0xff5555ff;
    	public static final int GREEN			= 0xff55ff55;
    	public static final int AQUA			= 0xff55ffff;
    	public static final int RED				= 0xffff5555;
    	public static final int LIGHT_PURPLE	= 0xffff55ff;
    	public static final int YELLOW			= 0xffffff55;
    	public static final int WHITE			= 0xffffffff;
    	private ChatColors() {}
    }

    /**
     * 获取当前配置的主题颜色。
     * <p>
     * Gets the currently configured theme color.
     *
     * @return 带完整alpha的主题颜色 / the theme color with full alpha
     */
    public static int themeColor() {
        return setAlpha(FHConfig.CLIENT.themeColor.get(), 1F);
    }

    /**
     * 将青色替换为主题颜色，其他颜色保持不变。
     * <p>
     * Replaces cyan with the theme color, leaving other colors unchanged.
     *
     * @param color 输入颜色 / the input color
     * @return 如果是青色则返回主题颜色，否则返回原色 / theme color if input is cyan, otherwise the original color
     */
    public static int cyanToTheme(int color) {
        return color == CYAN ? themeColor() : color;
    }

    /**
     * 设置颜色的alpha通道值（整数形式）。
     * <p>
     * Sets the alpha channel of a color using an integer value.
     *
     * @param color 原始颜色 / the original color
     * @param alpha alpha值 (0-255) / alpha value (0-255)
     * @return 带新alpha值的颜色 / the color with the new alpha value
     */
    public static int setAlpha(int color, int alpha) {
        return alpha << 24 | color & 0x00FFFFFF;
    }

    /**
     * 设置颜色的alpha通道值（浮点形式）。
     * <p>
     * Sets the alpha channel of a color using a float value.
     *
     * @param color 原始颜色 / the original color
     * @param alpha alpha值 (0.0-1.0) / alpha value (0.0-1.0)
     * @return 带新alpha值的颜色 / the color with the new alpha value
     */
    public static int setAlpha(int color, float alpha) {
        return setAlpha(color, (int)(alpha*255));
    }

    /**
     * 按指定比例混合两种颜色。
     * <p>
     * Blends two colors by the specified ratio.
     *
     * @param color1 第一种颜色 / the first color
     * @param color2 第二种颜色 / the second color
     * @param ratio 第一个颜色的比例 (0.0-1.0) / the ratio of the first color (0.0-1.0)
     * @return 混合后的颜色 / the blended color
     */
    public static int blend(int color1, int color2, float ratio) {
        if (color1 == color2) return color1;
        ratio = Mth.clamp(ratio, 0, 1);

        int a = (int)(alpha(color2) * (1-ratio) + alpha(color1) * ratio);
        int r = (int)(red  (color2) * (1-ratio) + red  (color1) * ratio);
        int g = (int)(green(color2) * (1-ratio) + green(color1) * ratio);
        int b = (int)(blue (color2) * (1-ratio) + blue (color1) * ratio);

        return FastColor.ARGB32.color(a, r, g, b);
    }

    /**
     * 将颜色变暗指定程度。
     * <p>
     * Darkens a color by the specified amount.
     *
     * @param color 原始颜色 / the original color
     * @param darkness 变暗程度 (0.0=不变, 1.0=全黑) / darkness amount (0.0=unchanged, 1.0=fully black)
     * @return 变暗后的颜色 / the darkened color
     */
    public static int makeDark(int color, float darkness) {
        darkness = 1-Mth.clamp(darkness, 0, 1);

        int a = alpha(color);
        int r = (int)(red  (color) * darkness);
        int g = (int)(green(color) * darkness);
        int b = (int)(blue (color) * darkness);

        return pack(a, r, g, b);
    }

    /**
     * 从OpenGL帧缓冲区读取指定窗口坐标处的像素颜色。
     * <p>
     * Reads the pixel color at the specified window coordinates from the OpenGL framebuffer.
     *
     * @param x 窗口X坐标 / window X coordinate
     * @param y 窗口Y坐标 / window Y coordinate
     * @return 该位置的ARGB颜色值 / the ARGB color at that position
     */
    public static int getColorAt(int x, int y) {
        int windowHeight = ClientUtils.getMc().getWindow().getHeight();
        y = windowHeight - y - 1;

        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        int r = buffer.get(0) & 0xFF;
        int g = buffer.get(1) & 0xFF;
        int b = buffer.get(2) & 0xFF;
        int a = buffer.get(3) & 0xFF;

        return pack(a, r, g, b);
    }

    /**
     * 根据背景颜色亮度返回可读的前景颜色（黑色或白色），使用默认阈值180。
     * <p>
     * Returns a readable foreground color (black or white) based on background luminance, using default threshold 180.
     *
     * @param color 背景颜色 / the background color
     * @return 黑色或白色 / black or white
     */
    public static int readableColor(int color) {
        return readableColor(color, 180);
    }

    /**
     * 根据背景颜色亮度返回可读的前景颜色（黑色或白色），使用浮点阈值。
     * <p>
     * Returns a readable foreground color (black or white) based on background luminance, using a float threshold.
     *
     * @param color 背景颜色 / the background color
     * @param threshold 亮度阈值（浮点） / luminance threshold (float)
     * @return 黑色或白色 / black or white
     */
    public static int readableColor(int color, float threshold) {
        return readableColor(color, denormalize(threshold));
    }

    /**
     * 根据背景颜色亮度返回可读的前景颜色（黑色或白色），使用整数阈值。
     * <p>
     * Returns a readable foreground color (black or white) based on background luminance, using an integer threshold.
     *
     * @param color 背景颜色 / the background color
     * @param threshold 亮度阈值 (0-255) / luminance threshold (0-255)
     * @return 黑色或白色 / black or white
     */
    public static int readableColor(int color, int threshold) {
        int r = red  (color);
        int g = green(color);
        int b = blue (color);

        float y = 0.2126F * r + 0.7152F * g + 0.0722F * b;
        return y > threshold ? BLACK : WHITE;
    }

    /**
     * 将颜色分量值从整数范围归一化到浮点范围。
     * <p>
     * Normalizes a color component value from integer range to float range.
     *
     * @param value 整数颜色分量 (0-255) / integer color component (0-255)
     * @return 归一化后的浮点值 (0.0-1.0) / normalized float value (0.0-1.0)
     */
    public static float normalize(int value) {
        return Mth.clamp(value, 0, 255) / 255F;
    }

    /**
     * 将颜色分量值从浮点范围反归一化到整数范围。
     * <p>
     * Denormalizes a color component value from float range to integer range.
     *
     * @param value 浮点颜色分量 (0.0-1.0) / float color component (0.0-1.0)
     * @return 反归一化后的整数值 (0-255) / denormalized integer value (0-255)
     */
    public static int denormalize(float value) {
        return Math.round(Mth.clamp(value, 0, 1) * 255F);
    }

    /**
     * 提取颜色的alpha分量。
     * <p>
     * Extracts the alpha component of a color.
     *
     * @param color ARGB颜色值 / the ARGB color value
     * @return alpha分量 (0-255) / the alpha component (0-255)
     */
    public static int alpha(int color) {
        return FastColor.ARGB32.alpha(color);
    }

    /**
     * 提取颜色的红色分量。
     * <p>
     * Extracts the red component of a color.
     *
     * @param color ARGB颜色值 / the ARGB color value
     * @return 红色分量 (0-255) / the red component (0-255)
     */
    public static int red(int color) {
        return FastColor.ARGB32.red(color);
    }

    /**
     * 提取颜色的绿色分量。
     * <p>
     * Extracts the green component of a color.
     *
     * @param color ARGB颜色值 / the ARGB color value
     * @return 绿色分量 (0-255) / the green component (0-255)
     */
    public static int green(int color) {
        return FastColor.ARGB32.green(color);
    }

    /**
     * 提取颜色的蓝色分量。
     * <p>
     * Extracts the blue component of a color.
     *
     * @param color ARGB颜色值 / the ARGB color value
     * @return 蓝色分量 (0-255) / the blue component (0-255)
     */
    public static int blue(int color) {
        return FastColor.ARGB32.blue(color);
    }

    /**
     * 将ARGB分量打包为单个整数颜色值。
     * <p>
     * Packs ARGB components into a single integer color value.
     *
     * @param a alpha分量 / the alpha component
     * @param r 红色分量 / the red component
     * @param g 绿色分量 / the green component
     * @param b 蓝色分量 / the blue component
     * @return 打包后的ARGB颜色值 / the packed ARGB color value
     */
    public static int pack(int a, int r, int g, int b) {
        return FastColor.ARGB32.color(a, r, g, b);
    }

    /**
     * 将颜色值转换为8位十六进制字符串。
     * <p>
     * Converts a color value to an 8-character uppercase hexadecimal string.
     *
     * @param color 颜色值 / the color value
     * @return 十六进制颜色字符串 / the hexadecimal color string
     */
    public static String toHexString(int color) {
        return String.format("%08X", color).toUpperCase();
    }

    /**
     * 从十六进制字符串解析颜色值，解析失败时返回备用值。
     * <p>
     * Parses a color value from a hexadecimal string, returning the fallback on parse failure.
     *
     * @param color 十六进制颜色字符串 / the hexadecimal color string
     * @param fallback 解析失败时的备用颜色 / the fallback color on parse failure
     * @return 解析出的颜色值或备用值 / the parsed color value or fallback
     */
    public static int getColor(String color, int fallback) {
        try {
            return Integer.parseUnsignedInt(color, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
