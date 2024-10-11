package com.teammoeg.frostedheart.util.client;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class FHColorHelper {
    public static final int BLACK = 0xFF000000;
    public static final int CYAN = 0xFFC6FCFF;
    public static final int RED = 0xFFFF5340;

    /**
     * 按照比例混合两个颜色
     * @param ratio 0.0 ~ 1.0
     */
    public static int blendColor(int color1, int color2, float ratio) {
        if (color1 == color2) return color1;
        ratio = Mth.clamp(ratio, 0, 1);

        int a = (int)(FastColor.ARGB32.alpha(color2) * (1-ratio) + FastColor.ARGB32.alpha(color1) * ratio);
        int r = (int)(FastColor.ARGB32.red  (color2) * (1-ratio) + FastColor.ARGB32.red  (color1) * ratio);
        int g = (int)(FastColor.ARGB32.green(color2) * (1-ratio) + FastColor.ARGB32.green(color1) * ratio);
        int b = (int)(FastColor.ARGB32.blue (color2) * (1-ratio) + FastColor.ARGB32.blue (color1) * ratio);

        return FastColor.ARGB32.color(a, r, g, b);
    }
}
