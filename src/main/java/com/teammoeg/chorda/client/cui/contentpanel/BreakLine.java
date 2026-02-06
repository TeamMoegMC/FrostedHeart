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
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedresearch.gui.LineIcon;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

@Getter
public class BreakLine extends Line<BreakLine> {
    protected boolean solid = false;
    protected LineIcon lineIcon;

    public BreakLine(UIElement parent) {
        this(parent, (int)(DEF_LINE_HEIGHT * 1.5F));
        color(Colors.L_BG_GRAY);
    }

    public BreakLine(UIElement parent, int height) {
        super(parent);
        setHeight(height);
        color(Colors.L_BG_GRAY);
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
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
        if (lineIcon != null) {
            lineIcon.draw(graphics, x, y, w, h);
        } else if (isSolid()) {
            graphics.fill(x, y+h/2, x+w, y+h/2+1, color);
        } else {
            CGuiHelper.fillGradient(graphics.pose(), x, y+h/2, x+w/2, y+h/2+1, Colors.setAlpha(color, 0), color);
            CGuiHelper.fillGradient(graphics.pose(),x+w/2, y+h/2, x+w, y+h/2+1, color, Colors.setAlpha(color, 0));
        }
    }
}
