/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.client.ui;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

@SuppressWarnings("unused")
public class ColorHelper {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int CYAN = 0xFFC6FCFF;
    public static final int RED = 0xFFFF5340;

    public static final int L_TEXT_GRAY = 0xFF9294A3;
    public static final int L_BG_GREEN = 0xFFC1E52F;
    public static final int L_BG_GRAY = 0xFF585966;

    public static int setAlpha(int color, int alpha) {
        return alpha << 24 | color & 0x00FFFFFF;
    }

    public static int setAlpha(int color, float alpha) {
        return setAlpha(color, (int)(alpha*255));
    }

    public static int blendColor(int color1, int color2, float ratio) {
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

    public static int getTextColor(int backgroundColor) {
        int r = red  (backgroundColor);
        int g = green(backgroundColor);
        int b = blue (backgroundColor);

        float y = (0.2126F * r + 0.7152F * g + 0.0722F * b) / 255;
        float contrastWhite = 1.05F / (y + 0.05F);
        float contrastBlack = (y + 0.05F) / 0.05F;
        return contrastBlack > contrastWhite ? BLACK : WHITE;
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
