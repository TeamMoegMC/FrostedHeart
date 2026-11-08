/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Bounded acceleration data, shared exactly between observers; eviction only causes a resend. */
final class InfraredDisplayCache {
    static final long MAX_BYTES = 8L << 20;
    private final long limit;
    private final int observers;
    private final LinkedHashMap<UUID, Display> displays = new LinkedHashMap<>(16, .75f, true);
    private final Map<Key, Page> latest = new HashMap<>();
    private long bytes;
    private int pages;

    record Key(ResourceKey<Level> dimension, long section) { }
    static final class Page {
        final Key key;
        final byte[] encoded;
        int readers;
        Page(Key key, byte[] encoded) { this.key = key; this.encoded = encoded; }
        long bytes() { return 160L + encoded.length; }
    }
    record Display(ResourceKey<Level> dimension, long generation, long center, int requestId, Page[] pages) {
        long bytes() { return 128L + 8L * pages.length; }
    }
    record Statistics(int observers, int pages, long bytes) { }

    InfraredDisplayCache() { this(MAX_BYTES, 256); }
    InfraredDisplayCache(long limit, int observers) { this.limit = limit; this.observers = observers; }

    Display find(UUID observer, ResourceKey<Level> dimension, long generation, long center, int requestId) {
        Display previous = displays.get(observer);
        return previous != null && previous.requestId == requestId && previous.dimension.equals(dimension)
                && previous.generation == generation && previous.center == center ? previous : null;
    }

    Page page(ResourceKey<Level> dimension, long section, byte[] encoded, Page baseline) {
        if (baseline != null && Arrays.equals(baseline.encoded, encoded)) return baseline;
        var key = new Key(dimension, section);
        Page shared = latest.get(key);
        return shared != null && Arrays.equals(shared.encoded, encoded) ? shared : new Page(key, encoded);
    }

    void put(UUID observer, Display display) {
        if (display == null) { remove(observer); return; }
        // Retain shared pages before releasing the observer's previous references.
        bytes += display.bytes();
        for (Page page : display.pages) if (page != null) {
            if (page.readers++ == 0) { bytes += page.bytes(); pages++; }
            latest.put(page.key, page);
        }
        Display old = displays.put(observer, display);
        if (old != null) release(old);
        while (bytes > limit || displays.size() > observers) {
            var oldest = displays.entrySet().iterator();
            Display retired = oldest.next().getValue();
            oldest.remove();
            release(retired);
        }
    }

    void remove(UUID observer) {
        Display old = displays.remove(observer);
        if (old != null) release(old);
    }

    void removeDimension(ResourceKey<Level> dimension) {
        var entries = displays.values().iterator();
        while (entries.hasNext()) {
            Display display = entries.next();
            if (display.dimension.equals(dimension)) { entries.remove(); release(display); }
        }
    }

    private void release(Display display) {
        bytes -= display.bytes();
        for (Page page : display.pages) if (page != null && --page.readers == 0) {
            bytes -= page.bytes();
            pages--;
            latest.remove(page.key, page);
        }
    }

    Statistics statistics() { return new Statistics(displays.size(), pages, bytes); }
    void clear() { displays.clear(); latest.clear(); bytes = 0; pages = 0; }
}
