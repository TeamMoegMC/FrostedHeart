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

import java.util.function.BooleanSupplier;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 钉在木板左缘的标签木牌；选中的那块向外抽出。调试页的木牌是红色。
 * A tab plaque nailed to the board's left edge; the selected one sticks out. The debug tab's plaque is red.
 */
public class PlaqueTab extends UIElement {
    private final CIcon icon;
    private final Component title;
    private final BooleanSupplier selected;
    private final Runnable onSelect;
    private final boolean debug;
    private final java.util.function.DoubleSupplier out;

    public PlaqueTab(UIElement parent, CIcon icon, Component title, boolean debug, BooleanSupplier selected, Runnable onSelect) {
        this(parent, icon, title, debug, selected, onSelect, () -> selected.getAsBoolean() ? 1 : 0);
    }

    /** @param out how far the plaque sticks out, from 0 (in line) to 1 (pulled out); see {@link TabSlide} */
    public PlaqueTab(UIElement parent, CIcon icon, Component title, boolean debug, BooleanSupplier selected, Runnable onSelect,
                     java.util.function.DoubleSupplier out) {
        super(parent);
        this.icon = icon;
        this.title = title;
        this.debug = debug;
        this.selected = selected;
        this.onSelect = onSelect;
        this.out = out;
        setSize(28, 24);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        boolean on = selected.getAsBoolean();
        int shift = Math.round(4 * (1 - (float) Math.max(0, Math.min(1, out.getAsDouble()))));
        (debug ? NoticeBoardIcons.PLAQUE_DEBUG : on ? NoticeBoardIcons.PLAQUE_SELECTED : NoticeBoardIcons.PLAQUE)
                .draw(g, x + shift, y, w - shift + 4, h);
        icon.draw(g, x + shift + (w - shift - 16) / 2 + 1, y + 4, 16, 16);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (!selected.getAsBoolean()) {
            CInputHelper.playUISound(SoundEvents.BOOK_PAGE_TURN, 1.0f);
            onSelect.run();
        }
        return true;
    }

    @Override
    public Cursor getCursor() {
        return isMouseOver() ? Cursor.HAND : null;
    }

    @Override
    public void getTooltip(TooltipBuilder builder) {
        if (isMouseOver()) builder.accept(title);
    }
}
