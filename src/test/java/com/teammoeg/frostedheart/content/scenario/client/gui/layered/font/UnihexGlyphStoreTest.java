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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class UnihexGlyphStoreTest {
    @Test
    void parsesAllSupportedRowWidthsWithExistingMetricsAndPixels() throws Exception {
        String input = line("0041", "81")
                + line("0042", "8001")
                + line("0043", "FF0000")
                + line("1F642", "80000000");

        UnihexGlyphStore store = parse(input, List.of());

        assertGlyph(store, 0x41, 25, 0, 7);
        assertGlyph(store, 0x42, 17, 0, 15);
        assertGlyph(store, 0x43, 9, 0, 7);
        assertGlyph(store, 0x1F642, 1, 0);
    }

    @Test
    void sizeOverrideControlsSourceBoundsAndMetrics() throws Exception {
        UnihexGlyphStore store = parse(line("0041", "FF"),
                List.of(new UnihexParser.OverrideRange(0x41, 0x41, 2, 5)));

        int glyph = store.findFirst(0x41);
        assertEquals(4, store.width(glyph));
        assertEquals(5, store.advance(glyph));
        BufferedImage image = store.createImage(glyph, 0xFF31A2C4);
        for (int x = 0; x < 4; x++) {
            assertEquals(0xFF31A2C4, image.getRGB(x, 0));
        }
    }

    @Test
    void duplicateCodePointsKeepFirstNormalAndLastUnicodeEntry() throws Exception {
        UnihexGlyphStore store = parse(line("0041", "80") + line("0041", "40"), List.of());

        int first = store.findFirst(0x41);
        int last = store.findLast(0x41);
        assertNotEquals(first, last);
        assertEquals(0xFFFFFFFF, store.createImage(first, 0xFFFFFFFF).getRGB(0, 0));
        assertEquals(0, store.createImage(last, 0xFFFFFFFF).getRGB(0, 0));
        assertEquals(0xFFFFFFFF, store.createImage(last, 0xFFFFFFFF).getRGB(1, 0));
    }

    @Test
    void parserCopiesRowsBeforeReusingItsScratchStorage() throws Exception {
        UnihexGlyphStore store = parse(line("0041", "80") + line("0042", "01"), List.of());

        assertEquals(0xFFFFFFFF, store.createImage(store.findFirst(0x41), 0xFFFFFFFF).getRGB(0, 0));
        assertEquals(0, store.createImage(store.findFirst(0x42), 0xFFFFFFFF).getRGB(0, 0));
        assertEquals(0xFFFFFFFF, store.createImage(store.findFirst(0x42), 0xFFFFFFFF).getRGB(7, 0));
    }

    @Test
    void rejectsMalformedCodePointBitmapAndHex() {
        assertThrows(IllegalArgumentException.class, () -> parse("041:0000\n", List.of()));
        assertThrows(IllegalArgumentException.class, () -> parse("0041:0000\n", List.of()));
        assertThrows(IllegalArgumentException.class, () -> parse(line("0041", "GG"), List.of()));
    }

    @Test
    void compactStorageStaysWithinBudgetAtMeasuredGlyphCount() {
        long estimate = UnihexGlyphStore.estimatedStorageBytes(113_000);

        assertTrue(estimate <= 16L * 1024 * 1024, "estimated bytes=" + estimate);
    }

    private static UnihexGlyphStore parse(String input, List<UnihexParser.OverrideRange> overrides) throws Exception {
        UnihexGlyphStore.Builder builder = new UnihexGlyphStore.Builder();
        UnihexParser.readFromStream(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII)), builder, overrides);
        return builder.build(1);
    }

    private static String line(String codePoint, String row) {
        return codePoint + ":" + row.repeat(16) + "\n";
    }

    private static void assertGlyph(UnihexGlyphStore store, int codePoint, int width, int... opaquePixels) {
        int glyph = store.findFirst(codePoint);
        assertTrue(glyph >= 0);
        assertEquals(width, store.width(glyph));
        assertEquals(width + 1, store.advance(glyph));
        BufferedImage image = store.createImage(glyph, 0xFFFFFFFF);
        assertEquals(width, image.getWidth());
        assertEquals(16, image.getHeight());
        for (int x : opaquePixels) {
            assertEquals(0xFFFFFFFF, image.getRGB(x, 0), "x=" + x);
        }
    }
}
