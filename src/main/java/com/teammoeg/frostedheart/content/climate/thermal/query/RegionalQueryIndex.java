/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot.Template;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * Live address indexes guarded by QueryPublication's monitor. Page membership is
 * immutable after preparation, so readers can retain a Page across local installs.
 * A change depends only on its old owners and touched Pages, never a dimension epoch.
 */
final class RegionalQueryIndex {
    final IntMap<Template> cells = new IntMap<>();
    final LongMap<Template> regions = new LongMap<>();
    final IntMap<Page> pages = new IntMap<>();
    final LongMap<Page> sections = new LongMap<>();

    /** Explicit pre-commit growth; removals keep charged capacity until owner shutdown. */
    static final class IntMap<V> extends Int2ObjectOpenHashMap<V> {
        long backingBytes() { return 64L + AirBackingMeter.shallowArrayBytes(key) + AirBackingMeter.shallowArrayBytes(value); }
        long growthBytes(int expected) {
            int capacity = expected <= maxFill ? 0 : HashCommon.arraySize(expected, f) + 1;
            return capacity == 0 ? 0 : AirBackingMeter.arrayPayloadBytes(capacity, 4) + AirBackingMeter.arrayPayloadBytes(capacity, 8);
        }
        void reserve(int expected) { if (expected > maxFill) rehash(HashCommon.arraySize(expected, f)); }
        @Override protected void rehash(int capacity) { if (capacity > n) super.rehash(capacity); }
    }
    static final class LongMap<V> extends Long2ObjectOpenHashMap<V> {
        long backingBytes() { return 64L + AirBackingMeter.shallowArrayBytes(key) + AirBackingMeter.shallowArrayBytes(value); }
        long growthBytes(int expected) {
            return expected <= maxFill ? 0 : 2 * AirBackingMeter.arrayPayloadBytes(HashCommon.arraySize(expected, f) + 1, 8);
        }
        void reserve(int expected) { if (expected > maxFill) rehash(HashCommon.arraySize(expected, f)); }
        @Override protected void rehash(int capacity) { if (capacity > n) super.rehash(capacity); }
    }

    static final class Page {
        final int slot;
        final long section;
        final boolean hasAir;
        final IntMap<Template> cells;
        final LongMap<Template[]> bricks;
        final Template[] templates;
        final long backingBytes;
        // Only the publication monitor changes lifetime accounting; membership above never changes.
        int references;

        Page(PageBuilder builder) {
            slot = builder.slot; section = builder.section; cells = builder.cells;
            bricks = new LongMap<>(); bricks.reserve(builder.bricks.size());
            var iterator = builder.bricks.long2ObjectEntrySet().fastIterator();
            long arrays = 0;
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var owners = entry.getValue().toArray(Template[]::new);
                bricks.put(entry.getLongKey(), owners);
                arrays += AirBackingMeter.shallowArrayBytes(owners);
            }
            hasAir = !bricks.isEmpty();
            templates = builder.templates.toArray(Template[]::new);
            backingBytes = 96 + cells.backingBytes() + bricks.backingBytes()
                    + AirBackingMeter.shallowArrayBytes(templates) + arrays;
        }
    }

    private static final class PageBuilder {
        final int slot;
        long section;
        final IntMap<Template> cells = new IntMap<>();
        final LongMap<ArrayList<Template>> bricks = new LongMap<>();
        final ReferenceOpenHashSet<Template> templates = new ReferenceOpenHashSet<>();

        PageBuilder(int slot, Page previous, ReferenceOpenHashSet<Template> removed, int expectedCells) {
            this.slot = slot;
            cells.reserve(expectedCells);
            if (previous == null) return;
            section = previous.section;
            var cellIterator = previous.cells.int2ObjectEntrySet().fastIterator();
            while (cellIterator.hasNext()) {
                var entry = cellIterator.next();
                if (!removed.contains(entry.getValue())) addCell(entry.getIntKey(), entry.getValue());
            }
            var brickIterator = previous.bricks.long2ObjectEntrySet().fastIterator();
            while (brickIterator.hasNext()) {
                var entry = brickIterator.next();
                for (var owner : entry.getValue()) if (!removed.contains(owner)) addBrick(entry.getLongKey(), owner);
            }
        }
        void addCell(int cell, Template owner) { cells.put(cell, owner); templates.add(owner); }
        void addBrick(long brick, Template owner) {
            var owners = bricks.computeIfAbsent(brick, ignored -> new ArrayList<>());
            if (owners.isEmpty() || owners.get(owners.size() - 1) != owner) owners.add(owner);
        }
    }

    /** Private local proposal. Index reads occur while capturing it or under the publication monitor. */
    static final class Change {
        final Template[] removed;
        final RegionalQuerySnapshot[] added;
        final int[] pageSlots, pageCellCounts;
        final Page[] expectedPages, replacementPages;
        final ReferenceOpenHashSet<Template> removedSet = new ReferenceOpenHashSet<>();
        private final Int2IntOpenHashMap pageIndexes = new Int2IntOpenHashMap();
        private PageBuilder[] builders;
        private int phase, page, region, cell, component;
        final int addedCells, removedCells;
        long replacementBytes;
        boolean ready;

        Change(RegionalQueryIndex index, List<RegionalQuerySnapshot> removed, List<RegionalQuerySnapshot> added) {
            this.removed = new Template[removed.size()];
            this.added = added.toArray(RegionalQuerySnapshot[]::new);
            var touched = new IntOpenHashSet();
            int removedCount = 0;
            for (int i = 0; i < removed.size(); i++) {
                var template = this.removed[i] = removed.get(i).template;
                removedSet.add(template);
                removedCount = Math.addExact(removedCount, template.cells.length);
                for (int slot : template.distinctPages) touched.add(slot);
            }
            removedCells = removedCount;
            int cells = 0;
            for (var frame : this.added) {
                cells = Math.addExact(cells, frame.template.cells.length);
                for (int slot : frame.template.distinctPages) touched.add(slot);
            }
            addedCells = cells;
            pageSlots = touched.toIntArray(); Arrays.sort(pageSlots);
            pageCellCounts = new int[pageSlots.length];
            expectedPages = new Page[pageSlots.length]; replacementPages = new Page[pageSlots.length];
            builders = new PageBuilder[pageSlots.length]; pageIndexes.defaultReturnValue(-1);
            for (int i = 0; i < pageSlots.length; i++) {
                expectedPages[i] = index.pages.get(pageSlots[i]); pageIndexes.put(pageSlots[i], i);
                pageCellCounts[i] = expectedPages[i] == null ? 0 : expectedPages[i].cells.size();
            }
            // Reuse the immutable PageIndex instead of recounting every cell.
            // A stale removed owner may already be absent from the captured Pages.
            for (var template : this.removed) if (index.regions.get(template.regionId) == template) adjustPageCells(template, -1);
            for (var frame : this.added) adjustPageCells(frame.template, 1);
        }

        private void adjustPageCells(Template template, int sign) {
            var rows = template.checkpointPages;
            for (int page = 0; page < template.distinctPages.length; page++)
                pageCellCounts[pageIndexes.get(template.distinctPages[page])] += sign * (rows.airCount(page) + rows.materialCount(page));
        }

        long preparationBytes() {
            long bytes = 1024L + 256L * pageSlots.length + 192L * addedCells;
            for (var page : expectedPages) if (page != null) bytes += 3 * page.backingBytes;
            for (var frame : added) bytes += 192L * frame.template.air.componentCount();
            return bytes;
        }

        boolean current(RegionalQueryIndex index) {
            for (var template : removed) if (index.regions.get(template.regionId) != template) return false;
            for (var frame : added) {
                var previous = index.regions.get(frame.template.regionId);
                if (previous != null && !removedSet.contains(previous)) return false;
            }
            for (int i = 0; i < pageSlots.length; i++) if (index.pages.get(pageSlots[i]) != expectedPages[i]) return false;
            return true;
        }

        boolean run(long deadline) {
            if (ready) return true;
            do {
                if (phase == 0) {
                    if (page == pageSlots.length) { phase++; page = 0; continue; }
                    builders[page] = new PageBuilder(pageSlots[page], expectedPages[page], removedSet, pageCellCounts[page]); page++;
                } else if (phase == 1) {
                    if (region == added.length) { phase++; region = 0; continue; }
                    var template = added[region].template;
                    int end = Math.min(template.cells.length, cell + 512);
                    for (; cell < end; cell++) builders[pageIndexes.get(template.pageSlots[cell])].addCell(template.cells[cell], template);
                    if (cell == template.cells.length) { cell = 0; region++; }
                } else if (phase == 2) {
                    if (region == added.length) { phase++; continue; }
                    var template = added[region].template;
                    int end = Math.min(template.air.componentCount(), component + 128);
                    for (; component < end; component++) {
                        var air = template.air.component(component);
                        var builder = builders[pageIndexes.get(template.pageSlots[template.cellIndex(air.arenaSlot)])];
                        builder.section = SectionPos.asLong(air.minX >> 4, air.minY >> 4, air.minZ >> 4);
                        builder.addBrick(BlockPos.asLong(air.minX, air.minY, air.minZ), template);
                    }
                    if (component == template.air.componentCount()) { component = 0; region++; }
                } else {
                    if (page == pageSlots.length) { builders = null; ready = true; return true; }
                    var builder = builders[page];
                    if (!builder.cells.isEmpty()) {
                        replacementPages[page] = new Page(builder);
                        replacementBytes += replacementPages[page].backingBytes;
                    }
                    builders[page++] = null;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
    }

    Template at(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        var page = sections.get(SectionPos.asLong(bx >> 4, by >> 4, bz >> 4));
        var candidates = page == null ? null : page.bricks.get(BlockPos.asLong(bx & ~3, by & ~3, bz & ~3));
        if (candidates != null) for (var template : candidates) if (template.air.componentAt(bx, by, bz) != null) return template;
        return null;
    }

    long backingBytes() { return cells.backingBytes() + regions.backingBytes() + pages.backingBytes() + sections.backingBytes(); }
    long growthBytes(Change change) {
        return cells.growthBytes(Math.addExact(cells.size() - change.removedCells, change.addedCells))
                + regions.growthBytes(Math.addExact(regions.size() - change.removed.length, change.added.length))
                + pages.growthBytes(Math.addExact(pages.size(), change.pageSlots.length))
                + sections.growthBytes(Math.addExact(sections.size(), change.pageSlots.length));
    }
    void reserve(Change change) {
        // These two indexes remove every old owner before adding any replacement.
        // Page/Section changes are interleaved and keep their conservative bound.
        cells.reserve(Math.addExact(cells.size() - change.removedCells, change.addedCells));
        regions.reserve(Math.addExact(regions.size() - change.removed.length, change.added.length));
        pages.reserve(Math.addExact(pages.size(), change.pageSlots.length));
        sections.reserve(Math.addExact(sections.size(), change.pageSlots.length));
    }

    /** Capacity is prepared first; no whole-dimension traversal or container growth at install. */
    void install(Change change) {
        for (var template : change.removed) {
            regions.remove(template.regionId);
            for (int slot : template.cells) cells.remove(slot);
        }
        for (var frame : change.added) {
            var template = frame.template; regions.put(template.regionId, template);
            for (int slot : template.cells) cells.put(slot, template);
        }
        for (int i = 0; i < change.pageSlots.length; i++) {
            var previous = change.expectedPages[i];
            if (previous != null && previous.hasAir) sections.remove(previous.section);
            var next = change.replacementPages[i];
            if (next == null) pages.remove(change.pageSlots[i]);
            else {
                pages.put(next.slot, next);
                if (next.hasAir) sections.put(next.section, next);
            }
        }
    }
}
