/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.knowledge;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Flat, high-contrast button used by the paper archive UI. */
final class KnowledgeLabButton extends UIElement {
    private static final int INK = 0xFF27231D;
    private static final int MUTED = 0xFF746B5C;
    private static final int PAPER = 0xFFF7EFD9;
    private static final int PAPER_HOVER = 0xFFFFF8E8;
    private static final int SELECTED = 0xFFD0C29E;
    private static final int BORDER = 0xFF8E8068;
    private Component title;
    private final Consumer<MouseButton> callback;
    private boolean selected;
    private boolean centered;

    KnowledgeLabButton(UIElement parent, Component title, Consumer<MouseButton> callback) {
        super(parent);
        this.title = title;
        this.callback = callback;
        setSize(80, 18);
    }

    KnowledgeLabButton setText(Component replacement) {
        title = replacement;
        return this;
    }

    KnowledgeLabButton setSelected(boolean value) {
        selected = value;
        return this;
    }

    KnowledgeLabButton centered() {
        centered = true;
        return this;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (getFont().width(title) > Math.max(0, getWidth() - 10)) tooltip.accept(title);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        int fill = !isEnabled() ? 0xFFDDD2BA : selected ? SELECTED : isMouseOver() ? PAPER_HOVER : PAPER;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
        int available = Math.max(0, width - 10);
        String clipped = getFont().plainSubstrByWidth(title.getString(), available);
        int textWidth = getFont().width(clipped);
        int textX = centered ? x + Math.max(4, (width - textWidth) / 2) : x + 5;
        graphics.drawString(getFont(), clipped, textX, y + Math.max(3, (height - getFont().lineHeight) / 2),
                isEnabled() ? INK : MUTED, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;
        if (isEnabled()) {
            CInputHelper.playClickSound();
            callback.accept(button);
        }
        return true;
    }

    @Override
    public CInputHelper.Cursor getCursor() {
        return CInputHelper.Cursor.HAND;
    }
}
