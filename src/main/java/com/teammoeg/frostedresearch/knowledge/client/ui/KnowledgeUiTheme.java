/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskIcons;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 沿用研究素材的纸张与墨色。 / Existing research paper and ink, with restrained accents.
 */
public final class KnowledgeUiTheme extends DrawDeskTheme {
    public static final KnowledgeUiTheme INSTANCE = new KnowledgeUiTheme();
    public static final int INK = 0xFF302A21, MUTED = 0xFF786C57, ACCENT = 0xFF476B62,
            PAPER = 0xFFE8DDC0, PAPER_DARK = 0xFFD2C3A0, GOLD = 0xFFB39459, RED = 0xFF954E3E;

    private KnowledgeUiTheme() {
    }

    public static void drawPaper(GuiGraphics g, int x, int y, int w, int h) {
        DrawDeskIcons.DIALOG.draw(g, x, y, w, h);
    }

    public static void rule(GuiGraphics g, int x, int y, int w) {
        TechIcons.HLINE.draw(g, x, y, w, 1);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PAPER);
        rule(g, x, y, w);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFBCAC85);
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
    public int buttonTextColor() {
        return INK;
    }

    @Override
    public int buttonTextOverColor() {
        return INK;
    }

    @Override
    public int buttonTextDisabledColor() {
        return 0xFF9F947C;
    }

    @Override
    public void drawButton(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean enabled) {
        // A single nine-slice frame; no 4-pixel tiling loop for every button.
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, hover && enabled ? PAPER : PAPER_DARK);
        TechIcons.BUTTON_FRAME.draw(g, x, y, w, h);
    }

    @Override
    public void drawTextboxBackground(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        g.fill(x, y, x + w, y + h, 0xFFEFE5CA);
        rule(g, x, y + h - 1, w);
        if (focused) g.fill(x, y + h - 1, x + w, y + h, ACCENT);
    }
}
