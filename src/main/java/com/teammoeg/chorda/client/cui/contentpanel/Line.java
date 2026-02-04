/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import lombok.Getter;
import net.minecraft.network.chat.Style;

@Getter
public abstract class Line<T extends Line<T>> extends UILayer {
    public static final int DEF_LINE_HEIGHT = 12;
    protected static Style hoveredStyle;
    protected Alignment alignment;
    protected int color;

    public Line(UIElement parent) {
        this(parent, Alignment.LEFT);
    }

    public Line(UIElement parent, Alignment alignment) {
        this(parent,alignment, Colors.WHITE);
    }

    public Line(UIElement parent, Alignment alignment, int color) {
        super(parent);
        this.alignment = alignment;
        this.color = color;
    }

    @SuppressWarnings("unchecked")
    public T height(int h) {
        setHeight(h);
        refresh();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T alignment(Alignment alignment) {
        this.alignment = alignment;
        refresh();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T color(int color) {
        this.color = color;
        refresh();
        return (T) this;
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth());
    }

    @Override
    public void alignWidgets() {}

    @Override
    public void addUIElements() {}
}
