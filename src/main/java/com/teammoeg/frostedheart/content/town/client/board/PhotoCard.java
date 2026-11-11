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

import com.mojang.math.Axis;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Duty;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Person;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 钉在板上的居民照片：按居民生成的像素人像、名字；不在岗时盖「休养」或「歇工」印，住处冷时照片结霜。
 * <p>
 * A resident's photo pinned to the board: a pixel portrait generated for the resident, and the name. A resident off duty
 * carries a 休养 or 歇工 stamp; a photo frosts over when the resident's home is cold.
 */
public class PhotoCard extends UIElement {
    private static final float MIN_NAME_SCALE = 0.7f;
    private final Person person;
    private final BooleanSupplier selected;
    private final Runnable onClick;
    private final float tilt;

    public PhotoCard(UIElement parent, Person person, float tilt, BooleanSupplier selected, Runnable onClick) {
        super(parent);
        this.person = person;
        this.tilt = tilt;
        this.selected = selected;
        this.onClick = onClick;
    }

    private String caption;

    /** The name written under the photo; the first name unless set. */
    public PhotoCard caption(String caption) {
        this.caption = caption;
        return this;
    }

    /**
     * 照片下的名字：一般只写名；同一面墙上有人同名时，加上姓的首字母（Jesse S.），免得认错人。
     * <p>
     * The name under a photo: the first name, with the surname's initial when someone else on the same wall shares it,
     * so two Jesses are not mistaken for each other.
     */
    public static String captionFor(Person person, java.util.List<Person> wall) {
        String first = firstOf(person.name());
        for (Person other : wall) {
            if (other == person || !firstOf(other.name()).equals(first)) continue;
            String last = person.name().contains(" ") ? person.name().substring(person.name().indexOf(' ') + 1).trim() : "";
            return last.isEmpty() ? first : new StringBuilder(first).append(' ').appendCodePoint(last.codePointAt(0)).append('.').toString();
        }
        return first;
    }

    private static String firstOf(String name) {
        return name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt));
        g.pose().translate(-w / 2f, -h / 2f, 0);
        if (selected.getAsBoolean()) g.fill(-2, -2, w + 2, h + 2, 0xFFFAC775);
        NoticeBoardIcons.PAPER.draw(g, 0, 0, w, h);
        int imageH = h - 14;
        g.fill(3, 3, w - 3, imageH, person.color());
        // A portrait drawn from the resident's id; the same person always looks the same.
        Portrait.draw(g, 3, 3, w - 6, imageH - 3, person.id(), person.age(), person.duty() == Duty.RESTING);
        String first = caption != null ? caption : firstOf(person.name());
        Component name = Component.literal(first);
        int nameW = BoardDraw.font().width(name);
        if (nameW <= w - 4) BoardDraw.textCentered(g, name, w / 2, imageH + 3, NoticeBoardTheme.INK);
        else {
            // Long names shrink to fit the caption before they are cut.
            float scale = Math.max(MIN_NAME_SCALE, (w - 4) / (float) nameW);
            g.pose().pushPose();
            g.pose().translate(w / 2f, imageH + 3 + 8 * (1 - scale) / 2, 0);
            g.pose().scale(scale, scale, 1);
            if (nameW * scale <= w - 4) BoardDraw.textCentered(g, name, 0, 0, NoticeBoardTheme.INK);
            else BoardDraw.text(g, name, -(int) ((w - 4) / scale / 2), 0, (int) ((w - 4) / scale), NoticeBoardTheme.INK);
            g.pose().popPose();
        }
        BoardDraw.frost(g, 0, 0, w, h, Math.min(2, person.frost()));
        if (person.duty() == Duty.RESTING || person.duty() == Duty.OFF)
            BoardDraw.stamp(g, Component.translatable("gui.frostedheart.board.stamp." + (person.duty() == Duty.RESTING ? "resting" : "off")),
                    w / 2, imageH / 2, NoticeBoardTheme.RED, false, -14, 1);
        BoardDraw.pin(g, NoticeBoardIcons.PIN_RED, w / 2, 1);
        g.pose().popPose();
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        CInputHelper.playClickSound();
        onClick.run();
        return true;
    }

    @Override
    public Cursor getCursor() {
        return isMouseOver() ? Cursor.HAND : null;
    }

    @Override
    public void getTooltip(TooltipBuilder builder) {
        if (!isMouseOver()) return;
        builder.accept(Component.literal(person.name()));
        if (!person.reason().getString().isEmpty()) builder.accept(person.reason());
    }
}
