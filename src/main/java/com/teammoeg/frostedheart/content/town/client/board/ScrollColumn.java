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

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 竖排的一列卡片，内容超高时可以用滚轮滚动，右侧画一条细滚动条。滚动位置交给界面保存，重建后不丢。
 * <p>
 * A vertical column of cards that scrolls with the wheel when its content is taller than itself, with a thin
 * scrollbar on the right. The scroll position is handed back to the screen so rebuilds keep it.
 */
public class ScrollColumn extends UILayer {
    private final Consumer<ScrollColumn> content;
    private final IntConsumer onScroll;
    private final int gap;
    private int scroll;

    public ScrollColumn(UIElement parent, int gap, int scroll, Consumer<ScrollColumn> content, IntConsumer onScroll) {
        super(parent);
        this.gap = gap;
        this.scroll = scroll;
        this.content = content;
        this.onScroll = onScroll;
    }

    @Override
    public void addUIElements() {
        content.accept(this);
    }

    @Override
    public void alignWidgets() {
        align(4, gap, false);
        clampScroll();
    }

    private int overflow() {
        return Math.max(0, getContentHeight() + 8 - getHeight());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, overflow()));
        setOffsetY(-scroll);
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (super.onMouseScrolled(amount)) return true;
        if (!isMouseOver() || overflow() == 0) return false;
        scroll -= (int) Math.round(amount * 16);
        clampScroll();
        onScroll.accept(scroll);
        return true;
    }

    @Override
    public void afterDrawElements(GuiGraphics graphics, int parX, int parY, int x, int y, int w, int h) {
        int over = overflow();
        if (over <= 0) return;
        int track = h - 4;
        int thumb = Math.max(12, track * h / (h + over));
        int top = parY + 2 + (track - thumb) * scroll / over;
        graphics.fill(parX + w - 3, parY + 2, parX + w - 1, parY + 2 + track, 0x40000000);
        graphics.fill(parX + w - 3, top, parX + w - 1, top + thumb, 0xFFFAEEDA);
    }
}
