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

import com.teammoeg.chorda.client.cui.theme.VanillaTheme;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 城镇界面的「木板底加布告纸」主题：窗口是钉着钉子、积着雪的木板，面板是布告纸，按钮是纸签，进度条带纸槽。
 * 任何 CUI 界面都可以 {@code setTheme(NoticeBoardTheme.INSTANCE)} 选用。
 * <p>
 * The wooden notice-board theme of the town screens: the window is a nailed plank board with snow along its top,
 * panels are notice paper, buttons are paper tags and progress bars sit in a paper track. Any CUI screen may use it
 * with {@code setTheme(NoticeBoardTheme.INSTANCE)}.
 */
public class NoticeBoardTheme extends VanillaTheme {
    public static final NoticeBoardTheme INSTANCE = new NoticeBoardTheme();

    /** Ink on paper. */
    public static final int INK = 0xFF2C2C2A;
    public static final int MUTED = 0xFF5F5E5A;
    public static final int FAINT = 0xFF888780;
    /** Light text on the wooden board. */
    public static final int ON_WOOD = 0xFFFAEEDA;
    public static final int GREEN = 0xFF27500A;
    public static final int GREEN_FILL = 0xFF3B6D11;
    public static final int AMBER = 0xFF854F0B;
    public static final int AMBER_INK = 0xFF633806;
    public static final int RED = 0xFFA32D2D;
    public static final int RED_DARK = 0xFF791F1F;
    public static final int BLUE = 0xFF185FA5;
    public static final int PURPLE = 0xFF534AB7;
    public static final int PURPLE_INK = 0xFF3C3489;
    public static final int NOTE_INK = 0xFF412402;

    protected NoticeBoardTheme() {
    }

    /** The board with its snow line; snow tiles along the top edge. */
    public static void drawBoard(GuiGraphics graphics, int x, int y, int w, int h) {
        NoticeBoardIcons.BOARD.draw(graphics, x, y, w, h);
        for (int sx = 0; sx < w; sx += 64) {
            int part = Math.min(64, w - sx);
            NoticeBoardIcons.BOARD_SNOW.asPart(0, 0, part, 12).draw(graphics, x + sx, y, part, 12);
        }
    }

    @Override
    public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        drawBoard(graphics, x, y, w, h);
    }

    @Override
    public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        NoticeBoardIcons.PAPER.draw(graphics, x, y, w, h);
    }

    @Override
    public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean hover, boolean enabled) {
        (!enabled ? NoticeBoardIcons.TAG_PRESSED : hover ? NoticeBoardIcons.TAG_HOVER : NoticeBoardIcons.TAG).draw(graphics, x, y, w, h);
    }

    @Override
    public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {
        NoticeBoardIcons.PAPER.draw(graphics, x, y, w, h);
        graphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, focused ? AMBER : FAINT);
    }

    /** A cell ruled into the paper: a soft recess with a pencil shadow on its top and left. */
    @Override
    public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {
        int sx = x - 1, sy = y - 1;
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFFE4DFD0);
        graphics.fill(sx, sy, sx + 18, sy + 1, 0xFFB4AE9C);
        graphics.fill(sx, sy, sx + 1, sy + 18, 0xFFB4AE9C);
        graphics.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFF8F5EC);
        graphics.fill(sx + 17, sy + 1, sx + 18, sy + 17, 0xFFF8F5EC);
    }

    /** A scroll track pressed into the paper. */
    @Override
    public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean hover) {
        graphics.fill(x + 3, y, x + w - 3, y + h, 0xFFD9D4C4);
        graphics.fill(x + 3, y, x + 4, y + h, 0xFFB4AE9C);
    }

    /** The scroll thumb is a small paper tag. */
    @Override
    public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean hover) {
        (hover ? NoticeBoardIcons.TAG_HOVER : NoticeBoardIcons.TAG).draw(graphics, x, y, w, h);
    }

    @Override
    public void drawProgressBar(GuiGraphics graphics, int x, int y, int w, int h, float progress, int fillColor) {
        NoticeBoardIcons.BAR_TRACK.draw(graphics, x, y, w, h);
        int filled = Math.round(Math.max(0f, Math.min(1f, progress)) * w);
        if (filled >= 2) NoticeBoardIcons.BAR_FILL.drawTinted(graphics, x, y, filled, h, fillColor);
    }

    @Override
    public int UITextColor() {
        return INK;
    }

    @Override
    public int UIAltTextColor() {
        return MUTED;
    }

    @Override
    public int UIBGColor() {
        return 0xFFF1EFE8;
    }

    @Override
    public int UIBGBorderColor() {
        return 0xFFD3D1C7;
    }

    @Override
    public int buttonTextColor() {
        return INK;
    }

    @Override
    public int buttonTextOverColor() {
        return NOTE_INK;
    }

    @Override
    public int buttonTextDisabledColor() {
        return FAINT;
    }

    @Override
    public int errorColor() {
        return RED;
    }

    @Override
    public int successColor() {
        return GREEN;
    }

    @Override
    public boolean isUITextShadow() {
        return false;
    }

    @Override
    public boolean isButtonTextShadow() {
        return false;
    }
}
