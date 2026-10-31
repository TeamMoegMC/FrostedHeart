/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import net.minecraft.core.BlockPos;

/** Passive observer of actual source events and build inputs; never changes thermal state. */
public final class SourceInputProductionObservation {
    private static BlockPos fire;
    private static WorkerPhysicalSourceBindings owner;
    private static WorkerPhysicalSourceBindings.FrozenSources retained;
    private static long retainedMask;
    private static int retainedPorts, retainedDirtyHash;
    private static boolean emitting;
    private static int freezes, checks, changedChecks, emissionChanges, commits, futurePending;
    private static long maxBacking;
    private static String failure;

    public static synchronized void begin(BlockPos position) {
        fire = position.immutable(); owner = null; retained = null;
        freezes = checks = changedChecks = emissionChanges = commits = futurePending = 0;
        maxBacking = 0; failure = null; emitting = false;
    }

    public static synchronized void afterEvent(WorkerPhysicalSourceBindings bindings, SpatialSourceTimeline.Descriptor descriptor, boolean registration) {
        if (fire == null) return;
        if (owner == null && registration
                && descriptor.x() == fire.getX() && descriptor.y() == fire.getY()
                && descriptor.z() == fire.getZ()) owner = bindings;
        if (owner != bindings) return;
        verifyRetained();
        var mixing = new AirMixingRegion();
        mixing.reset(fire.getX() & ~3, fire.getY() & ~3, fire.getZ() & ~3);
        bindings.collectMixingSources(mixing);
        boolean active = mask(mixing) != 0;
        if (active != emitting) {
            emitting = active;
            emissionChanges++;
            if (retained != null) changedChecks++;
        }
    }

    public static synchronized void frozen(WorkerPhysicalSourceBindings bindings,
            WorkerPhysicalSourceBindings.FrozenSources snapshot) {
        if (owner != bindings) return;
        verifyRetained();
        retained = snapshot;
        retainedMask = frozenMask(snapshot);
        retainedPorts = snapshot.activeAirPortCount();
        retainedDirtyHash = snapshot.mixingChanges().hashCode();
        var live = new AirMixingRegion();
        live.reset(fire.getX() & ~3, fire.getY() & ~3, fire.getZ() & ~3);
        bindings.collectMixingSources(live);
        if (retainedMask != mask(live) || retainedPorts != bindings.activeAirPortCount()
                || !snapshot.mixingChanges().equals(bindings.mixingChanges())
                || snapshot.mixingRevision() != bindings.mixingRevision())
            failure = "A real build snapshot differs from its event-time source state";
        var meter = new AirBackingMeter();
        snapshot.recordBacking(meter);
        maxBacking = Math.max(maxBacking, meter.bytes());
        if (meter.bytes() > bindings.frozenPreparationBytes())
            failure = "Real source snapshot backing exceeds its preparation admission bound";
        freezes++;
    }

    public static synchronized void committed(WorkerPhysicalSourceBindings bindings,
            long cutoff, it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap pending) {
        if (owner != bindings) return;
        verifyRetained();
        for (long revision : pending.values()) {
            if (revision <= cutoff) failure = "A completed mixing revision was not acknowledged";
            else futurePending++;
        }
        commits++;
    }

    private static void verifyRetained() {
        if (retained == null) return;
        checks++;
        if (retainedMask != frozenMask(retained) || retainedPorts != retained.activeAirPortCount()
                || retainedDirtyHash != retained.mixingChanges().hashCode())
            failure = "A retained real build input changed after a later source event or commit";
    }

    private static long frozenMask(WorkerPhysicalSourceBindings.FrozenSources snapshot) {
        var mixing = new AirMixingRegion();
        mixing.reset(fire.getX() & ~3, fire.getY() & ~3, fire.getZ() & ~3);
        snapshot.collectMixingSources(mixing);
        return mask(mixing);
    }

    private static long mask(AirMixingRegion mixing) {
        long result = 0;
        int x = fire.getX() & ~3, y = fire.getY() & ~3, z = fire.getZ() & ~3;
        for (int block = 0; block < 64; block++) if (mixing.volumeScale(
                x + (block & 3) + .5, y + (block >>> 4) + .5, z + (block >>> 2 & 3) + .5) > 1)
            result |= 1L << block;
        return result;
    }

    public static synchronized boolean emitting() { return emitting; }
    public static synchronized int changedChecks() { return changedChecks; }
    public static synchronized int freezes() { return freezes; }
    public static synchronized int commits() { return commits; }
    public static synchronized String failure() { return failure; }
    public static synchronized String summary() {
        return "freezes=" + freezes + ",retainedChecks=" + checks + ",checksAcrossEmissionChanges=" + changedChecks
                + ",emissionChanges=" + emissionChanges + ",commits=" + commits + ",futurePendingObserved=" + futurePending
                + ",maxSnapshotBackingBytes=" + maxBacking + ",failure=" + failure;
    }
    public static synchronized void end() { fire = null; owner = null; retained = null; }
}
