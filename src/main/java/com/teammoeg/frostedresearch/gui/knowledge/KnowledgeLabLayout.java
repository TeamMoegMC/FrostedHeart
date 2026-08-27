/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.knowledge;

/** Single source of truth for Knowledge Lab rendering and mouse-hit geometry. */
public record KnowledgeLabLayout(Bounds header, Bounds list, Bounds detail, Bounds context) {
    public static KnowledgeLabLayout calculate(int width, int height) {
        int outer = 6;
        int gap = 6;
        int bodyTop = 38;
        int bodyHeight = Math.max(40, height - bodyTop - outer);
        int usable = Math.max(120, width - outer * 2 - gap * 2);
        int listWidth;
        int contextWidth;
        if (usable < 360) {
            listWidth = Math.max(54, usable * 28 / 100);
            contextWidth = Math.max(58, usable * 27 / 100);
        } else {
            listWidth = clamp(usable * 28 / 100, 128, 220);
            contextWidth = clamp(usable * 27 / 100, 132, 210);
        }
        int detailWidth = usable - listWidth - contextWidth;
        Bounds list = new Bounds(outer, bodyTop, listWidth, bodyHeight);
        Bounds detail = new Bounds(list.right() + gap, bodyTop, detailWidth, bodyHeight);
        Bounds context = new Bounds(detail.right() + gap, bodyTop, contextWidth, bodyHeight);
        return new KnowledgeLabLayout(new Bounds(0, 0, width, 33), list, detail, context);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Bounds(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
