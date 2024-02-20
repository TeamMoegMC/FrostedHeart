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

package com.teammoeg.frostedheart.util.client;

public class Rect extends Point {
    protected final int w, h;

    public static Rect delta(int x1, int y1, int x2, int y2) {
        return new Rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public Rect(int x, int y, int w, int h) {
        super(x, y);
        this.w = w;
        this.h = h;
    }

    public Rect(Rect r) {
        this(r.x, r.y, r.w, r.h);
    }

    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }
}
