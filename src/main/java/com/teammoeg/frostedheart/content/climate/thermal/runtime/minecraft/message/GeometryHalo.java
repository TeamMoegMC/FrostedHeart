/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message;

/** Loaded-only neighboring geometry. These positions own no solver slots. */
public record GeometryHalo(long[] positions, int[] signatures) {
    public static final GeometryHalo EMPTY = new GeometryHalo(new long[0], new int[0]);
    public int size() { return positions.length; }
}
