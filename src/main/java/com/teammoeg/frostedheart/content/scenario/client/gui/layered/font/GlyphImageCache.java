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

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

final class GlyphImageCache {
    static final int DEFAULT_CAPACITY = 4096;

    private final int capacity;
    private final Long2ObjectLinkedOpenHashMap<BufferedImage> images;

    GlyphImageCache() {
        this(DEFAULT_CAPACITY);
    }

    GlyphImageCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Glyph image cache capacity must be positive");
        }
        this.capacity = capacity;
        this.images = new Long2ObjectLinkedOpenHashMap<>(Math.min(capacity, 256));
    }

    synchronized BufferedImage get(UnihexGlyphStore store, int glyphIndex, int color) {
        long key = key(store.cacheId(glyphIndex), color);
        BufferedImage cached = images.getAndMoveToLast(key);
        if (cached != null) {
            return cached;
        }
        return publish(key, store.createImage(glyphIndex, color));
    }

    synchronized BufferedImage get(GlyphData glyph, int color) {
        long key = key(glyph.cacheId(), color);
        BufferedImage cached = images.getAndMoveToLast(key);
        if (cached != null) {
            return cached;
        }
        return publish(key, glyph.createColoredImage(color));
    }

    synchronized int size() {
        return images.size();
    }

    private BufferedImage publish(long key, BufferedImage image) {
        if (images.size() >= capacity) {
            images.removeFirst();
        }
        images.putAndMoveToLast(key, image);
        return image;
    }

    private static long key(int cacheId, int color) {
        return ((long) cacheId << Integer.SIZE) | Integer.toUnsignedLong(color);
    }
}
