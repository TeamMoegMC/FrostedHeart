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

package com.teammoeg.frostedheart.content.climate.render.sky;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.BitSet;

/**
 * The heights of the columns around the eye above which diamond dust can fall, for diamond_dust.glsl, which shows a
 * glint only where its crystal could have fallen: at or above the first open height of its column, above the highest
 * block that stops falling things, as the MOTION_BLOCKING heightmap holds it. The window is SIZE by SIZE columns around
 * the eye's column, and each column sits at its world coordinates modulo SIZE, so a move only rereads the columns it
 * brings in. Nothing polls the world: a column is read when it enters the window, when a block change there may change
 * its height, or when its chunk arrives, at most a budget of columns at a time. A column not read yet counts as closed.
 * The static methods are the shader's lookup in plain Java for the GameTests.
 */
public final class DustColumnWindow {
    public static final int SIZE = 64;
    public static final int HALF_SIZE = SIZE / 2;
    public static final int COLUMNS = SIZE * SIZE;
    /**
     * The first open height of a column not read yet, above any world.
     */
    public static final float CLOSED = 1.0e6F;
    private static final int MASK = SIZE - 1;
    private static final int SIZE_BITS = Integer.numberOfTrailingZeros(SIZE);
    private static final int CHUNK_SIZE = 16;

    private final float[] heights = new float[COLUMNS];
    private final BitSet marked = new BitSet(COLUMNS);
    private boolean placed;
    private boolean changed = true;
    private int originX;
    private int originZ;

    /**
     * Gives the first open height of a column, the lowest height above every block in it that stops falling things.
     */
    @FunctionalInterface
    public interface ColumnSource {
        int firstOpenY(int x, int z);
    }

    public DustColumnWindow() {
        Arrays.fill(heights, CLOSED);
    }

    /**
     * Whether a block stops falling crystals, as the MOTION_BLOCKING heightmap counts it. A change between two states
     * that both stop them or both let them pass leaves the column's height as it was.
     */
    public static boolean stopsCrystals(BlockState state) {
        return state.blocksMotion() || !state.getFluidState().isEmpty();
    }

    /**
     * The index of the texel that holds a column, row by row in z.
     */
    public static int texelIndex(int x, int z) {
        return ((z & MASK) << SIZE_BITS) | (x & MASK);
    }

    /**
     * The column of a glint along one axis, counted from the window's first column, from the eye's column counted the
     * same way, the eye's place within its block, and the glint's offset from the eye, kept inside the window, as
     * diamond_dust.glsl finds it.
     */
    public static int columnInWindow(int eyeInWindow, float eyeFraction, float offset) {
        int column = eyeInWindow + (int) Math.floor(eyeFraction + offset);
        return Math.max(0, Math.min(MASK, column));
    }

    /**
     * The texel coordinate of a column counted from the window's first column, from the first column's own texel
     * coordinate, as diamond_dust.glsl finds it.
     */
    public static int texelOfColumn(int originTexel, int columnInWindow) {
        return (originTexel + columnInWindow) & MASK;
    }

    /**
     * Whether a glint at a height above the eye can have fallen there, as diamond_dust.glsl decides it.
     */
    public static boolean isOpenAbove(float eyeY, float offsetY, float firstOpenY) {
        return eyeY + offsetY >= firstOpenY;
    }

    /**
     * Forgets every column, so the next follow reads the whole window.
     */
    public void forget() {
        placed = false;
        marked.clear();
        Arrays.fill(heights, CLOSED);
        changed = true;
    }

    /**
     * Centers the window on the eye's column. The columns it brings in count as closed until they are read, and a jump
     * of the window's size or more, or the first call, closes and marks all of them.
     */
    public void follow(int eyeColumnX, int eyeColumnZ) {
        int newOriginX = eyeColumnX - HALF_SIZE;
        int newOriginZ = eyeColumnZ - HALF_SIZE;
        long movedX = (long) newOriginX - originX;
        long movedZ = (long) newOriginZ - originZ;
        if (!placed || Math.abs(movedX) >= SIZE || Math.abs(movedZ) >= SIZE) {
            originX = newOriginX;
            originZ = newOriginZ;
            placed = true;
            Arrays.fill(heights, CLOSED);
            marked.set(0, COLUMNS);
            changed = true;
            return;
        }
        int enteringFromX = movedX > 0 ? originX + SIZE : newOriginX;
        int enteringToX = movedX > 0 ? newOriginX + SIZE : originX;
        for (int x = enteringFromX; x < enteringToX; x++) {
            for (int texelZ = 0; texelZ < SIZE; texelZ++) {
                close((texelZ << SIZE_BITS) | (x & MASK));
            }
        }
        int enteringFromZ = movedZ > 0 ? originZ + SIZE : newOriginZ;
        int enteringToZ = movedZ > 0 ? newOriginZ + SIZE : originZ;
        for (int z = enteringFromZ; z < enteringToZ; z++) {
            for (int texelX = 0; texelX < SIZE; texelX++) {
                close(((z & MASK) << SIZE_BITS) | texelX);
            }
        }
        originX = newOriginX;
        originZ = newOriginZ;
    }

    private void close(int index) {
        heights[index] = CLOSED;
        marked.set(index);
        changed = true;
    }

    public boolean contains(int x, int z) {
        return placed && x - originX >= 0 && x - originX < SIZE && z - originZ >= 0 && z - originZ < SIZE;
    }

    /**
     * Marks a column to be read again if it lies in the window. Its last height stays until then.
     */
    public void markColumn(int x, int z) {
        if (contains(x, z)) {
            marked.set(texelIndex(x, z));
        }
    }

    /**
     * Marks the columns of a chunk that lie in the window to be read again.
     */
    public void markChunk(int chunkX, int chunkZ) {
        if (!placed) {
            return;
        }
        int fromX = Math.max(chunkX * CHUNK_SIZE, originX);
        int toX = Math.min(chunkX * CHUNK_SIZE + CHUNK_SIZE, originX + SIZE);
        int fromZ = Math.max(chunkZ * CHUNK_SIZE, originZ);
        int toZ = Math.min(chunkZ * CHUNK_SIZE + CHUNK_SIZE, originZ + SIZE);
        for (int z = fromZ; z < toZ; z++) {
            for (int x = fromX; x < toX; x++) {
                marked.set(texelIndex(x, z));
            }
        }
    }

    /**
     * Reads up to a budget of marked columns from the source and returns how many it read.
     */
    public int readMarked(ColumnSource source, int budget) {
        int read = 0;
        int index = marked.nextSetBit(0);
        while (index >= 0 && read < budget) {
            marked.clear(index);
            int x = originX + (((index & MASK) - originX) & MASK);
            int z = originZ + (((index >> SIZE_BITS) - originZ) & MASK);
            float height = source.firstOpenY(x, z);
            if (heights[index] != height) {
                heights[index] = height;
                changed = true;
            }
            read++;
            index = marked.nextSetBit(index + 1);
        }
        return read;
    }

    /**
     * Whether a height changed since the last call, which then clears it.
     */
    public boolean takeChanged() {
        boolean result = changed;
        changed = false;
        return result;
    }

    /**
     * The first open height of a column in the window, or CLOSED until it is read.
     */
    public float height(int x, int z) {
        return contains(x, z) ? heights[texelIndex(x, z)] : CLOSED;
    }

    /**
     * The heights by texel index, for upload.
     */
    public float[] heights() {
        return heights;
    }

    public int markedCount() {
        return marked.cardinality();
    }

    public boolean isPlaced() {
        return placed;
    }

    /**
     * The window's first column in x. The window holds the columns from it up to SIZE more.
     */
    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }
}
