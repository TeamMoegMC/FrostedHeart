/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/** Read-only coordinator observations for the single-dimension scattered-base scene. */
public final class ScatteredLoadObservation implements AutoCloseable {
    public record Snapshot(long tick, int regions, int regionSections, long energyObservations,
            long orderingFailures, long energyFailures, long runtimeFailures, double maximumImbalanceJ, double pendingJ) { }
    private final Long2ObjectOpenHashMap<SpatialSourceIngress.Progress> accepted = new Long2ObjectOpenHashMap<>();
    private static volatile ScatteredLoadObservation current;
    private final long anchorSource;
    private boolean active = true;
    private ThermalCellArena arena;
    private long lastTick = -1, observations, orderingFailures, energyFailures, runtimeFailures;
    private int regions, sections;
    private double maximumImbalance, pendingJ;

    public ScatteredLoadObservation(long anchorSource) {
        this.anchorSource = anchorSource;
        current = this;
    }
    static ScatteredLoadObservation current() { return current; }
    public synchronized Snapshot snapshot() {
        return new Snapshot(lastTick, regions, sections, observations, orderingFailures, energyFailures, runtimeFailures, maximumImbalance, pendingJ);
    }

    synchronized void runtimeClosed(ThermalCellArena cells) {
        if (!active || arena != cells) return;
        runtimeFailures++;
        arena = null; accepted.clear(); lastTick = -1;
    }

    synchronized void sourceProgress(long source, SpatialSourceIngress.Progress next,
            SpatialSourceIngress.Progress bound, boolean producerReleased, int ownedWindows) {
        if (!active) return;
        if (ownedWindows > 2) orderingFailures++;
        if (bound != null && next != null && bound.generation() == next.generation() && bound.tick() > next.tick()) orderingFailures++;
        if (!producerReleased || next == null) return;
        var previous = accepted.put(source, next);
        if (previous != null && (next.generation() < previous.generation()
                || next.generation() == previous.generation() && (next.tick() < previous.tick()
                || next.sequence() < previous.sequence()
                || next.sequence() == previous.sequence() && next.ordinal() <= previous.ordinal()))) orderingFailures++;
    }

    synchronized void roundFinished(RegionalAirRuntime air, ThermalCellArena cells, ThermalSourceLedger sources,
            long tick) {
        if (!active || tick == lastTick) return;
        var balance = sources.energyBalance();
        if (arena == null && sources.suppliesPower(anchorSource)) arena = cells;
        if (arena != cells) return;
        lastTick = tick;
        var runtime = air;
        double owned = runtime.pendingEnergyJ();
        double difference = Math.abs(balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ()
                - balance.degradedLossJ() - balance.unacceptedJ() - balance.exportedJ() - owned);
        maximumImbalance = Math.max(maximumImbalance, difference);
        // The same streaming-sum roundoff allowance used by source replay, scaled to this longer scene.
        if (!Double.isFinite(difference) || difference > Math.max(1e-6, 32 * Math.ulp(Math.abs(balance.outputJ())))) energyFailures++;
        observations++;
        if (tick % 200 == 0 || sections == 0) {
            var unique = new LongOpenHashSet();
            for (var region : runtime.observedRegions()) unique.addAll(region.scope.sections());
            sections = unique.size();
        }
        regions = runtime.observedRegions().size(); pendingJ = owned;
    }

    @Override public synchronized void close() {
        active = false;
        if (current == this) current = null;
        arena = null; accepted.clear();
    }
}
