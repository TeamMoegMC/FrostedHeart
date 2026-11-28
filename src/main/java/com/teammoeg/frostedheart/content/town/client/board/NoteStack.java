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
import java.util.Set;
import java.util.function.IntConsumer;

import com.mojang.math.Axis;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Note;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.NoteKind;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 一叠可以翻的便签：最上面一张是最重要的改进，点击、滚轮像掀纸一样翻到下一张；不自动轮转。
 * 上次打开后才出现的问题盖一个「新」印，翻过一次后消失。
 * <p>
 * A stack of notes the player flips through: the top one is the most important improvement; a click or the wheel
 * lifts it like a sheet of paper to show the next. It never rotates by itself. A problem that appeared since the
 * last visit carries a red 新 stamp until it is flipped past once.
 */
public class NoteStack extends UIElement {
    private static final long FLIP_MS = 220, HINT_AT_MS = 2000, HINT_MS = 500;
    private final List<Note> notes;
    private final Set<String> fresh;
    private final IntConsumer onIndex;
    private final IntConsumer onSeen;
    private int index;
    private long flipStart = -1;
    private int flipDirection = 1;
    private final long openedAt = System.currentTimeMillis();

    /**
     * @param fresh   新出现的问题种类 / problem kinds new since the last visit
     * @param onIndex 记住翻到第几张，界面重建后接着显示 / remembers the shown note across rebuilds
     * @param onSeen  某张便签被翻过 / a note has been flipped past
     */
    public NoteStack(UIElement parent, List<Note> notes, int index, Set<String> fresh, IntConsumer onIndex, IntConsumer onSeen) {
        super(parent);
        this.notes = notes;
        this.fresh = fresh;
        this.onIndex = onIndex;
        this.onSeen = onSeen;
        this.index = notes.isEmpty() ? 0 : Math.floorMod(index, notes.size());
    }

    private Note note(int offset) {
        return notes.get(Math.floorMod(index + offset, notes.size()));
    }

    private void flip(int direction) {
        if (notes.size() < 2 || flipStart >= 0) return;
        flipStart = System.currentTimeMillis();
        flipDirection = direction;
        CInputHelper.playUISound(SoundEvents.BOOK_PAGE_TURN, 1.4f);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        if (notes.isEmpty()) return;
        long now = System.currentTimeMillis();
        float progress = 0;
        if (flipStart >= 0) {
            progress = (now - flipStart) / (float) FLIP_MS;
            if (progress >= 1) {
                onSeen.accept(index);
                index = Math.floorMod(index + flipDirection, notes.size());
                onIndex.accept(index);
                flipStart = -1;
                progress = 0;
            }
        }
        if (notes.size() > 2) sheet(g, note(2), x + 5, y + 6, w, h, 2.2f);
        if (notes.size() > 1) sheet(g, note(1), x - 3, y + 3, w, h, -1.6f);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f + progress * 12, y + h / 2f - progress * 34, 10);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-0.8f - progress * 9));
        g.pose().translate(-w / 2f, -h / 2f, 0);
        drawNote(g, note(0), 0, 0, w, h, now);
        g.pose().popPose();
    }

    private static void sheet(GuiGraphics g, Note note, int x, int y, int w, int h, float degrees) {
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        (note.kind() == NoteKind.IMPROVE ? NoticeBoardIcons.NOTE : NoticeBoardIcons.PAPER).draw(g, -w / 2, -h / 2, w, h);
        g.pose().popPose();
    }

    private void drawNote(GuiGraphics g, Note note, int x, int y, int w, int h, long now) {
        boolean improve = note.kind() == NoteKind.IMPROVE;
        (improve ? NoticeBoardIcons.NOTE : NoticeBoardIcons.PAPER).draw(g, x, y, w, h);
        BoardDraw.tape(g, x + 8, y - 4, -8);
        BoardDraw.tape(g, x + w - 32, y - 4, 6);
        int ink = improve ? NoticeBoardTheme.NOTE_INK : NoticeBoardTheme.INK;
        int sub = improve ? NoticeBoardTheme.AMBER : NoticeBoardTheme.MUTED;
        int cx = x + 8, cy = y + 8, cw = w - 16;
        Component tag = Component.translatable(TownFormat.KEY + (note.blocking() ? "note.fix" : improve ? "note.improve" : "note.talk"));
        int tagWidth = BoardDraw.chip(g, tag, cx, cy,
                note.blocking() ? NoticeBoardTheme.RED : improve ? NoticeBoardTheme.AMBER_INK : NoticeBoardTheme.MUTED);
        if (improve && fresh.contains(note.problem()))
            BoardDraw.stamp(g, Component.translatable(TownFormat.KEY + "note.new"), cx + tagWidth + 10, cy + 5, NoticeBoardTheme.RED, false, -10, 1);
        BoardDraw.textRight(g, Component.translatable(TownFormat.KEY + "note.counter", index + 1, notes.size()), x + w - 8, cy + 2, sub);
        int ty = cy + 16;
        int room = h - 16 - 30;
        int lines = Math.max(1, room / BoardDraw.LINE);
        ty += BoardDraw.wrapped(g, note.line(), cx, ty, cw, ink, lines);
        if (improve && !note.action().getString().isEmpty()) {
            Component action = BoardWidgets.arrow(note.action()).copy().withStyle(ChatFormatting.BOLD);
            BoardDraw.wrapped(g, action, cx, ty + 3, cw, ink, 3);
        }
        BoardDraw.textRight(g, note.signature(), x + w - 18, y + h - 16, sub);
        CTextureIcon corner = improve ? NoticeBoardIcons.CORNER_NOTE : NoticeBoardIcons.CORNER_PAPER;
        long sinceOpen = now - openedAt;
        int lift = sinceOpen > HINT_AT_MS && sinceOpen < HINT_AT_MS + HINT_MS && notes.size() > 1
                ? (int) Math.round(Math.sin((sinceOpen - HINT_AT_MS) / (double) HINT_MS * Math.PI) * 4) : 0;
        corner.draw(g, x + w - 12 - lift, y + h - 12 - lift, 12 + lift, 12 + lift);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;
        flip(button == MouseButton.RIGHT ? -1 : 1);
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        flip(scroll > 0 ? -1 : 1);
        return true;
    }

    @Override
    public Cursor getCursor() {
        return notes.size() > 1 && isMouseOver() ? Cursor.HAND : null;
    }
}
