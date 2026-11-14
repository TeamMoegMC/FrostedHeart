/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import java.util.List;

/** Immutable committed Section values and pending input for one original Chunk save. */
public final class RegionalCheckpoint implements AutoCloseable {
    public enum Purpose { SAVE, UNLOAD }
    private List<Section> sections;
    private SpatialHeatInput pending;
    private PendingHeatCheckpoint coldPending;
    public final com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation captureCredit;
    private java.util.function.BiConsumer<Boolean, java.util.concurrent.CompletableFuture<Void>> finalizer;
    private java.util.concurrent.CompletableFuture<Void> finished;
    private boolean closed;
    private com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer;
    private com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Pending production;
    public void production(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Pending production) {
        this.producer = producer; this.production = production;
    }
    public void collectAdditionalThrough(long tick,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore.RecordConsumer consumer) {
        if (producer != null) producer.collectAdditionalThrough(tick, production, consumer);
    }
    public RegionalCheckpoint(List<Section> sections, SpatialHeatInput pending, PendingHeatCheckpoint coldPending,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation captureCredit) {
        this.sections = List.copyOf(sections); this.pending = pending; this.coldPending = coldPending; this.captureCredit = captureCredit;
    }
    public List<Section> sections() { return sections; }
    public SpatialHeatInput pending() { return pending; }
    public PendingHeatCheckpoint coldPending() { return coldPending; }
    public com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producerCapture() { return producer; }
    public record Section(long sectionKey, long geometryRevision, long sampledBricks, long sampleTick,
            AirFieldCheckpoint acceptedAir, MaterialSectionState acceptedMaterials) {
        public AirFieldCheckpoint captureAir(double naturalC) {
            return acceptedAir == null ? null : acceptedAir.retain(sampledBricks, naturalC, sampleTick, 0, sampleTick);
        }
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
        if (action == null) {
            if (captureCredit != null) captureCredit.close();
            receipt.complete(null);
        } else action.accept(unloading, receipt);
        return receipt;
    }

    @Override public void close() {
        synchronized (this) { if (closed) return; closed = true; }
        releaseFrames(); finish(false);
    }
    public synchronized void releaseFrames() {
        sections = List.of(); pending = SpatialHeatInput.EMPTY; coldPending = PendingHeatCheckpoint.EMPTY;
        producer = null; production = null;
    }
}
