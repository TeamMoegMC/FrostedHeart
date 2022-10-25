/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.AbstractGui;

public class UV extends Rect {

    public static UV delta(int x1, int y1, int x2, int y2) {
        return new UV(Rect.delta(x1, y1, x2, y2));
    }

    public UV(Rect r) {
        super(r);
    }

    public UV(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public UV(UV uv) {
        this(uv.x, uv.y, uv.w, uv.h);
    }

    //normal blit add point
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly, Point loc) {
        blit(gui, s, lx + loc.getX(), ly + loc.getY());
    }

    //normal blit add point with custom texture size
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly, Point loc, int textureW, int textureH) {
        blit(gui, s, lx + loc.getX(), ly + loc.getY(), textureW, textureH);
    }

    //blit with width transition add point
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly, Point loc, int w) {
        blit(gui, s, lx + loc.getX(), ly + loc.getY(), w);
    }

    //normal blit
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly) {
        gui.blit(s, lx, ly, x, y, w, h);
    }

    // normal blit with custom texture size
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly, int textureW, int textureH) {
        AbstractGui.blit(s, lx, ly, x, y, w, h, textureW, textureH);
    }

    //blit with width transition
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly, int w) {
        gui.blit(s, lx, ly, x, y, w, h);
    }
    // blit with height transition
    public void blitHeightTransition(AbstractGui gui, MatrixStack s, int lx, int ly, int h) {
        gui.blit(s, lx, ly, x, y, w, h);
    }
    //blit with width transition and  custom texture size
    public void blit(AbstractGui gui, MatrixStack s, int lx, int ly, int w, int textureW, int textureH) {
        gui.blit(s, lx, ly, x, y, w, h, textureW, textureH);
    }

    //blit add point
    public void blit(MatrixStack s, int lx, int ly, Point loc, int p3, int p4) {
        blit(s, lx + loc.getX(), ly + loc.getY(), p3, p4);
    }

    //normal blit
    public void blit(MatrixStack s, int lx, int ly, int p3, int p4) {
        AbstractGui.blit(s, lx, ly, x, y, w, h, p3, p4);
    }

    //blit with atlas and add point
    public void blit(MatrixStack s, int lx, int ly, Point loc, int mx, int my, int p3, int p4) {
        blit(s, lx + loc.getX(), ly + loc.getY(), mx, my, p3, p4);
    }

    //blit with atlas
    public void blit(MatrixStack s, int lx, int ly, int mx, int my, int p3, int p4) {
        AbstractGui.blit(s, lx, ly, x + mx * w, y + my * h, w, h, p3, p4);
    }
}
