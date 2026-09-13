/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;
/** Stable axis-paired order for source ports and sparse Page faces. */
public enum BlockFace {
    NEGATIVE_X, POSITIVE_X, NEGATIVE_Y, POSITIVE_Y, NEGATIVE_Z, POSITIVE_Z;
    public static final int COUNT=6;
    private static final BlockFace[] VALUES=values();
    public static BlockFace fromOrdinal(int ordinal) { return VALUES[ordinal]; }
    public int stepX() { return this == NEGATIVE_X ? -1 : this == POSITIVE_X ? 1 : 0; }
    public int stepY() { return this == NEGATIVE_Y ? -1 : this == POSITIVE_Y ? 1 : 0; }
    public int stepZ() { return this == NEGATIVE_Z ? -1 : this == POSITIVE_Z ? 1 : 0; }
}
