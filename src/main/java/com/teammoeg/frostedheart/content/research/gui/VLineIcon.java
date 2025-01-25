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

package com.teammoeg.frostedheart.content.research.gui;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconProperties;
import dev.ftb.mods.ftblibrary.icon.IconWithParent;
import net.minecraft.client.gui.GuiGraphics;

public class VLineIcon extends IconWithParent {
    int x;
    int y;
    int h;
    int w;
    int side1;
    int side2;
    int tw = 256;
    int th = 256;
    Icon s0;
    Icon m;
    Icon s1;

    public VLineIcon(Icon i, int x, int y, int w, int h, int side1, int side2, int tw, int th) {
        super(i);
        this.x = x;
        this.y = y;
        this.h = h;
        this.w = w;
        this.side1 = side1;
        this.side2 = side2;
        this.tw = tw;
        this.th = th;
        updateParts();
    }

    @Override
    public VLineIcon copy() {
        return new VLineIcon(parent.copy(), x, y, w, h, side1, side2, tw, th);
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

    private Icon get(int x, int y, int w, int h) {
        return parent.withUV(this.x + x, this.y + y, w, h, tw, th);
    }

    @Override
    protected void setProperties(IconProperties properties) {
        super.setProperties(properties);
        x = properties.getInt("x", x);
        y = properties.getInt("y", y);
        w = properties.getInt("width", w);
        h = properties.getInt("height", h);
        side1 = properties.getInt("side1", side1);
        side2 = properties.getInt("side2", side2);
        tw = properties.getInt("texture_w", tw);
        th = properties.getInt("texture_h", th);

        String s = properties.getString("pos", "");

        if (!s.isEmpty()) {
            String[] s1 = s.split(",", 4);

            if (s1.length == 4) {
                x = Integer.parseInt(s1[0]);
                y = Integer.parseInt(s1[1]);
                w = Integer.parseInt(s1[2]);
                h = Integer.parseInt(s1[3]);
            }
        }

        updateParts();
    }

    public void updateParts() {
        s0 = get(0, 0, w, side1);
        m = get(0, side1, w, h - side2 - side1);
        s1 = get(0, h - side2, w, side2);
    }

}
