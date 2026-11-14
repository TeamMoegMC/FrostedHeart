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

import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 一块看不见的热区：悬停给提示，点击执行动作，可选地画内容。用来给画出来的文字、标签和数字加上交互。
 * <p>
 * An invisible region that shows a tooltip on hover and runs an action on click, optionally drawing content. It
 * makes drawn text, chips and figures interactive.
 */
public class HitArea extends UIElement {
    private Supplier<List<Component>> tooltip = List::of;
    private Runnable onClick;
    private PaperCard.Painter painter;

    public HitArea(UIElement parent, int x, int y, int w, int h) {
        super(parent);
        setPosAndSize(x, y, w, h);
    }

    public HitArea tooltip(Supplier<List<Component>> lines) {
        tooltip = lines == null ? List::of : lines;
        return this;
    }

    public HitArea tooltip(List<Component> lines) {
        return tooltip(() -> lines);
    }

    public HitArea onClick(Runnable action) {
        onClick = action;
        return this;
    }

    public HitArea painter(PaperCard.Painter value) {
        painter = value;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        if (painter != null) painter.paint(graphics, x, y, w, h);
        if (onClick != null && isMouseOver()) graphics.fill(x, y + h - 1, x + w, y + h, 0x40412402);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (onClick != null && isMouseOver() && button == MouseButton.LEFT) {
            CInputHelper.playClickSound();
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public Cursor getCursor() {
        return onClick != null && isMouseOver() ? Cursor.HAND : null;
    }

    @Override
    public void getTooltip(TooltipBuilder builder) {
        if (isMouseOver()) tooltip.get().forEach(builder::accept);
    }
}
