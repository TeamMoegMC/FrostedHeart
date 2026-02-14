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

package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.chorda.text.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class LimitedTextField extends UIElement {
    private Component title;
    private Component displayTitle = Components.immutableEmpty();
    public int color;
    public boolean tooltip = true;
    public boolean shadow = true;

    public LimitedTextField(UIElement parent, Component title, int w) {
        this(parent, title, w, Colors.WHITE);
    }

    public LimitedTextField(UIElement parent, Component title, int w, int textColor) {
        super(parent);
        this.title = title;
        this.color = textColor;
        setSize(w, getFont().lineHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.drawString(getFont(), displayTitle, x, y, color);
    }

    @Override
    public void refresh() {
        if (getFont().width(title) > getWidth()) {
            var e = CommonComponents.ELLIPSIS;
            var sub = getFont().substrByWidth(title, getWidth() - getFont().width(e));
            sub = FormattedText.composite(sub, e);
            displayTitle = StringTextComponentParser.parse(sub.getString()).withStyle(title.getStyle());
            return;
        }
        displayTitle = StringTextComponentParser.parse(title.getString());
    }

    public LimitedTextField shouldShowTooltip(boolean b) {
        tooltip = b;
        return this;
    }

    @Override
    public boolean hasTooltip() {
        return tooltip && super.hasTooltip();
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    public void setTitle(Component title) {
        this.title = title;
        refresh();
    }

    @Override
    public void setWidth(int v) {
        super.setWidth(v);
        refresh();
    }
}
