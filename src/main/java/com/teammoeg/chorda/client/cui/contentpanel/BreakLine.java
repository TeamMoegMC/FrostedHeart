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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedresearch.gui.LineIcon;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 分割线，用于在内容面板中添加水平分隔线。
 * 支持实线和渐变线两种样式，可选配装饰图标。
 * <p>
 * Break line for adding horizontal separators in a content panel.
 * Supports both solid and gradient line styles, with optional decorative icons.
 */
@Getter
public class BreakLine extends Line<BreakLine> {
    protected boolean solid = false;
    protected LineIcon lineIcon;

    public BreakLine(UIElement parent) {
        this(parent, (int)(DEF_LINE_HEIGHT * 1.5F));
        color(theme().UIBGBorderColor());
    }

    public BreakLine(UIElement parent, int height) {
        super(parent);
        setHeight(height);
        color(theme().UIBGBorderColor());
    }

    public BreakLine icon(LineIcon lineIcon) {
        this.lineIcon = lineIcon;
        return this;
    }

    public BreakLine solid(boolean isSolid) {
        this.solid = isSolid;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        super.render(graphics, x, y, w, h, hint);
        if (lineIcon != null) {
            lineIcon.draw(graphics, x, y, w, h);
        } else if (isSolid()) {
            graphics.fill(x, y+h/2, x+w, y+h/2+1, color);
        } else {
        	TesselateHelper.getShapeTesslator()
            .fillGradient(graphics.pose().last().pose(), x, y+h/2, x+w/2, y+h/2+1, Colors.setAlpha(color, 0), color)
            .fillGradient(graphics.pose().last().pose(),x+w/2, y+h/2, x+w, y+h/2+1, color, Colors.setAlpha(color, 0))
            .close();
        }
    }
}
