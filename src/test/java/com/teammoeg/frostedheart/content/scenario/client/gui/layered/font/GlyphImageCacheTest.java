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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class GlyphImageCacheTest {
    @Test
    void reusesHitsAndSeparatesColorVariants() {
        UnihexGlyphStore store = oneGlyphStore();
        GlyphImageCache cache = new GlyphImageCache(4);
        int glyph = store.findFirst(0x41);

        BufferedImage white = cache.get(store, glyph, 0xFFFFFFFF);
        BufferedImage whiteAgain = cache.get(store, glyph, 0xFFFFFFFF);
        BufferedImage blue = cache.get(store, glyph, 0xFF3366CC);

        assertSame(white, whiteAgain);
        assertNotSame(white, blue);
        assertEquals(0xFFFFFFFF, white.getRGB(0, 0));
        assertEquals(0xFF3366CC, blue.getRGB(0, 0));
        assertEquals(2, cache.size());
    }

    @Test
    void evictsLeastRecentlyUsedVariantAtHardLimit() {
        UnihexGlyphStore store = oneGlyphStore();
        GlyphImageCache cache = new GlyphImageCache(2);
        int glyph = store.findFirst(0x41);
        BufferedImage first = cache.get(store, glyph, 0xFF000001);
        BufferedImage second = cache.get(store, glyph, 0xFF000002);
        assertSame(first, cache.get(store, glyph, 0xFF000001));

        cache.get(store, glyph, 0xFF000003);

        assertEquals(2, cache.size());
        assertSame(first, cache.get(store, glyph, 0xFF000001));
        assertNotSame(second, cache.get(store, glyph, 0xFF000002));
    }

    @Test
    void concurrentRequestsPublishOneCompleteImmutableImage() throws Exception {
        UnihexGlyphStore store = oneGlyphStore();
        GlyphImageCache cache = new GlyphImageCache(4);
        int glyph = store.findFirst(0x41);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<BufferedImage>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                tasks.add(() -> {
                    start.await();
                    return cache.get(store, glyph, 0xFF4A7BC1);
                });
            }
            List<Future<BufferedImage>> futures = new ArrayList<>();
            for (Callable<BufferedImage> task : tasks) {
                futures.add(executor.submit(task));
            }
            start.countDown();
            BufferedImage expected = futures.get(0).get();
            for (Future<BufferedImage> future : futures) {
                assertSame(expected, future.get());
            }
            assertEquals(0xFF4A7BC1, expected.getRGB(0, 0));
            assertEquals(1, cache.size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void defaultCacheHasA4096EntryHardLimit() {
        UnihexGlyphStore store = oneGlyphStore();
        GlyphImageCache cache = new GlyphImageCache();
        int glyph = store.findFirst(0x41);

        for (int variant = 0; variant <= GlyphImageCache.DEFAULT_CAPACITY; variant++) {
            cache.get(store, glyph, 0xFF000000 | variant);
        }

        assertEquals(GlyphImageCache.DEFAULT_CAPACITY, cache.size());
    }

    private static UnihexGlyphStore oneGlyphStore() {
        UnihexGlyphStore.Builder builder = new UnihexGlyphStore.Builder();
        int[] rows = new int[16];
        rows[0] = 0x80000000;
        builder.add(0x41, rows, 0, 7);
        return builder.build(1);
    }
}
