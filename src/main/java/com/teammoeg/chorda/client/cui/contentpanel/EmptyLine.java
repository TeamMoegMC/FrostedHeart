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

import net.minecraft.client.gui.GuiGraphics;

/**
 * 空白行，用于在内容面板中添加垂直间距。默认高度为8像素。
 * <p>
 * Empty line for adding vertical spacing in a content panel. Default height is 8 pixels.
 */
public class EmptyLine extends Line<EmptyLine> {
    public EmptyLine(UIElement parent) {
        this(parent, 8);
    }

    public EmptyLine(UIElement parent, int height) {
        super(parent);
        setHeight(height);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
    }
}
