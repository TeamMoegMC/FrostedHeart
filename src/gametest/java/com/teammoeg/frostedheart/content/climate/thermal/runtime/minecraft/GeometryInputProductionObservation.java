/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FrozenAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;
import net.minecraft.core.BlockPos;

/** Borrow one actual build input across real construction, without changing heat or worker timing. */
public final class GeometryInputProductionObservation {
    private static BlockPos fire, pillar;
    private static ThermalCellArena owner;
    private static FrozenAirGeometry held;
    private static FrozenAirGeometry.ReadLease reader;
    private static int oldSlot, oldGeneration, oldX, oldY, oldZ, oldPillarSignature, latestPillarSignature;
    private static long oldAirMask, oldPageRevision, maxReservation, maxMetered, rootBeforeRelease, rootAfterRelease;
    private static int captures, retainedChecks, maxSpans;
    private static boolean completed, retiredSeen, freedAfterRelease;
    private static String failure;

    public static synchronized void begin(BlockPos source, BlockPos mutation) {
        fire = source.immutable(); pillar = mutation.immutable(); owner = null; held = null; reader = null;
        completed = retiredSeen = freedAfterRelease = false; failure = null;
        captures = retainedChecks = maxSpans = 0;
        maxReservation = maxMetered = rootBeforeRelease = rootAfterRelease = 0;
    }

    public static synchronized void prepared(FrozenAirGeometry input, ThermalCellArena arena) {
        if (fire == null || completed) return;
        var page = input.pageAt(net.minecraft.core.SectionPos.asLong(fire.getX() >> 4, fire.getY() >> 4, fire.getZ() >> 4));
        if (page == null) return;
        captures++;
        maxReservation = Math.max(maxReservation, input.reservedBytes());
        maxSpans = Math.max(maxSpans, input.retainedSpanCount());
        var meter = new AirBackingMeter();
        input.recordBacking(meter);
        maxMetered = Math.max(maxMetered, meter.bytes());
        if (meter.bytes() > input.reservedBytes()) failure = "Actual geometry backing exceeds its admission bound";
        latestPillarSignature = input.signatureAtWorld(pillar.getX(), pillar.getY(), pillar.getZ());
        if (held != null) return;
        var target = new WorkerPageStore.MutableAirTarget();
        int slot = input.resolveAirFaceTarget(fire.getX(), fire.getY() + 1, fire.getZ(), BlockFace.NEGATIVE_Y, null, target);
        if (slot < 0 || !input.retainsCell(slot)) return;
        owner = arena;
        held = input;
        reader = input.retainRead();
        oldSlot = slot; oldGeneration = arena.lifecycleGeneration(slot);
        oldX = arena.minimum(slot, 0); oldY = arena.minimum(slot, 1); oldZ = arena.minimum(slot, 2);
        oldAirMask = arena.airBlockMask(slot);
        oldPillarSignature = latestPillarSignature;
        oldPageRevision = input.geometryRevision(page);
    }

    public static synchronized void afterCut(ThermalCellArena arena) {
        if (owner != arena || held == null || completed) return;
        try {
            retainedChecks++;
            var target = new WorkerPageStore.MutableAirTarget();
            var page = held.pageAt(net.minecraft.core.SectionPos.asLong(fire.getX() >> 4, fire.getY() >> 4, fire.getZ() >> 4));
            if (held.signatureAtWorld(pillar.getX(), pillar.getY(), pillar.getZ()) != oldPillarSignature
                    || held.geometryRevision(page) != oldPageRevision
                    || held.resolveAirFaceTarget(fire.getX(), fire.getY() + 1, fire.getZ(), BlockFace.NEGATIVE_Y, null, target) != oldSlot
                    || arena.lifecycleGeneration(oldSlot) != oldGeneration || arena.airBlockMask(oldSlot) != oldAirMask
                    || arena.minimum(oldSlot, 0) != oldX || arena.minimum(oldSlot, 1) != oldY || arena.minimum(oldSlot, 2) != oldZ)
                failure = "An actual retained build input changed after a later geometry commit";
            if (latestPillarSignature == oldPillarSignature || !arena.isRetainedGeometryCell(oldSlot)) return;
            retiredSeen = !arena.isLive(oldSlot) && !arena.isStagedCell(oldSlot);
            rootBeforeRelease = MinecraftThermalInput.thermalBudgetUsedBytes();
            reader.close(); reader = null;
            rootAfterRelease = MinecraftThermalInput.thermalBudgetUsedBytes();
            freedAfterRelease = !arena.isRetainedGeometryCell(oldSlot) && !arena.isLive(oldSlot)
                    && held.reservedBytes() == 0;
            held = null;
            completed = true;
        } catch (RuntimeException failureAtRead) {
            failure = failureAtRead.toString();
        }
    }

    public static synchronized void closing(ThermalCellArena arena) {
        if (arena != owner) return;
        if (reader != null) { reader.close(); reader = null; }
        held = null;
    }
    public static synchronized boolean borrowed() { return reader != null || completed; }
    public static synchronized boolean completed() { return completed; }
    public static synchronized boolean retirementPassed() { return retiredSeen && freedAfterRelease && rootAfterRelease < rootBeforeRelease; }
    public static synchronized String failure() { return failure; }
    public static synchronized String summary() {
        return "captures=" + captures + ",retainedChecks=" + retainedChecks + ",maxRetainedSpans=" + maxSpans
                + ",maxInputReservationBytes=" + maxReservation + ",maxMeteredBytes=" + maxMetered
                + ",retiredSeen=" + retiredSeen + ",freedAfterRelease=" + freedAfterRelease
                + ",rootBeforeRelease=" + rootBeforeRelease + ",rootAfterRelease=" + rootAfterRelease
                + ",completed=" + completed + ",failure=" + failure;
    }
    public static synchronized void end() { fire = null; pillar = null; }
}
