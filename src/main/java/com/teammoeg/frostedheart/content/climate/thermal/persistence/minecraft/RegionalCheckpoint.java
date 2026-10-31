/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import java.util.ArrayList;
import java.util.List;

/** Field references and unconsumed input captured by one coordinator turn, without solving. */
public final class RegionalCheckpoint implements AutoCloseable {
    public enum Purpose { SAVE, UNLOAD }
    private List<RegionalQuerySnapshot> fields;
    private List<Section> sections;
    private SpatialHeatInput pending;
    private PendingHeatCheckpoint coldPending;
    public final com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation captureCredit;
    private List<QueryPublication.FrameLease> leases;
    private java.util.function.BiConsumer<Boolean, java.util.concurrent.CompletableFuture<Void>> finalizer;
    private java.util.concurrent.CompletableFuture<Void> finished;
    private boolean closed;
    private com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer;
    private com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Pending production;
    public void production(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Pending production) {
        this.producer = producer; this.production = production;
    }
    /** Main-only, once, immediately before an unchanged native capture is consumed. */
    public void collectAdditionalThrough(long tick,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore.RecordConsumer consumer) {
        if (producer != null) producer.collectAdditionalThrough(tick, production, consumer);
    }

    public RegionalCheckpoint(List<RegionalQuerySnapshot> fields,
            List<Section> sections, SpatialHeatInput pending, List<QueryPublication.FrameLease> leases, PendingHeatCheckpoint coldPending,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation captureCredit) {
        this.fields = List.copyOf(fields);
        this.sections = List.copyOf(sections);
        this.pending = pending; this.leases = List.copyOf(leases);
        this.coldPending = coldPending;
        this.captureCredit = captureCredit;
    }

    public List<RegionalQuerySnapshot> fields() { return fields; }
    public List<Section> sections() { return sections; }
    public SpatialHeatInput pending() { return pending; }
    public PendingHeatCheckpoint coldPending() { return coldPending; }

    /** A page slice of the frame's shared immutable topology; no per-save row-index copy. */
    public record Part(RegionalQuerySnapshot field, int page) {
        public int airCount() { return field.checkpointPages().airCount(page); }
        public int airComponent(int ordinal) { return field.checkpointPages().airComponent(page, ordinal); }
        public int materialCount() { return field.checkpointPages().materialCount(page); }
        public int materialBody(int ordinal) { return field.checkpointPages().materialBody(page, ordinal); }
    }
    public record Section(long sectionKey, long geometryRevision, long sampledBricks, long sampleTick, List<Part> parts) {
        public AirFieldCheckpoint captureAir(double naturalC) {
            var capture = new AirFieldCheckpoint.Capture(naturalC);
            for (var part : parts) {
                var field = part.field(); var layout = field.airLayout();
                capture.atTick(field.sampleTick());
                for (int ordinal = 0; ordinal < part.airCount(); ordinal++) {
                    int index = part.airComponent(ordinal);
                    var component = layout.component(index);
                    int brick = brick(component.minX, component.minY, component.minZ);
                    if (field.minimumQuality()) capture.add(layout,
                            (x, y, z) -> field.componentTemperatureC(component.arenaSlot, x, y, z),
                            brick, component.arenaSlot, component.arenaGeneration);
                    else capture.add(layout, field.borrowedAirCoefficients(), field.referenceTemperatureC(),
                            brick, component.arenaSlot, component.arenaGeneration);
                }
            }
            return capture.finish();
        }
    }
    private static int brick(int x, int y, int z) {
        return (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
    }
    private static final class SectionBuilder {
        final ArrayList<Part> parts = new ArrayList<>();
        long bricks, tick = Long.MAX_VALUE;
    }

    /** Physical positions group only this chunk's accepted values; live arena/Page addresses are not read by the writer. */
    public static List<Section> indexSections(long chunk, List<RegionalQuerySnapshot> fields,
            java.util.function.LongUnaryOperator geometryRevision) {
        var sections = new Long2ObjectOpenHashMap<SectionBuilder>();
        for (var field : fields) {
            var pages = field.checkpointPages();
            for (int page = 0; page < pages.size(); page++) {
                long key = pages.section(page);
                if (ChunkPos.asLong(SectionPos.x(key), SectionPos.z(key)) != chunk) continue;
                var section = sections.computeIfAbsent(key, ignored -> new SectionBuilder());
                section.parts.add(new Part(field, page));
                section.bricks |= field.checkpointBrickMask(page);
                section.tick = Math.min(section.tick, field.sampleTick());
            }
        }
        var result = new ArrayList<Section>(sections.size());
        for (var entry : sections.long2ObjectEntrySet()) {
            var section = entry.getValue();
            result.add(new Section(entry.getLongKey(), geometryRevision.applyAsLong(entry.getLongKey()),
                    section.bricks, section.tick, List.copyOf(section.parts)));
        }
        return List.copyOf(result);
    }

    /** Installed before the immutable checkpoint is handed to main. */
    public void onFinish(java.util.function.BiConsumer<Boolean, java.util.concurrent.CompletableFuture<Void>> finalizer) {
        this.finalizer = finalizer;
    }
    public java.util.concurrent.CompletableFuture<Void> finish(boolean unloading) {
        java.util.concurrent.CompletableFuture<Void> receipt;
        java.util.function.BiConsumer<Boolean, java.util.concurrent.CompletableFuture<Void>> action;
        synchronized (this) {
            if (finished != null) return finished;
            receipt = finished = new java.util.concurrent.CompletableFuture<>();
            action = finalizer; finalizer = null;
        }
        // The coordinator may retire this checkpoint while holding its intake lock.
        // Never call back into it while holding the checkpoint monitor.
        if (action == null) receipt.complete(null); else action.accept(unloading, receipt);
        return receipt;
    }

    @Override public void close() {
        synchronized (this) { if (closed) return; closed = true; }
        releaseFrames(); finish(false);
    }
    public void releaseFrames() {
        List<QueryPublication.FrameLease> retained;
        synchronized (this) {
            fields = List.of(); sections = List.of();
            pending = SpatialHeatInput.EMPTY; coldPending = PendingHeatCheckpoint.EMPTY;
            producer = null; production = null;
            retained = leases; leases = List.of();
        }
        for (var lease : retained) lease.close();
    }
}
