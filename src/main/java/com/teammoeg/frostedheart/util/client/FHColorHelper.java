package com.teammoeg.frostedheart.util.client;

public class FHColorHelper {
    public static final int BLACK = 0xFF000000;
    public static final int CYAN = 0xFFC6FCFF;
    public static final int RED = 0xFFFF5340;

    public static int setAlpha (int color, int alpha) {
        return alpha << 24 | color & 0x00FFFFFF;
    }

    public static int setAlpha (int color, float alpha) {
        return setAlpha(color, (int)(alpha*255));
    }
}
