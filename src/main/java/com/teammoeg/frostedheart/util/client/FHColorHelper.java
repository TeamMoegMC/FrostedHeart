package com.teammoeg.frostedheart.util.client;

import net.minecraft.util.ColorHelper;
import net.minecraft.util.math.MathHelper;

public class FHColorHelper {
    public static final int CYAN = 0xFFC6FCFF;
    public static final int RED = 0xFFFF5340;

    /**
     * 按照比例混合两个颜色
     * @param ratio 0.0 ~ 1.0
     */
    public static int blendColor(int color1, int color2, float ratio) {
        if (color1 == color2) return color1;
        ratio = MathHelper.clamp(ratio, 0, 1);

        int a = (int)(((color2 >> 24) & 0xFF) * (1-ratio) + ((color1 >> 24) & 0xFF) * ratio);
        int r = (int)(((color2 >> 16) & 0xFF) * (1-ratio) + ((color1 >> 16) & 0xFF) * ratio);
        int g = (int)(((color2 >> 8 ) & 0xFF) * (1-ratio) + ((color1 >> 8 ) & 0xFF) * ratio);
        int b = (int)((color2 & 0xFF) * (1-ratio) + (color1 & 0xFF) * ratio);

        return ColorHelper.PackedColor.packColor(a, r, g, b);
    }
}
