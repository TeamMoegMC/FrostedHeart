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

import java.util.Arrays;
import java.util.BitSet;

/**
 * The block light around the eye, for diamond_dust.glsl, which lights glints near torches and lamps from it. The volume
 * is SIZE blocks on each side around the block corner nearest the eye, and each block sits at its world coordinates
 * modulo SIZE, so a move only rereads the slabs it brings in. The shader samples it between blocks, with the texture
 * wrapping at its edges. Glints show within DustGlitterField.FADE_END blocks of the eye, and the volume reaches half a
 * block farther for the filter and a block farther for the eye's move between ticks, so no sample of a glint mixes the
 * blocks on the two sides of the seam where the wrap meets. Nothing polls the world: a block is read when it enters the
 * volume or when the light of its section changed, at most a budget of blocks at a time. A block not read yet has no
 * light. The static methods are the shader's lookup and color in plain Java for the GameTests.
 */
public final class DustLightVolume {
    public static final int SIZE = 36;
    public static final int HALF_SIZE = SIZE / 2;
    public static final int BLOCKS = SIZE * SIZE * SIZE;
    public static final int BRIGHTEST_LEVEL = 15;
    /**
     * A light level is stored as this many 255ths per level, so the texture reads it as a share of BRIGHTEST_LEVEL.
     */
    public static final int STORED_PER_LEVEL = 255 / BRIGHTEST_LEVEL;
    private static final int SECTION_SIZE = 16;

    private final byte[] values = new byte[BLOCKS];
    private final BitSet marked = new BitSet(BLOCKS);
    private boolean placed;
    private boolean changed = true;
    private int originX;
    private int originY;
    private int originZ;

    /**
     * Gives the block light level of a block, from 0 to BRIGHTEST_LEVEL.
     */
    @FunctionalInterface
    public interface LightSource {
        int blockLight(int x, int y, int z);
    }

    /**
     * The texel coordinate of a block along one axis.
     */
    public static int texel(int coordinate) {
        return Math.floorMod(coordinate, SIZE);
    }

    /**
     * The index of the texel that holds a block, in rows along x, then y, then layers along z.
     */
    public static int texelIndex(int x, int y, int z) {
        return (texel(z) * SIZE + texel(y)) * SIZE + texel(x);
    }

    /**
     * The texture coordinate along one axis of a point at an offset from the eye, from the texel of the volume's first
     * block and the eye's place counted from that block, as diamond_dust.glsl finds it. The point is kept half a block
     * inside the volume, so the filter never reads across the seam.
     */
    public static float textureCoordinate(int originTexel, float eyeInVolume, float offset) {
        float inVolume = Math.max(0.5F, Math.min(SIZE - 0.5F, eyeInVolume + offset));
        return (originTexel + inVolume) / SIZE;
    }

    /**
     * The color of block light at a share of the brightest level, as diamond_dust.glsl computes it. It is the block
     * part of LightTexture without flicker, each channel clamped to 1.
     */
    public static void blockLightColor(float lightShare, float[] color) {
        float share = Math.max(0.0F, Math.min(1.0F, lightShare));
        float brightness = 1.5F * share / (4.0F - 3.0F * share);
        color[0] = Math.min(1.0F, brightness);
        color[1] = Math.min(1.0F, brightness * ((brightness * 0.6F + 0.4F) * 0.6F + 0.4F));
        color[2] = Math.min(1.0F, brightness * (brightness * brightness * 0.6F + 0.4F));
    }

    /**
     * Forgets every block, so the next follow reads the whole volume.
     */
    public void forget() {
        placed = false;
        marked.clear();
        Arrays.fill(values, (byte) 0);
        changed = true;
    }

    /**
     * Centers the volume on the block corner nearest the eye. The blocks it brings in have no light until they are
     * read, and a jump of the volume's size or more, or the first call, clears and marks all of them.
     */
    public void follow(int cornerX, int cornerY, int cornerZ) {
        int newOriginX = cornerX - HALF_SIZE;
        int newOriginY = cornerY - HALF_SIZE;
        int newOriginZ = cornerZ - HALF_SIZE;
        long movedX = (long) newOriginX - originX;
        long movedY = (long) newOriginY - originY;
        long movedZ = (long) newOriginZ - originZ;
        if (!placed || Math.abs(movedX) >= SIZE || Math.abs(movedY) >= SIZE || Math.abs(movedZ) >= SIZE) {
            originX = newOriginX;
            originY = newOriginY;
            originZ = newOriginZ;
            placed = true;
            Arrays.fill(values, (byte) 0);
            marked.set(0, BLOCKS);
            changed = true;
            return;
        }
        int enteringFromX = movedX > 0 ? originX + SIZE : newOriginX;
        int enteringToX = movedX > 0 ? newOriginX + SIZE : originX;
        for (int x = enteringFromX; x < enteringToX; x++) {
            for (int texelZ = 0; texelZ < SIZE; texelZ++) {
                for (int texelY = 0; texelY < SIZE; texelY++) {
                    clear((texelZ * SIZE + texelY) * SIZE + texel(x));
                }
            }
        }
        int enteringFromY = movedY > 0 ? originY + SIZE : newOriginY;
        int enteringToY = movedY > 0 ? newOriginY + SIZE : originY;
        for (int y = enteringFromY; y < enteringToY; y++) {
            for (int texelZ = 0; texelZ < SIZE; texelZ++) {
                for (int texelX = 0; texelX < SIZE; texelX++) {
                    clear((texelZ * SIZE + texel(y)) * SIZE + texelX);
                }
            }
        }
        int enteringFromZ = movedZ > 0 ? originZ + SIZE : newOriginZ;
        int enteringToZ = movedZ > 0 ? newOriginZ + SIZE : originZ;
        for (int z = enteringFromZ; z < enteringToZ; z++) {
            for (int texelY = 0; texelY < SIZE; texelY++) {
                for (int texelX = 0; texelX < SIZE; texelX++) {
                    clear((texel(z) * SIZE + texelY) * SIZE + texelX);
                }
            }
        }
        originX = newOriginX;
        originY = newOriginY;
        originZ = newOriginZ;
    }

    private void clear(int index) {
        values[index] = 0;
        marked.set(index);
        changed = true;
    }

    public boolean contains(int x, int y, int z) {
        return placed && x - originX >= 0 && x - originX < SIZE && y - originY >= 0 && y - originY < SIZE
                && z - originZ >= 0 && z - originZ < SIZE;
    }

    /**
     * Marks the blocks of a light section that lie in the volume to be read again, and returns whether there were
     * any. Their last light stays until then.
     */
    public boolean markSection(int sectionX, int sectionY, int sectionZ) {
        if (!placed) {
            return false;
        }
        int fromX = Math.max(sectionX * SECTION_SIZE, originX);
        int toX = Math.min(sectionX * SECTION_SIZE + SECTION_SIZE, originX + SIZE);
        int fromY = Math.max(sectionY * SECTION_SIZE, originY);
        int toY = Math.min(sectionY * SECTION_SIZE + SECTION_SIZE, originY + SIZE);
        int fromZ = Math.max(sectionZ * SECTION_SIZE, originZ);
        int toZ = Math.min(sectionZ * SECTION_SIZE + SECTION_SIZE, originZ + SIZE);
        if (fromX >= toX || fromY >= toY || fromZ >= toZ) {
            return false;
        }
        for (int z = fromZ; z < toZ; z++) {
            for (int y = fromY; y < toY; y++) {
                for (int x = fromX; x < toX; x++) {
                    marked.set(texelIndex(x, y, z));
                }
            }
        }
        return true;
    }

    /**
     * Reads up to a budget of marked blocks from the source and returns how many it read.
     */
    public int readMarked(LightSource source, int budget) {
        int read = 0;
        int index = marked.nextSetBit(0);
        while (index >= 0 && read < budget) {
            marked.clear(index);
            int x = originX + Math.floorMod(index % SIZE - originX, SIZE);
            int y = originY + Math.floorMod(index / SIZE % SIZE - originY, SIZE);
            int z = originZ + Math.floorMod(index / (SIZE * SIZE) - originZ, SIZE);
            int level = Math.max(0, Math.min(BRIGHTEST_LEVEL, source.blockLight(x, y, z)));
            byte value = (byte) (level * STORED_PER_LEVEL);
            if (values[index] != value) {
                values[index] = value;
                changed = true;
            }
            read++;
            index = marked.nextSetBit(index + 1);
        }
        return read;
    }

    /**
     * Whether a light changed since the last call, which then clears it.
     */
    public boolean takeChanged() {
        boolean result = changed;
        changed = false;
        return result;
    }

    /**
     * The block light level of a block in the volume, or 0 outside it or until it is read.
     */
    public int light(int x, int y, int z) {
        return contains(x, y, z) ? (values[texelIndex(x, y, z)] & 0xFF) / STORED_PER_LEVEL : 0;
    }

    /**
     * The stored light by texel index, for upload.
     */
    public byte[] values() {
        return values;
    }

    public int markedCount() {
        return marked.cardinality();
    }

    public boolean isPlaced() {
        return placed;
    }

    /**
     * The volume's first block in x. The volume holds the blocks from it up to SIZE more.
     */
    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    public int originZ() {
        return originZ;
    }
}
