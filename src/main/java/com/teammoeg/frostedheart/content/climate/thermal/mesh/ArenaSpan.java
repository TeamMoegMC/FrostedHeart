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

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/** Dense primitive-arena span with no architecture-level cell-count ceiling. */
public record ArenaSpan(int firstSlot, int count) {
    public static final ArenaSpan EMPTY = new ArenaSpan(0, 0);

    public ArenaSpan {
        if (firstSlot < 0) {
            throw new IllegalArgumentException("firstSlot must be non-negative");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        if ((long) firstSlot + count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("span exceeds the int arena address space");
        }
    }

    public int endSlotExclusive() {
        return firstSlot + count;
    }
}
