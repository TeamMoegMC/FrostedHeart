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
