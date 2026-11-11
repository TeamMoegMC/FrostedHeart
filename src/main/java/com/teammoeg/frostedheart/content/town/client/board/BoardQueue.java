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

package com.teammoeg.frostedheart.content.town.client.board;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 布告板上的排队清单：每一行是一张可以拖动的纸条，右侧有一道纸槽进度和一颗黄铜图钉当滑块。岗位调度和住房页共用。
 * <p>
 * The queue lists on the notice board: each row is a paper strip that can be dragged, with a paper track on the right
 * and a brass pin as its slider knob. Shared by the staffing and housing pages.
 */
public final class BoardQueue {
    public static final int GOOD = NoticeBoardTheme.GREEN_FILL, WARN = 0xFFBA7517, BAD = NoticeBoardTheme.RED,
            EXTRA = NoticeBoardTheme.BLUE, OFF = 0xFFB4B2A9;

    private BoardQueue() {
    }

    /** A paper strip for one row; a dragged strip is lifted with a deeper shadow, a closed building is greyed. */
    public static void strip(GuiGraphics g, int x, int y, int w, int h, boolean lifted, boolean dim) {
        int drop = lifted ? 3 : 1;
        g.fill(x + drop, y + drop + 1, x + w + drop, y + h + drop, lifted ? 0x50000000 : 0x30000000);
        int lift = lifted ? -1 : 0;
        NoticeBoardIcons.PAPER.draw(g, x + lift, y + lift, w, h);
        if (dim) g.fill(x + lift + 2, y + lift + 2, x + lift + w - 2, y + lift + h - 2, 0x40B4B2A9);
        if (lifted) {
            g.fill(x + lift, y + lift, x + lift + w, y + lift + 1, 0xFFFAC775);
            g.fill(x + lift, y + lift + h - 1, x + lift + w, y + lift + h, 0xFFFAC775);
        }
    }

    /** The drag handle: three pencil rules at the left of a strip. */
    public static void handle(GuiGraphics g, int x, int y, boolean dim) {
        int color = dim ? NoticeBoardTheme.FAINT : NoticeBoardTheme.MUTED;
        for (int i = 0; i < 3; i++) g.fill(x, y + i * 3, x + 7, y + i * 3 + 1, color);
    }

    /** An empty paper track. */
    public static void track(GuiGraphics g, int x, int y, int w, int h) {
        NoticeBoardIcons.BAR_TRACK.draw(g, x, y, w, h);
    }

    /** One coloured stretch of a track, from {@code from} to {@code to} pixels. */
    public static void fill(GuiGraphics g, int x, int y, int from, int to, int h, int color) {
        if (to - from < 1) return;
        NoticeBoardIcons.BAR_FILL.drawTinted(g, x + from, y, to - from, h, color);
    }

    /** The slider knob: a brass pin standing on the track at {@code at} pixels. */
    public static void knob(GuiGraphics g, int x, int y, int at, int h, boolean warn) {
        int cx = x + at;
        g.fill(cx, y - 2, cx + 1, y + h + 2, warn ? BAD : NoticeBoardTheme.NOTE_INK);
        // The pin sits on the track, clear of the counts written above it.
        BoardDraw.pin(g, NoticeBoardIcons.PIN_BRASS, cx, y + h / 2);
    }

    /** Where a dragged strip will land: a red pencil line with a small arrow at each end. */
    public static void dropLine(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y - 1, x + w, y + 1, NoticeBoardTheme.RED);
        g.fill(x - 2, y - 3, x, y + 3, NoticeBoardTheme.RED);
        g.fill(x + w, y - 3, x + w + 2, y + 3, NoticeBoardTheme.RED);
    }
}
