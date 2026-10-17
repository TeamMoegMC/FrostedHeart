/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * Cached text wrapping with visible-line rendering, without one widget per paragraph.
 */
public final class JournalText extends UIElement {
    private List<Component> paragraphs = List.of();
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private Language language;
    private int wrappedWidth = -1, scroll;
    private int color = KnowledgeUiTheme.INK;

    public JournalText(UIElement parent) {
        super(parent);
    }

    public void text(List<Component> paragraphs) {
        if (!this.paragraphs.equals(paragraphs)) {
            this.paragraphs = List.copyOf(paragraphs);
            wrappedWidth = -1;
            scroll = 0;
        }
    }

    public void color(int color) {
        this.color = color;
    }

    private void wrap() {
        if (wrappedWidth == getWidth() && language == Language.getInstance()) return;
        wrappedWidth = getWidth();
        language = Language.getInstance();
        lines.clear();
        for (Component p : paragraphs) {
            lines.addAll(getFont().split(p, Math.max(1, getWidth() - 7)));
            lines.add(FormattedCharSequence.EMPTY);
        }
        if (!lines.isEmpty()) lines.remove(lines.size() - 1);
        scroll = Math.min(scroll, Math.max(0, lines.size() - getHeight() / 12));
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        if (!isMouseOver()) return false;
        wrap();
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 3, 0, Math.max(0, lines.size() - getHeight() / 12));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        wrap();
        g.enableScissor(x, y, x + w, y + h);
        for (int i = scroll; i < lines.size() && (i - scroll) * 12 < h; i++)
            g.drawString(getFont(), lines.get(i), x, y + (i - scroll) * 12, color, false);
        g.disableScissor();
        int visible = Math.max(1, h / 12);
        if (lines.size() > visible) {
            int thumb = Math.max(8, h * visible / lines.size());
            int top = (h - thumb) * scroll / Math.max(1, lines.size() - visible);
            g.fill(x + w - 2, y, x + w - 1, y + h, KnowledgeUiTheme.PAPER_DARK);
            g.fill(x + w - 2, y + top, x + w - 1, y + top + thumb, KnowledgeUiTheme.MUTED);
        }
    }
}
