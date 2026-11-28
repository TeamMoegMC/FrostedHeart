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
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.CSSStylingUtil;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 钉在木板上的一张纸：布告纸、黄便签或方格纸，可带图钉或胶带、轻微倾斜（CUI 变形，内部控件照常可点）和霜。
 * 静态内容用 {@link Painter} 画，可交互的内容作为子元素加入。
 * <p>
 * One sheet pinned to the board: notice paper, a yellow note or grid paper, with a pin or tape, an optional slight
 * tilt (a CUI transform, so child widgets stay clickable) and frost. Static content is drawn by a {@link Painter};
 * interactive content is added as children.
 */
public class PaperCard extends UILayer {
    public enum Kind {
        PAPER, NOTE, GRID
    }

    /** Draws a card's static content inside its bounds. */
    @FunctionalInterface
    public interface Painter {
        void paint(GuiGraphics graphics, int x, int y, int w, int h);
    }

    private Kind kind = Kind.PAPER;
    private CTextureIcon pin;
    private boolean taped;
    private IntSupplier frost = () -> 0;
    private Painter painter;
    private Consumer<PaperCard> content;
    private Supplier<List<Component>> tooltip;
    private Runnable onClick;

    public PaperCard(UIElement parent) {
        super(parent);
        setScissorEnabled(false);
    }

    public PaperCard kind(Kind value) {
        kind = value;
        return this;
    }

    public PaperCard pin(CTextureIcon value) {
        pin = value;
        return this;
    }

    public PaperCard taped() {
        taped = true;
        return this;
    }

    /** Tilts the card around its center; a degree or less looks pinned by hand. */
    public PaperCard tilt(float degrees) {
        setTransform(degrees == 0 ? null : CSSStylingUtil.rotate(degrees));
        return this;
    }

    public PaperCard frost(IntSupplier level) {
        frost = level;
        return this;
    }

    public PaperCard painter(Painter value) {
        painter = value;
        return this;
    }

    /** Adds the card's interactive children whenever it is rebuilt. */
    public PaperCard content(Consumer<PaperCard> value) {
        content = value;
        return this;
    }

    public PaperCard tooltip(Supplier<List<Component>> lines) {
        tooltip = lines;
        return this;
    }

    public PaperCard onClick(Runnable action) {
        onClick = action;
        return this;
    }

    @Override
    public void addUIElements() {
        if (content != null) content.accept(this);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        (kind == Kind.NOTE ? NoticeBoardIcons.NOTE : kind == Kind.GRID ? NoticeBoardIcons.GRID : NoticeBoardIcons.PAPER)
                .draw(graphics, x, y, w, h);
    }

    @Override
    public void beforeDrawElements(GuiGraphics graphics, int parX, int parY, int x, int y, int w, int h) {
        if (painter != null) painter.paint(graphics, parX, parY, w, h);
    }

    @Override
    public void afterDrawElements(GuiGraphics graphics, int parX, int parY, int x, int y, int w, int h) {
        BoardDraw.frost(graphics, parX, parY, w, h, frost.getAsInt());
        if (pin != null) BoardDraw.pin(graphics, pin, parX + w / 2, parY + 1);
        if (taped) {
            BoardDraw.tape(graphics, parX + 6, parY - 4, -8);
            BoardDraw.tape(graphics, parX + w - 30, parY - 4, 6);
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (super.onMousePressed(button)) return true;
        if (onClick != null && isMouseOver() && button == MouseButton.LEFT) {
            CInputHelper.playClickSound();
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public Cursor getCursor() {
        Cursor child = super.getCursor();
        if (child != null) return child;
        return onClick != null && isMouseOver() ? Cursor.HAND : null;
    }

    @Override
    public void getTooltip(TooltipBuilder builder) {
        super.getTooltip(builder);
        if (tooltip != null && isMouseOver() && !isMouseOverAnyWidget())
            tooltip.get().forEach(builder::accept);
    }
}
