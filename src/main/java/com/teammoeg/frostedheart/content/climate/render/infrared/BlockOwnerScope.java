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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render.infrared;

/** A model's real block position, scoped to the thread that emits its vertices. */
public final class BlockOwnerScope {
    private static final ThreadLocal<Cursor> CURRENT = ThreadLocal.withInitial(Cursor::new);

    private BlockOwnerScope() {}

    public static int enter(int x, int y, int z) {
        Cursor cursor = CURRENT.get();
        int previous = cursor.owner;
        cursor.owner = 0x1000 | (x & 15) | ((z & 15) << 4) | ((y & 15) << 8);
        return previous;
    }

    public static void restore(int previous) { CURRENT.get().owner = previous; }
    public static int current() { return CURRENT.get().owner; }

    private static final class Cursor { int owner; }
}
