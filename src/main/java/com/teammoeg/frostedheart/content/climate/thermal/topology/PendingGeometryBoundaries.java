/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.SectionPos;

/** Coordinator-owned geometry walks for pending inputs. Source outlets are projected from current bindings separately. */
public final class PendingGeometryBoundaries {
    private final TopologyUpdatePlanner planner;
    private final IdentityHashMap<TopologyInbox.Input, RegionalContinuationBoundary> walks = new IdentityHashMap<>();
    private final LongOpenHashSet changed = new LongOpenHashSet();
    private boolean reusable;

    public PendingGeometryBoundaries(TopologyUpdatePlanner planner, WorkerPageStore pages) {
        this.planner = planner;
        pages.onCaptureMembershipChanged(section -> { if (!walks.isEmpty()) changed.add(section); });
    }

    /** Includes staged admissions and their rollback between structure commits. */
    public boolean hasCaptureChanges() { return !changed.isEmpty(); }

    /** Refresh once before reading pending inputs. Route discovery can introduce distant seeds or dependencies. */
    public void refresh() {
        reusable = planner.boundaryUsesOnlyCaptureIndex();
        if (!reusable) walks.clear();
        else if (!changed.isEmpty()) walks.values().removeIf(walk -> intersectsFrontier(walk.sections(), changed));
        changed.clear();
    }

    /** An addition can open a path from a face just outside the old walk. Removals can split a path inside it. */
    private static boolean intersectsFrontier(LongSet walk, LongSet edits) {
        LongSet scan = edits.size() <= walk.size() ? edits : walk;
        LongSet lookup = scan == edits ? walk : edits;
        for (var it = scan.iterator(); it.hasNext();) {
            long key = it.nextLong();
            if (lookup.contains(key)) return true;
            int x = SectionPos.x(key), y = SectionPos.y(key), z = SectionPos.z(key);
            for (int face = 0; face < 6; face++)
                if (lookup.contains(SectionPos.asLong(x + TopologyView.DX[face], y + TopologyView.DY[face], z + TopologyView.DZ[face]))) return true;
        }
        return false;
    }

    /** Use the original seeds and full planner for every new or invalidated input. */
    public RegionalContinuationBoundary get(TopologyInbox.Input input) {
        var cached = walks.get(input);
        if (cached != null) return cached;
        var seeds = new LongOpenHashSet();
        TopologyUpdatePlanner.collectBoundarySeeds(input.batch(), seeds);
        seeds.add(input.page().sectionKey());
        var boundary = RegionalContinuationBoundary.hasStructuralInput(input.batch()) || planner.hasPendingRoutes()
                ? planner.continuationBoundary(seeds, true) : RegionalContinuationBoundary.UNCHANGED.including(seeds);
        if (reusable) walks.put(input, boundary);
        return boundary;
    }

    public void retain(Set<TopologyInbox.Input> pending) { walks.keySet().retainAll(pending); }

    public void clear() { walks.clear(); changed.clear(); }

    /** Include retained raw ranges when the outlet projection needed a separate set. */
    public void recordBacking(AirBackingMeter meter) {
        meter.container(walks, walks.size(), 32);
        meter.container(changed, changed.size(), 8);
        for (var walk : walks.values()) if (meter.objectOnce(walk, 64))
            meter.container(walk.sections(), walk.sectionCount(), 8);
    }
}
