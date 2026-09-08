/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.frostedresearch.gui.TechIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Virtualized CUI rows. The number of widgets and render work do not grow with the archive.
 */
public final class JournalList extends UIElement {
    public record Row(Object value, Component title, Component subtitle, ItemStack icon, boolean group, int depth) {
        public Row(Object value, Component title, Component subtitle, ItemStack icon, boolean group) {
            this(value, title, subtitle, icon, group, 0);
        }
    }

    private record Label(FormattedCharSequence title, FormattedCharSequence subtitle) {
    }

    private static final int ROW_HEIGHT = 31;
    private List<Row> rows = List.of();
    private final Map<Integer, Label> labels = new HashMap<>();
    private final BiConsumer<Row, Boolean> click;
    private Predicate<Object> selected = value -> false;
    private Object focus;
    private int offset, cacheWidth = -1;
    private boolean selection;
    private Language language;

    public JournalList(UIElement parent, BiConsumer<Row, Boolean> click) {
        super(parent);
        this.click = click;
    }

    public void rows(List<Row> rows) {
        this.rows = List.copyOf(rows);
        labels.clear();
        offset = Math.min(offset, maxOffset());
    }

    public void focus(Object focus) {
        this.focus = focus;
    }

    public void selection(boolean value, Predicate<Object> selected) {
        if (selection != value) labels.clear();
        selection = value;
        this.selected = selected;
    }

    public void top() {
        offset = 0;
    }

    private int maxOffset() {
        return Math.max(0, rows.size() - Math.max(1, getHeight() / ROW_HEIGHT));
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        if (!isMouseOver()) return false;
        offset = Mth.clamp(offset - (int) Math.signum(delta) * 2, 0, maxOffset());
        return true;
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        int i = offset + (int) getMouseY() / ROW_HEIGHT;
        if (i < rows.size())
            click.accept(rows.get(i), selection && getMouseX() >= rows.get(i).depth() * 10 && getMouseX() < 15 + rows.get(i).depth() * 10 && !rows.get(i).group());
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        int i = offset + (int) getMouseY() / ROW_HEIGHT;
        if (i >= 0 && i < rows.size()) {
            tooltip.accept(rows.get(i).title());
            if (!rows.get(i).subtitle().getString().isBlank()) tooltip.accept(rows.get(i).subtitle());
        }
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        if (cacheWidth != w || language != Language.getInstance()) {
            cacheWidth = w;
            language = Language.getInstance();
            labels.clear();
        }
        g.enableScissor(x, y, x + w, y + h);
        int hovered = isMouseOver() ? offset + (int) getMouseY() / ROW_HEIGHT : -1;
        for (int i = offset; i < rows.size() && (i - offset) * ROW_HEIGHT < h; i++) {
            Row row = rows.get(i);
            int ry = y + (i - offset) * ROW_HEIGHT;
            boolean isSelected = Objects.equals(row.value(), focus);
            int indent = row.depth() * 10;
            if (indent > 0) {
                g.fill(x + 5, ry, x + 6, ry + ROW_HEIGHT, KnowledgeUiTheme.MUTED);
                g.fill(x + 5, ry + 14, x + 10, ry + 15, KnowledgeUiTheme.MUTED);
            }
            if (isSelected || i == hovered)
                g.fill(x + indent, ry, x + w - 4, ry + ROW_HEIGHT - 2, isSelected ? KnowledgeUiTheme.PAPER_DARK : 0x36786C57);
            if (isSelected) g.fill(x + indent, ry + 3, x + indent + 2, ry + ROW_HEIGHT - 5, KnowledgeUiTheme.ACCENT);
            int iconX = x + indent + (selection ? 16 : 6), textX = iconX + 21;
            if (selection && !row.group())
                (selected.test(row.value()) ? TechIcons.CHECKBOX_CHECKED : TechIcons.CHECKBOX).draw(g, x + indent + 3, ry + 9, 9, 9);
            if (!row.icon().isEmpty()) g.renderItem(row.icon(), iconX, ry + 6);
            Label label = labels.computeIfAbsent(i, n -> new Label(language.getVisualOrder(getFont().substrByWidth(row.title(), Math.max(1, w - (textX - x) - 7))), language.getVisualOrder(getFont().substrByWidth(row.subtitle(), Math.max(1, w - (textX - x) - 7)))));
            g.drawString(getFont(), label.title(), textX, ry + 5, KnowledgeUiTheme.INK, false);
            g.drawString(getFont(), label.subtitle(), textX, ry + 17, KnowledgeUiTheme.MUTED, false);
            KnowledgeUiTheme.rule(g, x + 5, ry + ROW_HEIGHT - 1, w - 12);
        }
        g.disableScissor();
        if (maxOffset() > 0) {
            int thumb = Math.max(8, h * h / Math.max(h, rows.size() * ROW_HEIGHT)), top = (h - thumb) * offset / maxOffset();
            g.fill(x + w - 2, y + top, x + w - 1, y + top + thumb, KnowledgeUiTheme.MUTED);
        }
    }
}
