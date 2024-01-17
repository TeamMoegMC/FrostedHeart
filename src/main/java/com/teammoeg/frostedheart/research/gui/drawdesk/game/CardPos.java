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

package com.teammoeg.frostedheart.research.gui.drawdesk.game;

import net.minecraft.network.PacketBuffer;

public class CardPos {
    private static CardPos[][] cache = new CardPos[11][11];
    static {
        for (int i = 0; i < 11; i++)
            for (int j = 0; j < 11; j++)
                cache[i][j] = new CardPos(i - 1, j - 1);

    }
    final int x;
    final int y;

    private int hash = 0;

    public static CardPos valueOf(int x, int y) {
        int i = x + 1;
        int j = y + 1;
        if (i < cache.length && i >= 0 && j < cache[x].length && j >= 0)
            return cache[i][j];
        return new CardPos(x, y);
    }

    public static CardPos valueOf(PacketBuffer pb) {
        return valueOf(pb.readByte(), pb.readByte());
    }

    private CardPos(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CardPos other = (CardPos) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result + x;
            result = prime * result + y;
            hash = result;
        }
        return hash;

    }

    @Override
    public String toString() {
        return "CardPos [x=" + x + ", y=" + y + ", hash=" + hash + "]";
    }

    public void write(PacketBuffer pb) {
        pb.writeByte(x);
        pb.writeByte(y);
    }

}