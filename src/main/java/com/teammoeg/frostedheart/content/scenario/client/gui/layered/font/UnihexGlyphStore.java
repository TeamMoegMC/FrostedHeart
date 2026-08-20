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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.font;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

final class UnihexGlyphStore {
    static final int GLYPH_HEIGHT = 16;
    private static final int INITIAL_GLYPH_CAPACITY = 256;

    private final Int2IntOpenHashMap firstGlyphs;
    private final Int2IntOpenHashMap lastGlyphs;
    private final int[] rows;
    private final byte[] leftBounds;
    private final byte[] rightBounds;
    private final int cacheIdBase;

    private UnihexGlyphStore(Int2IntOpenHashMap firstGlyphs, Int2IntOpenHashMap lastGlyphs, int[] rows,
            byte[] leftBounds, byte[] rightBounds, int cacheIdBase) {
        this.firstGlyphs = firstGlyphs;
        this.lastGlyphs = lastGlyphs;
        this.rows = rows;
        this.leftBounds = leftBounds;
        this.rightBounds = rightBounds;
        this.cacheIdBase = cacheIdBase;
    }

    int findFirst(int codePoint) {
        return firstGlyphs.get(codePoint);
    }

    int findLast(int codePoint) {
        return lastGlyphs.get(codePoint);
    }

    int size() {
        return leftBounds.length;
    }

    int width(int glyphIndex) {
        return right(glyphIndex) - left(glyphIndex) + 1;
    }

    int advance(int glyphIndex) {
        return width(glyphIndex) + 1;
    }

    int cacheId(int glyphIndex) {
        return cacheIdBase + glyphIndex;
    }

    BufferedImage createImage(int glyphIndex, int color) {
        int left = left(glyphIndex);
        int right = right(glyphIndex);
        int width = right - left + 1;
        int mostLeftBit = 32 - left - 1;
        int mostRightBit = 32 - right - 1;
        BufferedImage image = new BufferedImage(width, GLYPH_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int rowOffset = glyphIndex * GLYPH_HEIGHT;
        for (int y = 0; y < GLYPH_HEIGHT; y++) {
            int row = rows[rowOffset + y];
            for (int bit = mostLeftBit; bit >= mostRightBit; bit--) {
                if (bit >= 0 && bit < Integer.SIZE && ((row >> bit) & 1) != 0) {
                    image.setRGB(mostLeftBit - bit, y, color);
                }
            }
        }
        return image;
    }

    static long estimatedStorageBytes(int glyphCount) {
        if (glyphCount <= 0) {
            return 0;
        }
        long rowAndBounds = (long) glyphCount * (GLYPH_HEIGHT * Integer.BYTES + 2L);
        long tableSlots = tableSizeFor(glyphCount);
        long twoLookupMaps = 2L * tableSlots * (Integer.BYTES + Integer.BYTES);
        return rowAndBounds + twoLookupMaps;
    }

    private static long tableSizeFor(int entries) {
        long needed = (long) Math.ceil(entries / 0.75D);
        long size = 2;
        while (size < needed) {
            size <<= 1;
        }
        return size + 1;
    }

    private int left(int glyphIndex) {
        return Byte.toUnsignedInt(leftBounds[glyphIndex]);
    }

    private int right(int glyphIndex) {
        return Byte.toUnsignedInt(rightBounds[glyphIndex]);
    }

    static final class Builder {
        private final Int2IntOpenHashMap firstGlyphs = indexMap();
        private final Int2IntOpenHashMap lastGlyphs = indexMap();
        private int[] rows = new int[INITIAL_GLYPH_CAPACITY * GLYPH_HEIGHT];
        private byte[] leftBounds = new byte[INITIAL_GLYPH_CAPACITY];
        private byte[] rightBounds = new byte[INITIAL_GLYPH_CAPACITY];
        private int size;

        void add(int codePoint, int[] glyphRows, int left, int right) {
            if (!Character.isValidCodePoint(codePoint)) {
                throw new IllegalArgumentException("Invalid Unicode code point: " + Integer.toHexString(codePoint));
            }
            if (glyphRows.length < GLYPH_HEIGHT) {
                throw new IllegalArgumentException("Unihex glyph requires 16 rows");
            }
            if (left < 0 || right < left || right >= Integer.SIZE) {
                throw new IllegalArgumentException("Invalid Unihex bounds: left=" + left + ", right=" + right);
            }
            ensureCapacity(size + 1);
            System.arraycopy(glyphRows, 0, rows, size * GLYPH_HEIGHT, GLYPH_HEIGHT);
            leftBounds[size] = (byte) left;
            rightBounds[size] = (byte) right;
            firstGlyphs.putIfAbsent(codePoint, size);
            lastGlyphs.put(codePoint, size);
            size++;
        }

        UnihexGlyphStore build(int cacheIdBase) {
            firstGlyphs.trim();
            lastGlyphs.trim();
            return new UnihexGlyphStore(firstGlyphs, lastGlyphs,
                    Arrays.copyOf(rows, size * GLYPH_HEIGHT),
                    Arrays.copyOf(leftBounds, size), Arrays.copyOf(rightBounds, size), cacheIdBase);
        }

        int size() {
            return size;
        }

        private void ensureCapacity(int required) {
            if (required <= leftBounds.length) {
                return;
            }
            int capacity = Math.max(required, leftBounds.length * 2);
            rows = Arrays.copyOf(rows, capacity * GLYPH_HEIGHT);
            leftBounds = Arrays.copyOf(leftBounds, capacity);
            rightBounds = Arrays.copyOf(rightBounds, capacity);
        }

        private static Int2IntOpenHashMap indexMap() {
            Int2IntOpenHashMap map = new Int2IntOpenHashMap();
            map.defaultReturnValue(-1);
            return map;
        }
    }
}
