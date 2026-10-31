/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Read-only witnesses of source prefixes, unpublished structures and their actual publication. */
public final class RegionalBoundaryObservation {
    public enum Mode { REMOTE_GEOMETRY, SOURCE_OUTLET, LOCAL_GEOMETRY }
    private static final class Structure {
        final long sequence, tick;
        final RegionalContinuationBoundary boundary;
        final boolean remote;
        boolean published;
        ThermalRegion guarded;
        long guardedSample;
        Structure(ThermalInputBatch input, RegionalContinuationBoundary boundary) {
            sequence = input.sequence(); tick = input.targetTick(); this.boundary = boundary;
            remote = !boundary.affects(SectionPos.asLong(small.getX() >> 4, small.getY() >> 4, small.getZ() >> 4));
        }
    }
    private static final IdentityHashMap<Object, Structure> structures = new IdentityHashMap<>();
    private static Mode mode;
    private static BlockPos small, edit;
    private static ThermalSourceLedger ledger;
    private static QueryPublication queries;
    private static RegionalAirRuntime root;
    private static ThermalSignatureTable signatures;
    private static TopologyInbox inbox;
    private static long remoteOldest = Long.MAX_VALUE, witnessBoundary, lastAcceptedTick, firstRevision, lastRevision;
    private static int requiredWindows = 2, acceptedWindows, pendingBoundaries, sourceHolds, localHolds;
    private static boolean pendingRoutesRequired, actionIssued, completed, mainIdle, workerPending;
    private static String failure;
    private static double confirmedJ, pendingJ, imbalanceJ;

    public static synchronized void requirePendingRoutes(boolean required) { pendingRoutesRequired = required; requiredWindows = required ? 4 : 2; }
    public static synchronized void requireConsecutiveWindows(int count) { requiredWindows = count; }
    public static synchronized void arm(Mode next, BlockPos point, BlockPos changed) {
        mode = next; small = point.immutable(); edit = changed.immutable(); root = null; inbox = null;
        signatures = MinecraftThermalProfiles.prepare().signatures();
        structures.clear(); actionIssued = completed = false; failure = null;
        remoteOldest = Long.MAX_VALUE; witnessBoundary = lastAcceptedTick = firstRevision = lastRevision = 0;
        acceptedWindows = pendingBoundaries = sourceHolds = localHolds = 0;
        confirmedJ = pendingJ = imbalanceJ = 0;
    }
    public static synchronized void engine(RegionalAirRuntime solver, ThermalSourceLedger sources, QueryPublication publication) {
        if (mode == null || solver == null || root != null && root != solver) return;
        if (solver == null) return;
        boolean ownsProbe = false;
        for (var region : solver.observedRegions()) ownsProbe |= containsSmall(region);
        if (!ownsProbe) return;
        root = solver; ledger = sources; queries = publication;
    }

    /** Only the owning coordinator inspects its inbox; main-thread transport callbacks do not. */
    public static synchronized void round(RegionalAirRuntime solver, TopologyInbox next, boolean pending) {
        if (root == null || root == solver) workerPending = pending;
        if (mode == null || root != solver) return;
        inbox = next; refreshRemoteBoundary();
    }
    private static void refreshRemoteBoundary() {
        remoteOldest = Long.MAX_VALUE;
        if (inbox != null) for (long section : inbox.pendingSections()) {
            if (Math.abs((SectionPos.x(section) << 4) - small.getX()) < 100) continue;
            var input = inbox.peek(section);
            if (input == null) continue;
            boolean alreadyPublished = false;
            for (var structure : structures.values()) if (structure.published && structure.boundary.affects(section)
                    && structure.tick == input.batch().targetTick() && structure.sequence == input.batch().sequence()) {
                alreadyPublished = true; break;
            }
            if (!alreadyPublished) remoteOldest = Math.min(remoteOldest, input.batch().targetTick());
        }
        for (var structure : structures.values()) if (structure.remote && !structure.published)
            remoteOldest = Math.min(remoteOldest, structure.tick);
    }
    static synchronized long unfinishedRemoteTick() { return remoteOldest; }

    public static synchronized void blocked(RegionalContinuationBoundary boundary, long tick, List<?> regions) {
        if (mode != null && root != null && root.observedRegions() == regions
                && boundary.includesUnresolvedRoutes()) pendingBoundaries++;
    }
    public static synchronized void geometry(Object task, ThermalInputBatch input,
            RegionalContinuationBoundary boundary, String stage, boolean done) {
        if (mode == null) return;
        if (done) { structures.remove(task); refreshRemoteBoundary(); return; }
        var structure = structures.get(task);
        if (structure == null || structure.tick != input.targetTick() || structure.sequence != input.sequence()) {
            structure = new Structure(input, boundary); structures.put(task, structure);
            structure.published = "CLEANUP".equals(stage);
        }
        try {
            if (!completed && failure == null && !structure.published && mode != Mode.REMOTE_GEOMETRY && actionIssued
                    && structure.guarded == null && "TOPOLOGY".equals(stage) && containsEdit(input) && root != null) {
                for (var region : root.observedRegions()) if (containsSmall(region)) {
                    check(region.executionLimit() <= input.targetTick(), "The edited region lacks its actual geometry hold");
                    check(region.sampleTick() <= input.targetTick(), "The edited region already passed its physical boundary");
                    structure.guarded = region; structure.guardedSample = region.sampleTick(); break;
                }
            }
            if (!completed && failure == null && structure.guarded != null && !structure.published)
                check(structure.guarded.sampleTick() == structure.guardedSample,
                        "Held room advanced during its actual geometry preparation");
            if ("PUBLISHED".equals(stage)) {
                structure.published = true;
                if (!completed && failure == null && structure.guarded != null) {
                    if (mode == Mode.SOURCE_OUTLET) sourceHolds++; else localHolds++;
                    completed = true;
                }
            }
        } catch (RuntimeException | AssertionError problem) { failure = problem.toString(); }
        refreshRemoteBoundary();
    }

    public static synchronized void accepted(Object owner, Object candidate, boolean accepted) {
        if (mode == null || completed || !accepted || failure != null) return;
        var job = (RegionJob) candidate;
        if (job.qualityOnly() || job.input == null) return;
        var region = job.region;
        if (!containsSmall(region) || region.sampleTick() != job.input.toTick || region.consumedSequence() != job.input.sequence) return;
        var runtime = (RegionalAirRuntime) owner;
        try {
            for (var structure : structures.values()) if (!structure.published && structure.guarded == region)
                check(region.sampleTick() <= structure.tick, "Affected heat crossed an unpublished structural input");
            refreshRemoteBoundary();
            if (mode != Mode.REMOTE_GEOMETRY || !actionIssued) return;
            // First prove a real independent commit. Then prove continued ownership
            // and progress, even if the unrelated construction finishes quickly.
            boolean firstCrossing = witnessBoundary == 0;
            if (firstCrossing && (remoteOldest == Long.MAX_VALUE || job.input.toTick <= remoteOldest
                    || pendingRoutesRequired && pendingBoundaries == 0)) return;
            if (job.input.toTick - job.input.fromTick != ThermalInputBatch.CUT_INTERVAL_TICKS) { acceptedWindows = 0; return; }
            if (acceptedWindows == 0 || job.input.fromTick != lastAcceptedTick) {
                acceptedWindows = 0; firstRevision = job.input.sequence;
            }
            check(job.input.toTick <= runtime.receivedThrough(), "Physical time preceded intake ownership");
            check(!region.inputSources.isEmpty(), "Observe a real source-fed region");
            for (var source : region.inputSources.long2IntEntrySet())
                check(ledger.spatialBoundThrough(source.getLongKey(), source.getIntValue()) >= job.input.toTick,
                        "Physical commit preceded its concrete source prefix");
            var sample = new QueryPublication.MutableSample();
            check(queries.tryReadAir(small.getX() + .5, small.getY() + .5, small.getZ() + .5, sample)
                    && sample.sampleTick() == job.input.toTick, "Query time differs from the accepted region");
            confirmedJ = job.deliveredEnergyJ(); pendingJ = runtime.pendingEnergyJ();
            var balance = ledger.energyBalance();
            imbalanceJ = balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ()
                    - balance.degradedLossJ() - balance.unacceptedJ() - balance.exportedJ() - pendingJ;
            check(Math.abs(imbalanceJ) < 1e-6, "Independent commit lost or duplicated source input");
            if (confirmedJ <= 0 || firstCrossing && pendingJ <= 0) { acceptedWindows = 0; return; }
            if (firstCrossing) witnessBoundary = remoteOldest;
            acceptedWindows++; lastAcceptedTick = job.input.toTick; lastRevision = job.input.sequence;
            FHMain.LOGGER.info("THERMAL_INDEPENDENT_ACCEPT,inputRevision={},sampleTick={},crossedBoundary={},firstCrossing={},windows={},pendingJ={}",
                    lastRevision, lastAcceptedTick, witnessBoundary, firstCrossing, acceptedWindows, pendingJ);
            completed = acceptedWindows >= requiredWindows;
        } catch (RuntimeException | AssertionError problem) { failure = problem.toString(); }
    }

    public static synchronized boolean takeWorldAction() {
        if (mode == null || completed || actionIssued) return false;
        boolean preparing = false;
        for (var structure : structures.values()) preparing |= !structure.published;
        if (remoteOldest == Long.MAX_VALUE && !preparing) return false;
        actionIssued = true; return true;
    }
    private static boolean containsEdit(ThermalInputBatch input) {
        if (signatures == null || edit == null) return false;
        long section = SectionPos.asLong(edit.getX() >> 4, edit.getY() >> 4, edit.getZ() >> 4);
        int block = (edit.getX() & 15) | (edit.getZ() & 15) << 4 | (edit.getY() & 15) << 8;
        var geometry = input.geometry();
        for (int index = 0; index < geometry.size(); index++) if (geometry.page(index).sectionKey() == section) {
            var full = geometry.fullPageSignatures(index);
            if (full != null && signatures.materialProfileId(full.get(block)) != 0) return true;
            if (full == null && geometry.blockIndex(index) == block && signatures.materialProfileId(geometry.signatureId(index)) != 0) return true;
        }
        var changes = geometry.materialChanges();
        for (int index = 0; index < changes.size(); index++) if (changes.page(index).sectionKey() == section
                && changes.blockIndex(index) == block && signatures.materialProfileId(changes.nextSignature(index)) != 0) return true;
        for (var halo : geometry.halos()) for (int index = 0; index < halo.halo().size(); index++)
            if (halo.halo().positions()[index] == edit.asLong() && signatures.materialProfileId(halo.halo().signatures()[index]) != 0) return true;
        return false;
    }
    private static boolean containsSmall(ThermalRegion region) { return region.solver.layout().componentAt(small.getX(), small.getY(), small.getZ()) != null; }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static synchronized void mainIdle(boolean idle) { mainIdle = idle; }
    public static synchronized boolean quiet() { return mainIdle && !workerPending && structures.isEmpty(); }
    public static synchronized boolean completed() { return completed; }
    public static synchronized boolean actionIssued() { return actionIssued; }
    public static synchronized Mode mode() { return mode; }
    public static synchronized String failure() { return failure; }
    public static synchronized String summary() {
        return "mode=" + mode + ",completed=" + completed + ",actionIssued=" + actionIssued
                + ",acceptedWindows=" + acceptedWindows + ",requiredWindows=" + requiredWindows + ",firstInputRevision=" + firstRevision
                + ",lastInputRevision=" + lastRevision + ",lastAcceptedTick=" + lastAcceptedTick
                + ",crossedBoundary=" + witnessBoundary + ",unfinishedBoundary=" + remoteOldest
                + ",pendingBoundaries=" + pendingBoundaries + ",sourceHolds=" + sourceHolds + ",localHolds=" + localHolds
                + ",smallConfirmedJ=" + confirmedJ + ",unfinishedPendingJ=" + pendingJ + ",imbalanceJ=" + imbalanceJ + ",failure=" + failure;
    }
    public static synchronized void end() {
        mode = null; root = null; ledger = null; queries = null; inbox = null; structures.clear();
    }
}
