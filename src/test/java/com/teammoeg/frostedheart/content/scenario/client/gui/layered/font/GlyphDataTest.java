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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class GlyphDataTest {
    @Test
    void bitmapColorVariantsPreserveAlphaMaskAndReuseCache() {
        BufferedImage atlas = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        atlas.setRGB(1, 0, 0x80775533);
        GlyphData glyph = GlyphData.bitmap(7, 1, 0, 2, 1, 3, 0, 1, atlas);
        GlyphImageCache cache = new GlyphImageCache(4);

        BufferedImage colored = cache.get(glyph, 0xCC3366AA);

        assertEquals(0xCC3366AA, colored.getRGB(0, 0));
        assertEquals(0, colored.getRGB(1, 0));
        assertSame(colored, cache.get(glyph, 0xCC3366AA));
        assertEquals(3, glyph.scaledAdvance(1));
    }

    @Test
    void legacyPackedMetricsMatchPreviousParsing() {
        BufferedImage page = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        GlyphData glyph = GlyphData.legacyUnicode(1, 16, 32, (byte) 0x25, page);

        assertEquals(6, glyph.scaledAdvance(16));
        assertEquals(16, glyph.height());
        assertEquals(true, glyph.isUnicode());
    }
}
