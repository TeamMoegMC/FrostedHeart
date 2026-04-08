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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.math.Colors;
import lombok.Getter;
import net.minecraft.network.chat.Style;

/**
 * 内容面板中行元素的抽象基类，支持对齐方式和颜色设置。
 * 通过自引用泛型实现链式调用。所有行类型（文本行、图片行、物品行等）均继承此类。
 * <p>
 * Abstract base class for line elements in a content panel, supporting alignment
 * and color configuration. Uses self-referencing generics for fluent method chaining.
 * All line types (text, image, item, etc.) extend this class.
 *
 * @param <T> 具体行类型，用于链式调用 / The concrete line type for method chaining
 */
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
        this.color= theme().UITextColor();
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
