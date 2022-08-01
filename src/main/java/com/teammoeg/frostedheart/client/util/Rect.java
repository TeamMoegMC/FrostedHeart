package com.teammoeg.frostedheart.client.util;

public class Rect extends Point {
    protected final int w, h;

    public static Rect delta(int x1, int y1, int x2, int y2) {
        return new Rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public Rect(Rect r) {
        this(r.x, r.y, r.w, r.h);
    }

    public Rect(int x, int y, int w, int h) {
        super(x, y);
        this.w = w;
        this.h = h;
    }
}
