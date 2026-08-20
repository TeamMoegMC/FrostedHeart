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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

class FontSnapshotTest {
    @Test
    void normalLookupUsesFirstProviderAndUnicodeLookupUsesLastProvider() {
        UnihexGlyphStore first = store(1, 0x41, 0x80000000);
        UnihexGlyphStore last = store(2, 0x41, 0x40000000);
        KGlyphProvider.FontSnapshot snapshot = new KGlyphProvider.FontSnapshot(List.of(
                new KGlyphProvider.UnihexProvider(first), new KGlyphProvider.UnihexProvider(last)));

        assertEquals(0xFFFFFFFF, firstPixel(snapshot, false));
        assertEquals(0, firstPixel(snapshot, true));
    }

    @Test
    void replacementSnapshotDoesNotRetainRemovedGlyphAndOldSnapshotRemainsDrawable() {
        UnihexGlyphStore oldStore = store(1, 0x41, 0x80000000);
        UnihexGlyphStore newStore = store(1, 0x42, 0x40000000);
        KGlyphProvider.FontSnapshot oldSnapshot = new KGlyphProvider.FontSnapshot(
                List.of(new KGlyphProvider.UnihexProvider(oldStore)));
        KGlyphProvider.FontSnapshot newSnapshot = new KGlyphProvider.FontSnapshot(
                List.of(new KGlyphProvider.UnihexProvider(newStore)));
        KGlyphProvider.FontSnapshot originalSnapshot = KGlyphProvider.INSTANCE.activeSnapshot();
        KGlyphProvider.ResolvedGlyph glyph = new KGlyphProvider.ResolvedGlyph();
        try {
            KGlyphProvider.INSTANCE.apply(oldSnapshot, null, null);
            KGlyphProvider.INSTANCE.activeSnapshot().resolve(0x41, false, glyph);
            assertFalse(glyph.isMissing());

            KGlyphProvider.INSTANCE.apply(newSnapshot, null, null);
            KGlyphProvider.INSTANCE.activeSnapshot().resolve(0x41, false, glyph);
            assertTrue(glyph.isMissing());

            oldSnapshot.resolve(0x41, false, glyph);
            assertEquals(0xFFFFFFFF, renderFirstPixel(oldSnapshot, glyph));
            assertEquals(1, oldSnapshot.cachedImageCount());
            assertEquals(0, newSnapshot.cachedImageCount());
        } finally {
            KGlyphProvider.INSTANCE.apply(originalSnapshot, null, null);
        }
    }

    @Test
    void failedPreparationKeepsTheActiveSnapshot() {
        UnihexGlyphStore store = store(1, 0x41, 0x80000000);
        KGlyphProvider.FontSnapshot working = new KGlyphProvider.FontSnapshot(
                List.of(new KGlyphProvider.UnihexProvider(store)));
        KGlyphProvider.FontSnapshot originalSnapshot = KGlyphProvider.INSTANCE.activeSnapshot();
        try {
            KGlyphProvider.INSTANCE.apply(working, null, null);

            assertThrows(IllegalStateException.class,
                    () -> KGlyphProvider.INSTANCE.prepare(new FailingResourceManager(), null));
            assertSame(working, KGlyphProvider.INSTANCE.activeSnapshot());
        } finally {
            KGlyphProvider.INSTANCE.apply(originalSnapshot, null, null);
        }
    }

    private static int firstPixel(KGlyphProvider.FontSnapshot snapshot, boolean forceUnicode) {
        KGlyphProvider.ResolvedGlyph glyph = new KGlyphProvider.ResolvedGlyph();
        snapshot.resolve(0x41, forceUnicode, glyph);
        return renderFirstPixel(snapshot, glyph);
    }

    private static int renderFirstPixel(KGlyphProvider.FontSnapshot snapshot, KGlyphProvider.ResolvedGlyph glyph) {
        BufferedImage target = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            glyph.render(snapshot, graphics, 0, 0, 16, 0xFFFFFFFF);
        } finally {
            graphics.dispose();
        }
        return target.getRGB(0, 0);
    }

    private static UnihexGlyphStore store(int cacheId, int codePoint, int firstRow) {
        UnihexGlyphStore.Builder builder = new UnihexGlyphStore.Builder();
        int[] rows = new int[16];
        rows[0] = firstRow;
        builder.add(codePoint, rows, 0, 7);
        return builder.build(cacheId);
    }

    private static final class FailingResourceManager implements ResourceManager {
        @Override
        public Set<String> getNamespaces() {
            return Set.of();
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation location) {
            return Optional.empty();
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation location) {
            throw new IllegalArgumentException("synthetic reload failure");
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
            return Map.of();
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String path,
                Predicate<ResourceLocation> filter) {
            return Map.of();
        }

        @Override
        public Stream<PackResources> listPacks() {
            return Stream.empty();
        }
    }
}
