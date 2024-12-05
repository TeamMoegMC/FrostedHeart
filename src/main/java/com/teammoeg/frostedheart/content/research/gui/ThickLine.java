/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui;

import net.minecraft.client.gui.GuiGraphics;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;

import dev.ftb.mods.ftblibrary.icon.Color4I;

public class ThickLine {
    int x, y, x2, y2;

    public Color4I color = Color4I.BLACK;

    public ThickLine() {
    }

    public void draw(GuiGraphics matrixStack, int x, int y) {
        //FHGuiHelper.drawLine(matrixStack.pose(), color, x + this.x, y + this.y, x + this.x2, y + this.y2);

        // super.draw(matrixStack, theme, x, y, w, h);
    }

    // ensure w and h is positive
    public void setPoints(int x, int y, int x2, int y2) {
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
    }

    // ensure w and h is positive
    public void setPosAndDelta(int x, int y, int dx, int dy) {
        this.x = x;
        this.y = y;
        this.x2 = x + dx;
        this.y2 = y + dy;
    }

}
