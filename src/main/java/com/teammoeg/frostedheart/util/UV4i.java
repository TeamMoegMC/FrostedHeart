package com.teammoeg.frostedheart.util;

public class UV4i {
    public final int x, y, w, h;
    public UV4i(int x1, int y1, int x2, int y2) {
        this.x = x1;
        this.y = y1;
        this.w = x2 - x1;
        this.h = y2 - y1;
    }
}
