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

@SuppressWarnings("unused")
public class Colors {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int CYAN = 0xFFC6FCFF;
    public static final int RED = 0xFFFF5340;
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
    public static final int L_TEXT_GRAY = 0xFF9294A3;
    public static final int L_BG_GREEN = 0xFFC1E52F;
    public static final int L_BG_GRAY = 0xFF585966;

    public static int themeColor() {
        return setAlpha(FHConfig.CLIENT.themeColor.get(), 1F);
    }

    public static int cyanToTheme(int color) {
        return color == CYAN ? themeColor() : color;
    }

    public static int setAlpha(int color, int alpha) {
        return alpha << 24 | color & 0x00FFFFFF;
    }

    public static int setAlpha(int color, float alpha) {
        return setAlpha(color, (int)(alpha*255));
    }

    /**
     * @param ratio 第一个颜色的比例
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

    public static int makeDark(int color, float darkness) {
        darkness = 1-Mth.clamp(darkness, 0, 1);

        int a = alpha(color);
        int r = (int)(red  (color) * darkness);
        int g = (int)(green(color) * darkness);
        int b = (int)(blue (color) * darkness);

        return pack(a, r, g, b);
    }

    /**
     * @param x Window X
     * @param y Window Y
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

    public static int readableColor(int color) {
        return readableColor(color, 180);
    }

    public static int readableColor(int color, float threshold) {
        return readableColor(color, denormalize(threshold));
    }

    public static int readableColor(int color, int threshold) {
        int r = red  (color);
        int g = green(color);
        int b = blue (color);

        float y = 0.2126F * r + 0.7152F * g + 0.0722F * b;
        return y > threshold ? BLACK : WHITE;
    }

    /**
     * @param value 0 ~ 255
     * @return 0.0 ~ 1.0
     */
    public static float normalize(int value) {
        return Mth.clamp(value, 0, 255) / 255F;
    }

    /**
     * @param value 0.0 ~ 1.0
     * @return 0 ~ 255
     */
    public static int denormalize(float value) {
        return Math.round(Mth.clamp(value, 0, 1) * 255F);
    }

    public static int alpha(int color) {
        return FastColor.ARGB32.alpha(color);
    }

    public static int red(int color) {
        return FastColor.ARGB32.red(color);
    }

    public static int green(int color) {
        return FastColor.ARGB32.green(color);
    }

    public static int blue(int color) {
        return FastColor.ARGB32.blue(color);
    }

    public static int pack(int a, int r, int g, int b) {
        return FastColor.ARGB32.color(a, r, g, b);
    }

    public static String toHexString(int color) {
        return String.format("%08X", color).toUpperCase();
    }
}
