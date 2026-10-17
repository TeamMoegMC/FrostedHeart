/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

/**
 * Small CUI button; its clipped label is cached until width, text or language changes.
 */
public final class JournalButton extends UIElement {
    private Component label;
    private final Runnable action;
    private Component tooltip;
    private final boolean quiet;
    private boolean selected;
    private int measuredWidth = -1;
    private Language language;
    private FormattedCharSequence fitted = FormattedCharSequence.EMPTY;
    private int textWidth;

    public JournalButton(UIElement parent, Component label, boolean quiet, Runnable action) {
        super(parent);
        this.label = label;
        this.quiet = quiet;
        this.action = action;
    }

    public JournalButton label(Component value) {
        if (!label.equals(value)) {
            label = value;
            measuredWidth = -1;
        }
        return this;
    }

    public JournalButton selected(boolean value) {
        selected = value;
        return this;
    }

    public JournalButton tooltip(Component value) {
        tooltip = value;
        return this;
    }

    @Override
    public boolean hasTooltip() {
        return isVisible() && isMouseOver() && (tooltip != null || getFont().width(label) > getWidth() - 10);
    }

    @Override
    public Component getTitle() {
        return label;
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        if (tooltip != null) list.accept(tooltip);
        else if (getFont().width(label) > getWidth() - 10) list.accept(label);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || !isEnabled() || button != MouseButton.LEFT) return false;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
        action.run();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        if (measuredWidth != w || language != Language.getInstance()) {
            language = Language.getInstance();
            measuredWidth = w;
            var clipped = getFont().substrByWidth(label, Math.max(1, w - 10));
            fitted = language.getVisualOrder(clipped);
            textWidth = getFont().width(clipped);
        }
        if (!quiet) KnowledgeUiTheme.INSTANCE.drawButton(g, x, y, w, h, isMouseOver(), isEnabled());
        else if (selected || isMouseOver() && isEnabled())
            g.fill(x, y, x + w, y + h, selected ? KnowledgeUiTheme.PAPER_DARK : 0x44786C57);
        if (selected) g.fill(x + 3, y + h - 2, x + w - 3, y + h - 1, KnowledgeUiTheme.ACCENT);
        g.drawString(getFont(), fitted, x + (w - textWidth) / 2, y + (h - getFont().lineHeight) / 2,
                !isEnabled() ? 0xFF9F947C : selected ? KnowledgeUiTheme.ACCENT : KnowledgeUiTheme.INK, false);
    }
}
