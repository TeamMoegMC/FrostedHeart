/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

/** Coordinator-captured influence of one still-uninstalled structural input. */
public final class RegionalContinuationBoundary {
    public static final RegionalContinuationBoundary UNCHANGED = new RegionalContinuationBoundary(LongSets.EMPTY_SET, false);
    private final LongSet sections;
    private final boolean unresolvedRoutes;

    RegionalContinuationBoundary(LongSet sections, boolean unresolvedRoutes) {
        this.sections = LongSets.unmodifiable(sections);
        this.unresolvedRoutes = unresolvedRoutes;
    }

    public boolean affects(long section) { return sections.contains(section); }
    public int sectionCount() { return sections.size(); }
    public boolean includesUnresolvedRoutes() { return unresolvedRoutes; }
    public LongSet sections() { return sections; }

    public RegionalContinuationBoundary including(LongSet additional) {
        if (sections.containsAll(additional)) return this;
        var merged = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(sections);
        merged.addAll(additional);
        return new RegionalContinuationBoundary(merged, unresolvedRoutes);
    }
    public static boolean hasStructuralInput(ThermalInputBatch batch) {
        if (batch.admissions().length != 0 || batch.retirements().length != 0
                || batch.residencyUpdates().length != 0 || !batch.geometry().isEmpty()
                || batch.phaseAcks().length != 0 || batch.phaseIntents().length != 0) return true;
        for (var environment : batch.environmentUpdates()) if (environment.skyColumns().length != 0) return true;
        return false;
    }
}
