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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.UnihexParser.OverrideRange;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.profiling.ProfilerFiller;

public final class KGlyphProvider extends SimplePreparableReloadListener<KGlyphProvider.FontSnapshot> {
    public static final KGlyphProvider INSTANCE = new KGlyphProvider();

    private volatile FontSnapshot activeSnapshot = FontSnapshot.empty();

    private KGlyphProvider() {
    }

    FontSnapshot activeSnapshot() {
        return activeSnapshot;
    }

    @Override
    protected FontSnapshot prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        try {
            SnapshotBuilder builder = new SnapshotBuilder(resourceManager);
            builder.loadFont(new ResourceLocation("default.json"));
            return builder.build();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to build scenario font snapshot", exception);
        }
    }

    @Override
    protected void apply(FontSnapshot snapshot, ResourceManager resourceManager, ProfilerFiller profiler) {
        activeSnapshot = snapshot;
    }

    static final class FontSnapshot {
        private final List<GlyphProvider> providers;
        private final GlyphImageCache imageCache;

        FontSnapshot(List<GlyphProvider> providers) {
            this.providers = List.copyOf(providers);
            this.imageCache = new GlyphImageCache();
        }

        static FontSnapshot empty() {
            return new FontSnapshot(List.of());
        }

        void resolve(int codePoint, boolean forceUnicode, ResolvedGlyph result) {
            if (forceUnicode) {
                for (int index = providers.size() - 1; index >= 0; index--) {
                    GlyphProvider provider = providers.get(index);
                    if (provider.supportsUnicode() && provider.resolve(codePoint, true, result)) {
                        return;
                    }
                }
            } else {
                for (GlyphProvider provider : providers) {
                    if (provider.resolve(codePoint, false, result)) {
                        return;
                    }
                }
            }
            result.set(GlyphData.EMPTY);
        }

        int cachedImageCount() {
            return imageCache.size();
        }
    }

    static final class ResolvedGlyph {
        private GlyphData glyph;
        private UnihexGlyphStore unihexStore;
        private int unihexIndex = -1;

        void set(GlyphData glyph) {
            this.glyph = glyph;
            this.unihexStore = null;
            this.unihexIndex = -1;
        }

        void set(UnihexGlyphStore store, int glyphIndex) {
            this.glyph = null;
            this.unihexStore = store;
            this.unihexIndex = glyphIndex;
        }

        int render(FontSnapshot snapshot, Graphics2D graphics, int x, int y, int targetHeight, int color) {
            if (glyph != null) {
                return glyph.renderFont(graphics, snapshot.imageCache, x, y, targetHeight, color);
            }
            BufferedImage image = snapshot.imageCache.get(unihexStore, unihexIndex, color);
            int width = unihexStore.width(unihexIndex);
            graphics.drawImage(image, x, y, x + (int) (width / 16F * targetHeight), y + targetHeight,
                    0, 0, width, UnihexGlyphStore.GLYPH_HEIGHT, null);
            return (int) (unihexStore.advance(unihexIndex) / 16F * targetHeight);
        }

        int height() {
            return glyph != null ? glyph.height() : UnihexGlyphStore.GLYPH_HEIGHT;
        }

        boolean isUnicode() {
            return glyph == null || glyph.isUnicode();
        }

        boolean isMissing() {
            return glyph == GlyphData.EMPTY;
        }
    }

    interface GlyphProvider {
        boolean supportsUnicode();

        boolean resolve(int codePoint, boolean unicodeLookup, ResolvedGlyph result);
    }

    private record StaticGlyphProvider(Int2ObjectMap<GlyphData> glyphs, boolean supportsUnicode)
            implements GlyphProvider {
        @Override
        public boolean resolve(int codePoint, boolean unicodeLookup, ResolvedGlyph result) {
            GlyphData glyph = glyphs.get(codePoint);
            if (glyph == null) {
                return false;
            }
            result.set(glyph);
            return true;
        }
    }

    record UnihexProvider(UnihexGlyphStore store) implements GlyphProvider {
        @Override
        public boolean supportsUnicode() {
            return true;
        }

        @Override
        public boolean resolve(int codePoint, boolean unicodeLookup, ResolvedGlyph result) {
            int glyphIndex = unicodeLookup ? store.findLast(codePoint) : store.findFirst(codePoint);
            if (glyphIndex < 0) {
                return false;
            }
            result.set(store, glyphIndex);
            return true;
        }
    }

    private static final class SnapshotBuilder {
        private final ResourceManager resourceManager;
        private final List<GlyphProvider> providers = new ArrayList<>();
        private final Deque<ResourceLocation> referenceStack = new ArrayDeque<>();
        private final Set<ResourceLocation> visitingReferences = new HashSet<>();
        private int nextCacheId = 1;

        private SnapshotBuilder(ResourceManager resourceManager) {
            this.resourceManager = resourceManager;
        }

        FontSnapshot build() {
            return new FontSnapshot(providers);
        }

        void loadFont(ResourceLocation location) throws IOException {
            ResourceLocation file = new ResourceLocation(location.getNamespace(), "font/" + location.getPath());
            if (!visitingReferences.add(file)) {
                throw new IllegalArgumentException("Cyclic font reference: " + referenceChain(file));
            }
            referenceStack.addLast(file);
            try {
                for (Resource resource : resourceManager.getResourceStack(file)) {
                    FHMain.LOGGER.info("Reloading scenario font from {}", resource.sourcePackId());
                    try (BufferedReader reader = resource.openAsReader()) {
                        readFont(JsonParser.parseReader(reader).getAsJsonObject());
                    }
                }
            } finally {
                referenceStack.removeLast();
                visitingReferences.remove(file);
            }
        }

        private void readFont(JsonObject font) throws IOException {
            JsonArray providerDefinitions = font.getAsJsonArray("providers");
            for (JsonElement element : providerDefinitions) {
                JsonObject provider = element.getAsJsonObject();
                String type = provider.get("type").getAsString();
                switch (type) {
                    case "bitmap" -> readBitmap(provider);
                    case "legacy_unicode" -> readLegacyUnicode(provider);
                    case "reference" -> loadFont(new ResourceLocation(provider.get("id").getAsString() + ".json"));
                    case "unihex" -> readUnihex(provider);
                    case "space" -> readSpace(provider);
                    default -> FHMain.LOGGER.warn("Ignoring unsupported scenario font provider type {}", type);
                }
            }
        }

        private void readSpace(JsonObject provider) {
            Int2ObjectMap<GlyphData> glyphs = new Int2ObjectOpenHashMap<>();
            for (var entry : provider.getAsJsonObject("advances").entrySet()) {
                int codePoint = entry.getKey().codePointAt(0);
                glyphs.putIfAbsent(codePoint, GlyphData.space(allocateCacheId(), entry.getValue().getAsInt() * 2));
            }
            providers.add(new StaticGlyphProvider(glyphs, false));
        }

        private void readBitmap(JsonObject provider) throws IOException {
            int targetHeight = provider.has("height") ? provider.get("height").getAsInt() : 9;
            int ascent = provider.get("ascent").getAsInt();
            ResourceLocation declaredFile = new ResourceLocation(provider.get("file").getAsString());
            ResourceLocation texture = new ResourceLocation(declaredFile.getNamespace(),
                    "textures/" + declaredFile.getPath());
            Optional<Resource> resource = resourceManager.getResource(texture);
            if (resource.isEmpty()) {
                return;
            }

            BufferedImage image;
            try (InputStream stream = resource.get().open()) {
                image = ImageIO.read(stream);
            }
            if (image == null) {
                throw new IOException("Unable to decode font bitmap " + texture);
            }

            JsonArray rows = provider.getAsJsonArray("chars");
            int cellWidth = image.getWidth() / rows.get(0).getAsString().length();
            int cellHeight = image.getHeight() / rows.size();
            float scale = targetHeight / (float) cellHeight;
            Int2ObjectMap<GlyphData> glyphs = new Int2ObjectOpenHashMap<>();
            for (int row = 0; row < rows.size(); row++) {
                String codePoints = rows.get(row).getAsString();
                int count = codePoints.codePointCount(0, codePoints.length());
                for (int column = 0; column < count; column++) {
                    int codePoint = codePoints.codePointAt(column);
                    if (codePoint == 0) {
                        continue;
                    }
                    int visibleWidth = getCharacterWidth(image, cellWidth, cellHeight, column, row);
                    int advance = (int) (0.5D + visibleWidth * scale) + 1;
                    GlyphData glyph = GlyphData.bitmap(allocateCacheId(), column * cellWidth, row * cellHeight,
                            cellWidth, cellHeight, advance, ascent, scale, image);
                    glyphs.putIfAbsent(codePoint, glyph);
                }
            }
            providers.add(new StaticGlyphProvider(glyphs, false));
        }

        private void readLegacyUnicode(JsonObject provider) throws IOException {
            ResourceLocation sizesLocation = new ResourceLocation(provider.get("sizes").getAsString());
            Optional<Resource> sizesResource = resourceManager.getResource(sizesLocation);
            if (sizesResource.isEmpty()) {
                return;
            }
            byte[] sizes = new byte[65536];
            try (InputStream stream = sizesResource.get().open()) {
                int offset = 0;
                while (offset < sizes.length) {
                    int read = stream.read(sizes, offset, sizes.length - offset);
                    if (read < 0) {
                        break;
                    }
                    offset += read;
                }
            }

            String template = provider.get("template").getAsString();
            Int2ObjectMap<GlyphData> glyphs = new Int2ObjectOpenHashMap<>();
            for (int page = 0; page <= 0xFF; page++) {
                ResourceLocation declaredPage = new ResourceLocation(String.format(template, String.format("%02x", page)));
                ResourceLocation texture = new ResourceLocation(declaredPage.getNamespace(),
                        "textures/" + declaredPage.getPath());
                Optional<Resource> pageResource = resourceManager.getResource(texture);
                if (pageResource.isEmpty()) {
                    continue;
                }
                BufferedImage image;
                try (InputStream stream = pageResource.get().open()) {
                    image = ImageIO.read(stream);
                }
                if (image == null) {
                    throw new IOException("Unable to decode legacy font page " + texture);
                }
                for (int pageIndex = 0; pageIndex <= 0xFF; pageIndex++) {
                    int codePoint = page * 0x100 + pageIndex;
                    GlyphData glyph = GlyphData.legacyUnicode(allocateCacheId(),
                            (pageIndex & 0xF) * 16, pageIndex & 0xF0, sizes[codePoint], image);
                    glyphs.put(codePoint, glyph);
                }
            }
            providers.add(new StaticGlyphProvider(glyphs, true));
        }

        private void readUnihex(JsonObject provider) throws IOException {
            ResourceLocation file = new ResourceLocation(provider.get("hex_file").getAsString());
            List<OverrideRange> overrides = new ArrayList<>();
            if (provider.has("size_overrides")) {
                for (JsonElement element : provider.getAsJsonArray("size_overrides")) {
                    overrides.add(new OverrideRange(element.getAsJsonObject()));
                }
            }

            UnihexGlyphStore.Builder storeBuilder = new UnihexGlyphStore.Builder();
            try (InputStream stream = resourceManager.open(file);
                    ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".hex")) {
                        UnihexParser.readFromStream(new FastBufferedInputStream(zip), storeBuilder, overrides);
                    }
                }
            }
            UnihexGlyphStore store = storeBuilder.build(nextCacheId);
            nextCacheId = Math.addExact(nextCacheId, store.size());
            providers.add(new UnihexProvider(store));
        }

        private int allocateCacheId() {
            return nextCacheId++;
        }

        private String referenceChain(ResourceLocation repeated) {
            StringBuilder chain = new StringBuilder();
            for (ResourceLocation location : referenceStack) {
                if (!chain.isEmpty()) {
                    chain.append(" -> ");
                }
                chain.append(location);
            }
            if (!chain.isEmpty()) {
                chain.append(" -> ");
            }
            return chain.append(repeated).toString();
        }

        private static int getCharacterWidth(BufferedImage image, int cellWidth, int cellHeight, int column,
                int row) {
            for (int x = cellWidth - 1; x >= 0; x--) {
                for (int y = row * cellHeight; y < row * cellHeight + cellHeight; y++) {
                    if ((image.getRGB(x + column * cellWidth, y) & 0xFF000000) != 0) {
                        return x + 1;
                    }
                }
            }
            return 0;
        }
    }
}
