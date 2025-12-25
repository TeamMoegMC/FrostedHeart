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

package com.teammoeg.frostedresearch.gui;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;

import net.minecraft.client.gui.GuiGraphics;

public class VLineIcon extends CIcon {
    int h;
    int w;
    int side1;
    int side2;
    CIcon s0;
    CIcon m;
    CIcon s1;
    CTextureIcon full;

    public VLineIcon(CTextureIcon i, int x, int y, int w, int h, int side1, int side2, int tw, int th) {
    	full=i.withUV(x, y, w, h, tw, th);
        this.h = h;
        this.w = w;
        this.side1 = side1;
        this.side2 = side2;
        updateParts();
    }

    @Override
    public void draw(GuiGraphics matrixStack, int x, int y, int w, int h) {
        int msize = h - side2 - side1;
        if (msize <= 0) {
            s0.draw(matrixStack, x, y, w, side1);
            s1.draw(matrixStack, x, y + side1, w, side2);
        } else {
            m.draw(matrixStack, x, y + side1, w, msize);
            s0.draw(matrixStack, x, y, w, side1);
            s1.draw(matrixStack, x, y + h - side2, w, side2);
        }
    }

    public void updateParts() {
        s0 = full.asPart(0, 0, w, side1);
        m = full.asPart(0, side1, w, h - side2 - side1);
        s1 = full.asPart(0, h - side2, w, side2);
    }

}
